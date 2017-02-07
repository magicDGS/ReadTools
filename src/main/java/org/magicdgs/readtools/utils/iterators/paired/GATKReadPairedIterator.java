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

package org.magicdgs.readtools.utils.iterators.paired;

import htsjdk.samtools.SamPairUtil;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract class to encapsulate iteration over pair-end reads from an interleaved file and a pair
 * of files.
 *
 * Notes: iterators passed to {@link #of(Iterator[])} are expected to set properly the pair-end
 * flags, and a {@link htsjdk.samtools.SAMException} will be thrown if they are not.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class GATKReadPairedIterator
        implements Iterator<Tuple2<GATKRead, GATKRead>>, Iterable<Tuple2<GATKRead, GATKRead>> {

    /**
     * Creates a paired iterator from an array of iterators.
     *
     * Note: iterators are expected to set the pair-end flags properly.
     *
     * @param iterators one or two iterators over read to iterate as pairs.
     *
     * @return the paired-end iterator.
     */
    public static GATKReadPairedIterator of(final Iterator<GATKRead>... iterators) {
        Utils.nonNull(iterators, "null iterators");
        switch (iterators.length) {
            case 1:
                return new InterleavedGATKReadPairedIterator(iterators[0]);
            case 2:
                return new SplitGATKReadPairedIterator(iterators[0], iterators[1]);
            default:
                throw new IllegalArgumentException(
                        "Only 1 or 2 iterators could be used in GATKReadPairedIterator");
        }
    }

    /**
     * Returns the next paired record for the reader.
     *
     * @return non-null pair of reads; the tuple should not be ordered by pair number.
     *
     * @throws IllegalStateException if two next reads could not be retrieved.
     */
    public abstract Tuple2<GATKRead, GATKRead> getNextPair();

    /**
     * Note: this method is not expected to throw any error if one of the iterators is exhausted
     *
     * {@inheritDoc}
     */
    @Override
    public abstract boolean hasNext();

    /**
     * Default implementation returns the same iterator.
     *
     * {@inheritDoc}
     */
    public final Iterator<Tuple2<GATKRead, GATKRead>> iterator() {
        return this;
    }

    /**
     * Returns the next pair of reads with proper flags and stored header.
     *
     * {@inheritDoc}
     *
     * @throws IllegalStateException        if the paired reads could not be retrieved.
     * @throws htsjdk.samtools.SAMException if the reads are not properly paired.
     */
    public final Tuple2<GATKRead, GATKRead> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Exhausted iterator");
        }
        final Tuple2<GATKRead, GATKRead> next = getNextPair();
        // if this happen, it is an implementation error
        if (next == null || next._1 == null || next._2 == null) {
            throw new GATKException.ShouldNeverReachHereException(
                    "BUG: " + this.getClass() + ".getNextPair() return null elements");
        }
        // assert that they are paired with the htsjdk API
        SamPairUtil
                .assertMate(next._1.convertToSAMRecord(null), next._2.convertToSAMRecord(null));
        return next;
    }

}
