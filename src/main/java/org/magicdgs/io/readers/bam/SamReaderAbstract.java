/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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
 */
package org.magicdgs.io.readers.bam;

import org.magicdgs.readtools.utils.fastq.QualityUtils;
import org.magicdgs.readtools.utils.fastq.StandardizerAndChecker;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.FastqQualityFormat;

import java.io.File;
import java.io.IOException;

/**
 * Abstract wrapper for SamReader
 *
 * @author Daniel Gómez-Sánchez
 */
public abstract class SamReaderAbstract implements SamReaderInterface {

    /**
     * The backed reader
     */
    protected final SamReader reader;

    /**
     * The checker
     */
    protected final StandardizerAndChecker checker;

    /**
     * Creates a SamReaderSanger with the provided factory (it only open the file)
     *
     * @param file    the file
     * @param factory the factory
     */
    public SamReaderAbstract(final File file, final ReadReaderFactory factory,
            final boolean allowHigherSangerQualities) {
        this.reader = factory.openSamReader(file);
        this.checker = new StandardizerAndChecker(QualityUtils.getFastqQualityFormat(file),
                allowHigherSangerQualities);
    }

    /**
     * Get the SAMRecordIterator for this reader that either checks or standardize the qualities
     *
     * @param iterator the original iterator created by the reader
     *
     * @return a {@link org.magicdgs.io.readers.bam.SamRecordIteratorWithStandardizer}
     */
    abstract SAMRecordIterator toReturnIterator(final SAMRecordIterator iterator);

    @Override
    public FastqQualityFormat getOriginalEncoding() {
        return checker.getEncoding();
    }

    @Override
    public SAMFileHeader getFileHeader() {
        return reader.getFileHeader();
    }

    @Override
    public Type type() {
        return reader.type();
    }

    @Override
    public String getResourceDescription() {
        return reader.getResourceDescription();
    }

    @Override
    public boolean hasIndex() {
        return reader.hasIndex();
    }

    @Override
    public Indexing indexing() {
        return reader.indexing();
    }

    @Override
    public SAMRecordIterator iterator() {
        return toReturnIterator(reader.iterator());
    }

    @Override
    public SAMRecordIterator query(String sequence, int start, int end, boolean contained) {
        return toReturnIterator(reader.query(sequence, start, end, contained));
    }

    @Override
    public SAMRecordIterator queryOverlapping(String sequence, int start, int end) {
        return toReturnIterator(reader.queryOverlapping(sequence, start, end));
    }

    @Override
    public SAMRecordIterator queryContained(String sequence, int start, int end) {
        return toReturnIterator(reader.queryContained(sequence, start, end));
    }

    @Override
    public SAMRecordIterator query(QueryInterval[] intervals, boolean contained) {
        return toReturnIterator(reader.query(intervals, contained));
    }

    @Override
    public SAMRecordIterator queryOverlapping(QueryInterval[] intervals) {
        return toReturnIterator(reader.queryOverlapping(intervals));
    }

    @Override
    public SAMRecordIterator queryContained(QueryInterval[] intervals) {
        return toReturnIterator(reader.queryContained(intervals));
    }

    @Override
    public SAMRecordIterator queryUnmapped() {
        return toReturnIterator(reader.queryUnmapped());
    }

    @Override
    public SAMRecordIterator queryAlignmentStart(String sequence, int start) {
        return toReturnIterator(reader.queryAlignmentStart(sequence, start));
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
