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

package org.magicdgs.readtools.utils.iterators;

import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Interleaves two iterators from pair-end data.
 *
 * Note: It assumes that both iterators have the same number of reads.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class InterleaveGATKReadIterators implements Iterator<GATKRead>, Iterable<GATKRead> {

    private final Iterator<GATKRead> firstIterator;
    private final Iterator<GATKRead> secondIterator;
    private boolean first;

    /**
     * Interleaves two iterators over GATKRead.
     *
     * @param first  the first one.
     * @param second the second one.
     */
    public InterleaveGATKReadIterators(final Iterator<GATKRead> first,
            final Iterator<GATKRead> second) {
        Utils.nonNull(first, "null first");
        Utils.nonNull(second, "null second");
        this.firstIterator = first;
        this.secondIterator = second;
        this.first = true;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<GATKRead> iterator() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        // if any of the two have a next one, there is a next record
        return firstIterator.hasNext() || secondIterator.hasNext();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the iterator that should return the read is exhausted.
     */
    @Override
    public GATKRead next() {
        if (hasNext()) {
            try {
                final GATKRead toReturn = (first) ? firstIterator.next() : secondIterator.next();
                first = !first;
                return toReturn;
            } catch (NoSuchElementException e) {
                throw new IllegalStateException(
                        "Unexpected exhausted iterator for " + ((first) ? "first" : "second")
                                + " pair");
            }
        }
        throw new NoSuchElementException("No more reads");
    }
}
