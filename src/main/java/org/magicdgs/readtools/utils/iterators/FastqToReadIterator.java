/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.utils.read.FastqGATKRead;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.fastq.FastqRecord;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Iterator;

/**
 * Wraps a FastqRecord iterator within an iterator of GATKReads.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqToReadIterator implements Iterator<GATKRead>, Iterable<GATKRead> {

    // wrapped FastqRecord iterator
    private final Iterator<FastqRecord> fastqIterator;
    // the header for all the GATKReads for this iterator.
    private final SAMFileHeader header;

    /**
     * Creates a new headerless GATKRead iterator from a {@link FastqRecord} iterator.
     *
     * @param fastqIterator non-null iterator.
     */
    public FastqToReadIterator(final Iterator<FastqRecord> fastqIterator) {
        this(fastqIterator, null);
    }

    /**
     * Creates a new GATKRead iterator from a {@link FastqRecord} iterator.
     *
     * @param fastqIterator non-null iterator.
     * @param header        the header for include in every returned reads. May be {@code null}.
     */
    public FastqToReadIterator(final Iterator<FastqRecord> fastqIterator, SAMFileHeader header) {
        Utils.nonNull(fastqIterator, "null iterator");
        this.fastqIterator = fastqIterator;
        this.header = header;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return fastqIterator.hasNext();
    }

    /** {@inheritDoc} */
    @Override
    public GATKRead next() {
        // every transformation is handle in the FastqGATKRead
        return new FastqGATKRead(header, fastqIterator.next());
    }

    /** WARNING: this iterator does not start from the begining. */
    @Override
    public Iterator<GATKRead> iterator() {
        return this;
    }

}
