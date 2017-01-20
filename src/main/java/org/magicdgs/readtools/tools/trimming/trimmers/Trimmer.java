/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 */
package org.magicdgs.readtools.tools.trimming.trimmers;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.readtools.metrics.trimming.TrimStat;
import org.magicdgs.readtools.utils.read.FastqGATKRead;
import org.magicdgs.readtools.utils.read.RTReadUtils;
import org.magicdgs.readtools.utils.read.ReservedTags;
import org.magicdgs.readtools.utils.read.filter.NoAmbiguousSequenceReadFilter;
import org.magicdgs.readtools.utils.read.transformer.trimming.ApplyTrimResultReadTransfomer;
import org.magicdgs.readtools.utils.read.transformer.trimming.MottQualityTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrailingNtrimmer;
import org.magicdgs.readtools.utils.record.FastqRecordUtils;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.Histogram;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadLengthReadFilter;
import org.broadinstitute.hellbender.transformers.MisencodedBaseQualityReadTransformer;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contains the pipeline for trimming quality in Kofler et al 2011 with some set thresholds,
 * using the algorithms in {@link org.magicdgs.readtools.utils.trimming.TrimmingUtil}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class Trimmer {

    private final static ReadFilter noNsInSequence = new NoAmbiguousSequenceReadFilter();

    private static final ReadTransformer trailingNs = new TrailingNtrimmer();

    private final boolean trimQuality;

    private final MottQualityTrimmer mottTrimmer = new MottQualityTrimmer();

    private final boolean discardRemainingNs;

    private final ApplyTrimResultReadTransfomer applyTrimming;

    private final ReadFilter lengthFilter;

    /**
     * Constructor for a new Mott algorithm with several thresholds.
     *
     * @param trimQuality        if {@code true}, quality-based trimming will be performed.
     * @param qualThreshold      quality threshold for the trimming by quality.
     * @param minLength          minimum length after triming.
     * @param discardRemainingNs if {@code true}, reads with Ns in their sequence after trimming Ns
     *                           will be discarded.
     * @param no5ptrim           if {@code true}, no trim will be performed in the 5'-end.
     */
    Trimmer(final boolean trimQuality, final int qualThreshold, final int minLength,
            final int maxLength, final boolean discardRemainingNs, final boolean no5ptrim) {
        this.trimQuality = trimQuality;
        this.discardRemainingNs = discardRemainingNs;
        this.applyTrimming = new ApplyTrimResultReadTransfomer(no5ptrim, false);
        this.mottTrimmer.qualThreshold = qualThreshold;
        this.lengthFilter = new ReadLengthReadFilter(minLength, maxLength).negate();
    }

    /**
     * Trim the record with the provided settings in the Trimmer object.
     *
     * @param record the record to trim.
     * @param format format to use for the quality encoding.
     *
     * @return the trimmed record or {@code null} if does not pass filters.
     */
    protected FastqRecord trimFastqRecord(final FastqRecord record, final FastqQualityFormat format,
            final TrimStat metric, final Histogram<Integer> histogram) {
        final GATKRead read = new FastqGATKRead(record);
        switch (format) {
            case Illumina:
                new MisencodedBaseQualityReadTransformer().apply(read);
                break;
            default:
                // do nothing, we don't expect the previous framework to pass Solexa
                break;
        }
        final GATKRead trimmed = trimRead(read, metric, histogram);
        if (RTReadUtils.isCompletelyTrimRead(trimmed)) {
            return null;
        }
        return new FastqRecord( // read/qual headers come from the record; bases from the trimmed read
                record.getReadHeader(),
                trimmed.getBasesString(),
                record.getBaseQualityHeader(),
                ReadUtils.getBaseQualityString(read));
    }

    /**
     * Trims the read with the provided settings in the Trimmer object.
     *
     * @param read      the read to trim.
     * @param metric    the trimming statistics to update.
     * @param histogram the length distribution to update.
     *
     * @return the trimmed read (modified in place). To check if it is completely trimmed, use
     * {@link RTReadUtils#isCompletelyTrimRead(GATKRead)}.
     */
    protected GATKRead trimRead(final GATKRead read, final TrimStat metric,
            final Histogram<Integer> histogram) {
        int start = RTReadUtils.getTrimmingStartPoint(read);
        int end = RTReadUtils.getTrimmingEndPoint(read);
        metric.TOTAL++;
        // first trim by Ns
        trailingNs.apply(read);
        // if the new record is completely trimmed return directly
        if (RTReadUtils.updateCompletelyTrimReadFlag(read)) {
            metric.POLY_N_TRIMMED++;
            // set as completely trimmed (because of the length)
            read.setAttribute(ReservedTags.ct, 10);
            return read;
        } else if (shouldUpdateStatistic(read, start, end)) {
            // if the record is trimmed, add it to the pipeline
            metric.POLY_N_TRIMMED++;
        }
        // if discard remaining Ns is set and the record to trim contain its, return null
        // the read should be copied and the trimming applied in this case to keep the tags
        // intact and check if still there are some internal Ns
        // this is very dirty, but this class will be deprecated soon and it is not necessary
        // to work on solving the issue
        if (discardRemainingNs && !noNsInSequence.test(applyTrimming.apply(read.deepCopy()))) {
            metric.INTERNAL_N_DISCARDED++;
            // set as completely trimmed (because of the length)
            read.setAttribute(ReservedTags.ct, 10);
            return read;
        }
        // now the new record will be trimmed by quality if set
        if (trimQuality) {
            // we should store before it to keep track if it was trimmed
            start = RTReadUtils.getTrimmingStartPoint(read);
            end = RTReadUtils.getTrimmingEndPoint(read);
            mottTrimmer.apply(read);
            if (RTReadUtils.updateCompletelyTrimReadFlag(read)) {
                metric.QUALITY_TRIMMED++;
                metric.LENGTH_DISCARDED++;
                return read;
            } else if (shouldUpdateStatistic(read, start, end)) {
                metric.QUALITY_TRIMMED++;
            }
        }
        // cut the read before filtering by length
        applyTrimming.apply(read);
        // filter by length
        if (RTReadUtils.updateCompletelyTrimReadFlag(read) || lengthFilter.test(read)) {
            metric.LENGTH_DISCARDED++;
            // set as completely trimmed (because of the length)
            read.setAttribute(ReservedTags.ct, 10);
            return read;
        }
        // here it passed all the filters
        metric.PASSED++;
        histogram.increment(read.getLength());
        return read;
    }

    // helper function for the read method
    // if no trimming 5p, re-set the trimming tags
    // then check if the read is completely trimmed
    private boolean shouldUpdateStatistic(final GATKRead maybeTrimmed,
            final int previousStart, final int previousEnd) {
        return RTReadUtils.getTrimmingEndPoint(maybeTrimmed) != previousEnd
                || (!applyTrimming.noTrim5p()
                && RTReadUtils.getTrimmingStartPoint(maybeTrimmed) != previousStart);
    }

    /**
     * Trim the record with the provided settings in the Trimmer object
     *
     * @param record the record to trim
     * @param format format to use for the quality encoding
     *
     * @return the trimmed record or <code>null</code> if does not pass filters (one or both of
     * them)
     */
    public abstract FastqPairedRecord trimFastqPairedRecord(final FastqPairedRecord record,
            final FastqQualityFormat format);

    /**
     * Trim the record with the provided settings in the Trimmer object
     *
     * @param record the record to trim
     * @param format format to use for the quality encoding
     *
     * @return the trimmed record or <code>null</code> if does not pass filters
     */
    public abstract FastqRecord trimFastqRecord(final FastqRecord record,
            final FastqQualityFormat format);

    /**
     * Print the metrics for the trimmer
     *
     * @param metricsFile the file for the output
     */
    public abstract void printTrimmerMetrics(final Path metricsFile) throws IOException;
}
