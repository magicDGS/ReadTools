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

/**
 * Wraps a single iterator which contains paired-reads one after the other.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class InterleavedGATKReadPairedIterator extends GATKReadPairedIterator {

    private final Iterator<GATKRead> interleavedIterator;
    private Tuple2<GATKRead, GATKRead> next;


    /**
     * Constructor for an interleaved GATKRead iterator.
     *
     * Note: the iterator is expected to return reads with the paired flags correctly set.
     *
     * @param interleavedIterator iterator where we expect pairs one after the other.
     */
    InterleavedGATKReadPairedIterator(final Iterator<GATKRead> interleavedIterator) {
        this.interleavedIterator = Utils.nonNull(interleavedIterator, "null interleaved iterator");
        assignNext();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return next != null;
    }


    /** {@inheritDoc} */
    @Override
    public Tuple2<GATKRead, GATKRead> getNextPair() {
        final Tuple2<GATKRead, GATKRead> toReturn = this.next;
        // this allows to throw the exception when the broken pair is requested
        if (toReturn._2 == null) {
            throw new IllegalStateException(
                    "Interleaved iterator does not have an even number of pairs");
        }
        assignNext();
        return toReturn;
    }

    private void assignNext() {
        // if it does not have a next one, assing null to mark the end
        if (!interleavedIterator.hasNext()) {
            next = null;
        } else {
            // if the interleaved iterator is broken, it will delegate the exception to the other
            // marking the second as null
            next = new Tuple2<>(interleavedIterator.next(), (interleavedIterator.hasNext())
                    ? interleavedIterator.next() : null);
        }
    }

}
