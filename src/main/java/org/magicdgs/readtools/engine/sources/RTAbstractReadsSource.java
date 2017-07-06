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

package org.magicdgs.readtools.engine.sources;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.iterators.ReadTransformerIterator;
import org.magicdgs.readtools.utils.iterators.paired.GATKReadPairedIterator;
import org.magicdgs.readtools.utils.read.transformer.CheckQualityReadTransformer;
import org.magicdgs.readtools.utils.read.transformer.SolexaToSangerReadTransformer;

import htsjdk.samtools.util.FastqQualityFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.transformers.MisencodedBaseQualityReadTransformer;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Helper class to implemen reads sources easily.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class RTAbstractReadsSource implements RTReadsSource {

    /** Logger for the class. */
    protected final Logger logger = LogManager.getLogger(this);

    private Optional<FastqQualityFormat> forcedEncoding = Optional.empty();

    /**
     * Number of reads to use for quality detection. Defaults in {@link
     * RTDefaults#MAX_RECORDS_FOR_QUALITY}.
     */
    protected long maxNumberOfReadsForQuality = RTDefaults.MAX_RECORDS_FOR_QUALITY;

    // cached format
    private FastqQualityFormat format = null;

    /** Guesses/detects the encoding by reading the file using {@code maxNumnerOfReads}. */
    protected abstract FastqQualityFormat detectEncoding(final long maxNumberOfReads);

    /** Returns a raw iterator over the reads, without transformation to standard quality. */
    protected abstract Iterator<GATKRead> rawIterator();

    /**
     * {@inheritDoc}
     *
     * <p>Default behavior wraps {@link #iterator()} as an interleaved iterator.
     */
    @Override
    public Iterator<Tuple2<GATKRead, GATKRead>> getPairedIterator() {
        if (!isPaired()) {
            throw new UnsupportedOperationException(
                    getSourceFormat() + ": cannot retrieve pair-end");
        }
        return GATKReadPairedIterator.of(iterator());
    }

    /**
     * Caches and return the format detected by {@link #detectEncoding(long)} and/or forced by
     * {@link #setForcedEncoding(FastqQualityFormat)}. If both of them differs, log a warning on
     * the
     * first call.
     */
    @Override
    public final FastqQualityFormat getQualityEncoding() {
        if (format != null) {
            final FastqQualityFormat detected = detectEncoding(maxNumberOfReadsForQuality);
            forcedEncoding.ifPresent(format -> {
                if (!format.equals(detected)) {
                    logger.warn("Forcing {} encoding for {}: detected encoding was {}",
                            () -> format, () -> getSourceFormat(), () -> detected);
                }
            });
            format = forcedEncoding.orElse(detected);
        }
        return format;
    }

    /**
     * Standardizes an iterator over reads. Useful for custom implementations of
     * {@link #getPairedIterator()}.
     */
    protected final Iterator<GATKRead> standardizeEncodingIterator(
            final Iterator<GATKRead> iterator) {
        return new ReadTransformerIterator(iterator, getQualityTransformer());
    }

    /**
     * Gets a read transformer to fix the quality based on the {@link #getQualityEncoding()}.
     * It may be used for custom implementations of {@link #getPairedIterator()}.
     */
    protected final ReadTransformer getQualityTransformer() {
        final FastqQualityFormat encoding = getQualityEncoding();
        switch (encoding) {
            case Standard:
                // TODO: port to GATK MisencodedBaseQualityReadTransformer
                return new CheckQualityReadTransformer();
            case Illumina:
                return new MisencodedBaseQualityReadTransformer();
            case Solexa:
                return new SolexaToSangerReadTransformer();
            default:
                throw new GATKException("Unknown quality encoding: " + encoding);
        }
    }

    /**
     * Returns the {@link #rawIterator()} wrapped standarized by {@link
     * #standardizeEncodingIterator(Iterator)}.
     */
    @Override
    public final Iterator<GATKRead> iterator() {
        return standardizeEncodingIterator(rawIterator());
    }

    @Override
    public final RTReadsSource setForcedEncoding(final FastqQualityFormat encoding) {
        if (format != null) {
            throw new IllegalStateException("Quality was already detected and cannot be forced");
        }
        this.forcedEncoding = Optional.ofNullable(encoding);
        return this;
    }

    @Override
    public final RTReadsSource setMaxNumberOfReadsForQuality(long maxNumberOfReadsForQuality) {
        this.maxNumberOfReadsForQuality = maxNumberOfReadsForQuality;
        return this;
    }
}
