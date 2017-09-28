/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
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
 * SOFTWARE.
 */

package org.magicdgs.readtools.engine;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.engine.sourcehandler.ReadsSourceHandler;
import org.magicdgs.readtools.utils.iterators.InterleaveGATKReadIterators;
import org.magicdgs.readtools.utils.iterators.ReadTransformerIterator;
import org.magicdgs.readtools.utils.iterators.paired.GATKReadPairedIterator;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;
import org.magicdgs.readtools.utils.read.transformer.CheckQualityReadTransformer;
import org.magicdgs.readtools.utils.read.transformer.SolexaToSangerReadTransformer;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamFileHeaderMerger;
import htsjdk.samtools.util.FastqQualityFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.engine.GATKDataSource;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.transformers.MisencodedBaseQualityReadTransformer;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * ReadTools abstract implementation of {@link GATKDataSource} for {@link GATKRead}, which includes:
 *
 * - Tracking the original quality encoding of the data source.
 * - Iterates over reads already in standard format.
 *
 * WARNING: query is not working yet.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class RTDataSource implements GATKDataSource<GATKRead>, AutoCloseable {

    // logger for the class
    private final Logger logger = LogManager.getLogger(this);

    // factory for creating readers
    private static ReadReaderFactory readerFactory = new ReadReaderFactory();

    // source handler
    private final ReadsSourceHandler readHandler;
    private final ReadsSourceHandler secondHandler;

    // parameters for handling the sources: file is interleaved/assign to a concrete encoding
    private final boolean interleaved;
    private final FastqQualityFormat forceEncoding;

    // CACHE VALUES
    // the original encoding will be equal to forceEncoding if that one is not null
    private FastqQualityFormat originalEncoding = null;
    // the file header: it will be always sort by queryname if paired and a minimal one for FASTQ files
    private SAMFileHeader header = null;

    /**
     * Internal constructor.
     *
     * @param readSourceString   no-null string representing the source of reads.
     * @param secondSourceString string representing the source of reads for the  second pair. May
     *                           be {@code null}.
     * @param interleaved        if {@code true} the input is interleaved. It is not allowed if
     *                           {@code secondReadPath} is no-null.
     * @param forceEncoding      force the input file to have this encoding. May be {@code null}.
     */
    private RTDataSource(final String readSourceString, final String secondSourceString,
            boolean interleaved, final FastqQualityFormat forceEncoding) {
        Utils.nonNull(readSourceString, "null readsPath");
        if (interleaved && secondSourceString != null) {
            throw new IllegalArgumentException("Provided interleaved and a second source");
        }
        this.readHandler = ReadsSourceHandler.getHandler(readSourceString, readerFactory);
        this.secondHandler = (secondSourceString == null)
                ? null : ReadsSourceHandler.getHandler(secondSourceString, readerFactory);
        this.interleaved = interleaved;
        this.forceEncoding = forceEncoding;
    }

    /** Copy constructor changing the forcing of the encoding. */
    @VisibleForTesting
    RTDataSource(final RTDataSource source, final FastqQualityFormat forceEncoding) {
        this.readHandler = source.readHandler;
        this.secondHandler = source.secondHandler;
        this.interleaved = source.interleaved;
        this.forceEncoding = forceEncoding;
    }

    /**
     * Single file/source data constructor.
     *
     * @param readSourceString the source of reads.
     * @param interleaved      if {@code true} the source will be able to retrieve pair-end reads.
     * @param forceEncoding    if {@code null}, auto-detects encoding; otherwise, force this
     *                         encoding for the source.
     */
    public RTDataSource(final String readSourceString, final boolean interleaved,
            final FastqQualityFormat forceEncoding) {
        this(readSourceString, null, interleaved, forceEncoding);
    }

    /**
     * Single file/source data constructor, with encoding auto-detection.
     *
     * @param readSourceString the source of reads.
     * @param interleaved      if {@code true} the source will be able to retrieve pair-end reads.
     */
    public RTDataSource(final String readSourceString, final boolean interleaved) {
        this(readSourceString, null, interleaved, null);
    }

    /**
     * Pair file/source data constructor.
     *
     * @param readSourceStringFirst  the source of reads for the first pair.
     * @param readSourceStringSecond the source of reads for the second pair.
     * @param forceEncoding          if {@code null}, auto-detects encoding; otherwise, force this
     *                               encoding for the source.
     */
    public RTDataSource(final String readSourceStringFirst, final String readSourceStringSecond,
            final FastqQualityFormat forceEncoding) {
        this(readSourceStringFirst, readSourceStringSecond, false, forceEncoding);
    }

    /**
     * Pair file/source data constructor, with encoding auto-detection.
     *
     * @param readSourceStringFirst  the source of reads for the first pair.
     * @param readSourceStringSecond the source of reads for the second pair.
     */
    public RTDataSource(final String readSourceStringFirst, final String readSourceStringSecond) {
        this(readSourceStringFirst, readSourceStringSecond, false, null);
    }

    /** Sets the reader factory used in the RTDataSource to open the sources. */
    public static void setReadReaderFactory(final ReadReaderFactory factory) {
        readerFactory = factory;
    }

    /** Returns {@code true} if the source represents pair-end data; {@code false} otherwise. */
    public boolean isPaired() {
        return interleaved || secondHandler != null;
    }

    /** Returns the original quality encoding. */
    public FastqQualityFormat getOriginalQualityEncoding() {
        if (originalEncoding == null) {
            originalEncoding = originalEncoding(readHandler);
            if (secondHandler != null) {
                final FastqQualityFormat second = originalEncoding(secondHandler);
                if (!originalEncoding.equals(second)) {
                    throw new UserException("Quality encoding for pair-end files is different: "
                            + readHandler.getHandledSource() + "=" + originalEncoding
                            + secondHandler.getHandledSource() + "=" + second);
                }
            }
        }
        return originalEncoding;
    }

    /**
     * Gets the consensus SAM header for the data.
     *
     * <p>Note: the returned header is cached, so any modification will be reflected in successive
     * calls.
     */
    public SAMFileHeader getHeader() {
        // if it is no cached
        if (header == null) {
            // check the first header
            final SAMFileHeader firstHeader = getSamFileHeader(readHandler);
            // if it is pair-end
            if (secondHandler != null) {
                // assuming always unsorted for pair-end data
                header = new SamFileHeaderMerger(SAMFileHeader.SortOrder.unsorted,
                        Arrays.asList(firstHeader, getSamFileHeader(secondHandler)), true)
                        .getMergedHeader();
                // removes group order included by the merger
                // this should be done like this because of a null pointer exception
                // TODO: we should assume SORT_ORDER = query for all the pair-end
                header.setAttribute(SAMFileHeader.GROUP_ORDER_TAG, null);
            } else {
                header = firstHeader;
            }
        }
        return header;
    }


    // helper for get the original encoding for a handler and log a warning if differs for the forced
    private FastqQualityFormat originalEncoding(final ReadsSourceHandler handler) {
        final FastqQualityFormat format =
                handler.getQualityEncoding(RTDefaults.MAX_RECORDS_FOR_QUALITY);
        if (forceEncoding != null && !format.equals(forceEncoding)) {
            logger.warn("Forcing {} encoding for {}: detected encoding was {}",
                    forceEncoding, handler.getHandledSource(), format);
            return forceEncoding;
        }
        return format;
    }

    // helper method to get the SAM header with coordinate
    private SAMFileHeader getSamFileHeader(final ReadsSourceHandler handler) {
        final SAMFileHeader header = handler.getHeader();
        SAMFileHeader.SortOrder order = header.getSortOrder();
        if (isPaired()) {
            switch (order) {
                // both coordinate/duplicate sorting are not allowed here if it is paired
                case coordinate:
                case duplicate:
                    throw new UserException(String.format(
                            "Pair-end read source %s sorted by '%s'. ReadTools requires pairs to be together: either sorted or grouped by queryname.",
                            handler.getHandledSource(), order));
                // both unsorted and unknown
                case unsorted:
                case unknown:
                    logger.warn(
                            "Pair-end read source {} with '{}' order grouped by '{}': Assuming that reads are grouped by read name, keeping pairs together.",
                            handler.getHandledSource(), order, header.getGroupOrder());
                    order = SAMFileHeader.SortOrder.unsorted;
                    break;
                case queryname:
                    // TODO - remove this limitation
                    order = SAMFileHeader.SortOrder.unsorted;
                    logger.warn("Output might reflect '%s' even if '%s' is specified in pair-end source %s. This limitation may be removed in the future.",
                            order, SAMFileHeader.SortOrder.queryname, handler.getHandledSource());
                    break;
                default:
                    throw new GATKException("Unknown sort order: " + header.getSortOrder());
            }
        }

        // log always a warning for queryname order
        if (SAMFileHeader.SortOrder.queryname.equals(order)) {
            logger.warn(
                    "Sort order by '{}' is assumed to follow the Picard specifications. This limitation may be removed in the future.",
                    SAMFileHeader.SortOrder.queryname);
        }

        // finally, set the sort order
        header.setSortOrder(order);

        return header;
    }


    /**
     * Returns an iterator over the interval requested. Unmapped reads will not be included.
     *
     * Note: some sources may not be able to query intervals.
     *
     * @throws UserException if the data does not support it (FASTQ/SAM files, not indexed BAM...).
     */
    @Override
    public Iterator<GATKRead> query(final SimpleInterval interval) {
        try {
            final List<SimpleInterval> singleInterval = Collections.singletonList(interval);
            return (secondHandler == null)
                    ? transformedIterator(readHandler.toIntervalIterator(singleInterval))
                    : new InterleaveGATKReadIterators(
                            transformedIterator(readHandler.toIntervalIterator(singleInterval)),
                            transformedIterator(secondHandler.toIntervalIterator(singleInterval))
                    );
        } catch (UnsupportedOperationException e) {
            throw new UserException("Cannot query intervals." + e.getMessage(), e);
        }
    }

    /**
     * Gets all the reads in the Standard format.
     *
     * Note: if {@link #isPaired()} is {@code true}, it returns an interleaved iterator.
     *
     * @return reads already in {@link FastqQualityFormat#Standard}.
     */
    @Override
    public Iterator<GATKRead> iterator() {
        return (secondHandler == null)
                ? transformedIterator(readHandler.toIterator())
                : new InterleaveGATKReadIterators(
                        transformedIterator(readHandler.toIterator()),
                        transformedIterator(secondHandler.toIterator()));
    }

    /**
     * Gets all the reads as pair-end in the Standard format.
     *
     * Note: this is not available if {@link #isPaired()} returns {@code false}.
     *
     * @return paired-reads already in {@link FastqQualityFormat#Standard}.
     *
     * @throws IllegalArgumentException if {@link #isPaired()} is {@code false}.
     */
    public GATKReadPairedIterator pairedIterator() {
        Utils.validateArg(isPaired(), "no paired iterator");
        if (interleaved) {
            return GATKReadPairedIterator.of(transformedIterator(readHandler.toIterator()));
        } else {
            return pairedIteratorForSplitInput();
        }
    }

    // helper for use the read transformer
    private Iterator<GATKRead> transformedIterator(final Iterator<GATKRead> iterator) {
        return new ReadTransformerIterator(iterator, qualityTransformer());
    }

    // helper for generate a paired iterator from a split
    private GATKReadPairedIterator pairedIteratorForSplitInput() {
        return GATKReadPairedIterator.of(
                transformedIterator(readHandler.toIterator()),
                transformedIterator(secondHandler.toIterator()));
    }


    // gets the quality transformer for normalization
    private ReadTransformer qualityTransformer() {
        switch (getOriginalQualityEncoding()) {
            case Standard:
                // TODO: port to GATK MisencodedBaseQualityReadTransformer
                return new CheckQualityReadTransformer();
            case Illumina:
                return new MisencodedBaseQualityReadTransformer();
            case Solexa:
                return new SolexaToSangerReadTransformer();
            default:
                throw new GATKException(
                        "Unknown quality encoding: " + getOriginalQualityEncoding());
        }
    }

    /** Close all the data to clean up resources. It could be re-used even if it was closed. */
    @Override
    public void close() throws Exception {
        readHandler.close();
        if (secondHandler != null) {
            secondHandler.close();
        }
    }
}
