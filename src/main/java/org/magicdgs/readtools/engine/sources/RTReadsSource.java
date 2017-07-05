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

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.hellbender.engine.GATKDataSource;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.io.Closeable;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface RTReadsSource extends GATKDataSource<GATKRead>, Closeable {

    /**
     * Guess the quality encoding by reading {@code maxNumberOfReads}. If set by {@link
     * #setForcedEncoding(FastqQualityFormat)}, this method will return that quality encoding, but
     * may log a warning if it differs.
     */
    public FastqQualityFormat getQualityEncoding();

    /** Gets the header for the source of reads. It may be a simple header, but never {@code null}. */
    public SAMFileHeader getHeader();

    /**
     * Returns an iterator over the reads.
     *
     * TODO: this should be already transformed with the quality encoding
     *
     * <p>Note: if {@link #isPaired()} is {@code true}, it should return an interleaved iterator.
     */
    public Iterator<GATKRead> iterator();

    /** Returns {@code true} if the source is paired; {@code false} otherwise. */
    public boolean isPaired();

    /**
     * Returns an iterator over the reads as pairs, already in standard format.
     *
     * @throws UnsupportedOperationException if {@link #isPaired()} returns {@code false}.
     */
    public Iterator<Tuple2<GATKRead, GATKRead>> getPairedIterator();

    /**
     * Returns an iterator over the reads overlapping the provided intervals, already in standard format.
     *
     * <p>Note: unmapped reads will not be returned by this iterator.
     *
     * @throws UnsupportedOperationException if the source of reads cannot be queried.
     */
    public Iterator<GATKRead> query(final List<SimpleInterval> locs);

    /** Gets a human-readable description of the read source. */
    public String getSourceDescription();

    /////////////////////////////////
    //// FACTORY OPEN METHODS

    /**
     * Sets the validation stringency.
     *
     * @return the same source for chaining with setters.
     *
     * @throws IllegalStateException if iteration already started.
     */
    public RTReadsSource setValidationStringency(final ValidationStringency stringency);

    /**
     * Sets if asynchronous reading should be used.
     *
     * @return the same source for chaining with setters.
     *
     * @throws IllegalStateException if iteration already started.
     */
    public RTReadsSource setUseAsyncIo(final boolean useAsyncIo);

    /**
     * Set the reference sequence for reading.
     *
     * @return the same source for chaining with setters.
     *
     * @throws IllegalStateException if iteration already started.
     */
    public RTReadsSource setReferenceSequence(final File referenceFile);


    /**
     * Forces the encoding to be one provided ignoring the detected by
     * {@link #getQualityEncoding()}.
     *
     * @param encoding if {@code null} do not force any encoding; otherwise, force the provided one
     *                 to be the real encoding.
     *
     * @return the same source for chaining with setters.
     */
    public RTReadsSource setForcedEncoding(final FastqQualityFormat encoding);

    /**
     * Sets the maximum number of reads to use for quality detection.
     *
     * @param maxNumberOfReadsForQuality maximum number of reads to use for quality detection.
     *
     * @return the same source for chaining with setters.
     */
    public RTReadsSource setMaxNumberOfReadsForQuality(final long maxNumberOfReadsForQuality);
}
