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

import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Wraps two iterators (one for the first pair and one for the second) to return paired-end reads
 * in a tuple after checking that they are correct.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class SplitGATKReadPairedIterator extends GATKReadPairedIterator {

    private final Iterator<GATKRead> firstPairIterator;
    private final Iterator<GATKRead> secondPairIterator;

    /**
     * Constructor for an interleaved GATKRead iterator.
     *
     * Note: the iterators are expected to return reads with the paired flags correctly set.
     *
     * @param firstPairIterator  iterator for the first pair.
     * @param secondPairIterator iterator for the second pair.
     */
    SplitGATKReadPairedIterator(final Iterator<GATKRead> firstPairIterator,
            final Iterator<GATKRead> secondPairIterator) {
        Utils.nonNull(firstPairIterator, "null first iterator");
        Utils.nonNull(secondPairIterator, "null second iterator");
        this.firstPairIterator = firstPairIterator;
        this.secondPairIterator = secondPairIterator;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        // We expect that at least one of them contains a next record
        // Exceptions will be only thrown if we call next
        return firstPairIterator.hasNext() || secondPairIterator.hasNext();
    }

    /** {@inheritDoc} */
    @Override
    public Tuple2<GATKRead, GATKRead> getNextPair() {
        try {
            return new Tuple2<>(firstPairIterator.next(), secondPairIterator.next());
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(
                    "Split paired iterator: one of the iterators is exhausted");
        }
    }
}
