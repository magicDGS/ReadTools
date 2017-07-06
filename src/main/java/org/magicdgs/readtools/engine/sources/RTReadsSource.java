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
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.io.Closeable;
import java.io.File;
import java.util.Iterator;

/**
 * Interface representing a source of reads. It allows to open an iteration over a source of reads,
 * in either single-end mode or pair-end mode.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface RTReadsSource extends Closeable {

    /**
     * Returns the quality encoding for the source of reads. Usually the quality is guessed by
     * reading {@link org.magicdgs.readtools.RTDefaults#MAX_RECORDS_FOR_QUALITY}.
     *
     * <p>Note: if {@link #setForcedEncoding(FastqQualityFormat)} was called with to force a
     * concrete quality, this method will return that encoding. This should log a warning if it
     * differs for the guessed quality at least the first time that it is called.
     */
    public FastqQualityFormat getQualityEncoding();

    /** Gets the header for the source of reads. It may be a simple header, but never {@code null}. */
    public SAMFileHeader getHeader();

    /**
     * Returns an iterator over the reads, already in standard format.
     *
     * <p>Note: if {@link #isPaired()} is {@code true}, it should return an interleaved iterator.
     *
     * @throws IllegalStateException if the iteration already started.
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

    /** Gets the format name of the read source. For example, SAM or FASTQ. */
    public String getSourceFormat();

    /**
     * Gets a human-readable name of the read source. For example, input.bam or input.fq.gz
     *
     * <p>Note: it may return the whole path of the file.
     */
    public String getSourceName();

    /////////////////////////////////
    //// METHODS FOR SETTING OPTIONS

    /**
     * Sets the validation stringency.
     *
     * <p>Note: some implementations may ignore validation stringency.
     *
     * @return the same source for chaining with setters.
     *
     * @throws IllegalStateException if iteration already started.
     */
    public RTReadsSource setValidationStringency(final ValidationStringency stringency);

    /**
     * Sets if asynchronous reading should be used.
     *
     * <p>Note: some implementations may ignore asynchronous reading.
     *
     * @return the same source for chaining with setters.
     *
     * @throws IllegalStateException if iteration already started.
     */
    public RTReadsSource setUseAsyncIo(final boolean useAsyncIo);

    /**
     * Set the reference sequence for reading.
     *
     * <p>Note: some implementations may ignore the reference file.
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
     *
     * @throws IllegalStateException if the quality was already detected.
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

    /**
     * {@inheritDoc}
     * Exceptions should be catched internally.
     */
    public void close();
}
