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

import org.broadinstitute.barclay.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Wraps an iterator and a function to encode the records into {@link GATKRead}. This is useful to
 * use implementations of different readers and convert to reads while iterating.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RecordToReadIterator<T> implements Iterator<GATKRead>, Iterable<GATKRead> {

    // underlying iterator over reads
    private final Iterator<T> underlyingIterator;
    // encoder of GATK reads
    private final Function<T, GATKRead> encoder;

    /**
     * Constructor.
     *
     * @param underlyingIterator underlying iterator over records.
     * @param encoder            function to encode each record into a {@link GATKRead}.
     */
    public RecordToReadIterator(final Iterator<T> underlyingIterator,
            final Function<T, GATKRead> encoder) {
        this.underlyingIterator = Utils.nonNull(underlyingIterator);
        this.encoder = Utils.nonNull(encoder);
    }

    @Override
    public boolean hasNext() {
        return underlyingIterator.hasNext();
    }

    @Override
    public GATKRead next() {
        return encoder.apply(underlyingIterator.next());
    }

    /**
     * {@inheritDoc}
     *
     * <p>WARNING: this iterator does not start from the beginning of the iteration.
     */
    @Override
    public Iterator<GATKRead> iterator() {
        return this;
    }
}
