/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.utils.distmap.encoder;

import org.magicdgs.readtools.utils.distmap.DistmapException;

import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

/**
 * Interface for encode/decode {@link GATKRead} to the Distmap format.
 *
 * <p>Distmap (<a href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0072614">
 * Pandey &amp; Schlötterer 2013</a>) is a wrapper around different mappers for distributed
 * computation using Hadoop. The input for this tool is a modified FASTQ format which is written in
 * HDFS to save space and to distribute easily pair-end reads.
 *
 * <p>The interface is required to allow experimenting with other formats in distmap, and for
 * integration if the format changes.
 *
 * <p>WARNING: decoding is not required to be implemented, because currently ReadTools does not
 * support reading distmap encoded files.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface DistmapCodec {

    /**
     * Encodes a single-end read into a Distmap string.
     *
     * @param read the read to encode.
     */
    public String encode(final GATKRead read);

    /**
     * Encodes a pair-end read into a Distmap string.
     *
     * @param pair tuple with the first and the second reads in the pair.
     *
     * @throws DistmapException if there is an error while encoding (e.g., different read names)
     */
    public String encode(final Tuple2<GATKRead, GATKRead> pair);

    /**
     * Decodes a single-end Distmap String.
     *
     * @param distmapSingleString the string encoded in the Distmap format.
     *
     * @return decoded read.
     *
     * @throws DistmapException if the String is not properly formatted.
     * @throws UnsupportedOperationException if decoding is not implemented.
     */
    public GATKRead decodeSingle(final String distmapSingleString);

    /**
     * Decodes a pair-end Distmap String.
     *
     * @param distmapPairedString the string encoded in the Distmap format.
     *
     * @return pair of reads. The flags should be set to be first and second of pair.
     *
     * @throws DistmapException if the String is not properly formatted.
     * @throws UnsupportedOperationException if decoding is not implemented.
     */
    public Tuple2<GATKRead, GATKRead> decodePaired(final String distmapPairedString);


    /**
     * Checks if a Distmap String represents paired or unpaired data.
     *
     * @param distmapString the string encoded in the Distmap format.
     *
     * @return {@code true} if it is paired; {@code false} otherwise.
     *
     * @throws DistmapException if the String is not properly formatted.
     * @throws UnsupportedOperationException if decoding is not implemented.
     */
    public boolean isPaired(final String distmapString);

}
