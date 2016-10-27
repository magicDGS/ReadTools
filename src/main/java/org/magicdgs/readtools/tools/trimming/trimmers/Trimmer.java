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
import org.magicdgs.readtools.tools.trimming.trimmers.stats.TrimStat;
import org.magicdgs.readtools.utils.record.SequenceMatch;
import org.magicdgs.readtools.utils.trimming.TrimmingMethods;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.Histogram;

import java.io.File;

/**
 * Contains the pipeline for trimming implemented in Kofler et al 2011 with some set thresholds,
 * using the algorithms in
 * {@link TrimmingMethods}
 *
 * @author Daniel Gómez-Sánchez
 */
public abstract class Trimmer {

    protected final boolean trimQuality;

    protected final int qualThreshold;

    protected final int minLength;

    protected final int maxLength;

    protected final boolean discardRemainingNs;

    protected final boolean no5ptrim;

    /**
     * Constructor for a new Mott algorithm with several thresholds
     *
     * @param trimQuality        should the quality be trimmed?
     * @param qualThreshold      quality threshold for the trimming
     * @param minLength          minimum length for the trimming
     * @param discardRemainingNs should we discard reads with Ns in the middle?
     * @param no5ptrim           no trim the 5 prime end
     */
    Trimmer(final boolean trimQuality, final int qualThreshold, final int minLength,
            final int maxLength, final boolean discardRemainingNs, final boolean no5ptrim) {
        this.trimQuality = trimQuality;
        this.qualThreshold = qualThreshold;
        this.maxLength = maxLength;
        this.minLength = minLength;
        this.discardRemainingNs = discardRemainingNs;
        this.no5ptrim = no5ptrim;
    }

    /**
     * Trim the record with the provided settings in the Trimmer object
     *
     * @param record the record to trim
     * @param format format to use for the quality encoding
     *
     * @return the trimmed record or <code>null</code> if does not pass filters
     */
    protected FastqRecord trimFastqRecord(final FastqRecord record, final FastqQualityFormat format,
            final TrimStat metric, final Histogram<Integer> histogram) {
        metric.TOTAL++;
        // the record that will be trimmed in sequence (it will change)
        FastqRecord toTrim = record;
        // first trim by Ns
        FastqRecord newRecord = TrimmingMethods.trimNs(toTrim, no5ptrim);
        // if the new record is null return null
        if (newRecord == null) {
            metric.POLY_N_TRIMMED++;
            // stats.addTrimmedNs();
            return null;
        } else if (!newRecord.equals(toTrim)) {
            // if the record is trimmed, add it to the pipeline
            metric.POLY_N_TRIMMED++;
            // stats.addTrimmedNs();
            toTrim = newRecord;
        }
        // if discard remaining Ns is set and the record to trim contain its, return null
        if (discardRemainingNs && SequenceMatch.sequenceContainNs(toTrim.getReadString())) {
            metric.INTERNAL_N_DISCARDED++;
            // stats.addCountRemainingNdiscards();
            return null;
        }
        // now the new record will be trimmed by quality if set
        if (trimQuality) {
            newRecord = TrimmingMethods.trimQualityMott(toTrim, format, qualThreshold, no5ptrim);
            if (newRecord == null) {
                metric.QUALITY_TRIMMED++;
                metric.LENGTH_DISCARDED++;
                // stats.addCountsQualityTrims();
                return null;
            } else if (!newRecord.equals(toTrim)) {
                metric.QUALITY_TRIMMED++;
                // stats.addCountsQualityTrims();
                toTrim = newRecord;
            }
        }
        // filter by length
        if (toTrim.length() < minLength || toTrim.length() > maxLength) {
            metric.LENGTH_DISCARDED++;
            // stats.addCountLengthDiscard();
            return null;
        }
        // here it passed all the filters
        metric.PASSED++;
        histogram.increment(toTrim.length());
        // stats.addReadPassing(toTrim.length());
        return toTrim;
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
    public abstract void printTrimmerMetrics(final File metricsFile);
}
