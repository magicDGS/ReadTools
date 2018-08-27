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

package org.magicdgs.readtools.engine.sourcehandler;

import org.magicdgs.readtools.utils.fastq.FastqGATKRead;
import org.magicdgs.readtools.utils.iterators.RecordToReadIterator;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Iterator;
import java.util.List;

/**
 * Source handler for FASTQ files.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final public class FastqSourceHandler extends FileSourceHandler<FastqReader> {

    // FASTQ headers are assumed to be unsorted for FASTQ files
    private static final SAMFileHeader FASTQ_HEADER = new SAMFileHeader();
    static {
        FASTQ_HEADER.setSortOrder(SAMFileHeader.SortOrder.unsorted);
    }

    /**
     * Constructor from a a source.
     *
     * @param source the source of reads (FASTQ).
     */
    public FastqSourceHandler(final String source, final ReadReaderFactory factory) {
        super(source, factory);
    }

    /** Returns an empty header with unsorted order. */
    // Override because this source does not have header
    @Override
    public SAMFileHeader getHeader() {
        return FASTQ_HEADER.clone();
    }

    @Override
    public Iterator<GATKRead> toIntervalIterator(final List<SimpleInterval> locs) {
        throw new UnsupportedOperationException("FASTQ files does not support querying intervals");
    }

    protected FastqReader getFreshReader() {
        return factory.openFastqReader(path);
    }

    @Override
    protected SAMFileHeader getReaderHeader(final FastqReader reader) {
        throw new GATKException.ShouldNeverReachHereException("This method should not be called");
    }

    @Override
    protected Iterator<GATKRead> getReaderIntervalIterator(FastqReader reader,
            List<SimpleInterval> locs) {
        throw new GATKException.ShouldNeverReachHereException("This method should not be called");
    }

    @Override
    protected FastqQualityFormat getReaderQualityEncoding(final FastqReader reader,
            long maxNumberOfReads) {
        return QualityEncodingDetector.detect(maxNumberOfReads, reader);
    }

    @Override
    protected Iterator<GATKRead> getReaderIterator(final FastqReader reader) {
        return new RecordToReadIterator<>(reader.iterator(), FastqGATKRead::new);
    }
}
