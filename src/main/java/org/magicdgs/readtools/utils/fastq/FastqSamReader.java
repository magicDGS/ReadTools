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

package org.magicdgs.readtools.utils.fastq;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderCustomTypes;
import htsjdk.samtools.fastq.FastqEncoder;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.IOUtil;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class FastqSamReader implements SamReader {

    private static final String QUERY_FASTQ_FILES_ERROR = "Cannot query a FASTQ file";

    // FASTQ headers are assumed to be unsorted for FASTQ files
    // TODO: maybe we should assume queryname group order? - maybe upon construction!
    private static final SAMFileHeader createFastqHeader() {
        final SAMFileHeader header = new SAMFileHeader();
        header.setSortOrder(SAMFileHeader.SortOrder.unsorted);
        return header;
    }

    private final SAMFileHeader header;
    private final Path path;

    private FastqReader reader;

    public FastqSamReader(final Path path) {
        this.path = path;
        this.header = createFastqHeader();
    }

    @Override
    public SAMFileHeader getFileHeader() {
        return header;
    }

    @Override
    public Type type() {
        return SamReaderCustomTypes.FASTQ_TYPE;
    }

    @Override
    public String getResourceDescription() {
        return path.toUri().toString();
    }

    @Override
    public boolean hasIndex() {
        // does not have index
        return false;
    }

    @Override
    public Indexing indexing() {
        throw new UnsupportedOperationException("Indexing is not supported for FASTQ files");
    }

    @Override
    public SAMRecordIterator iterator() {
        if (reader != null) {
            throw new IllegalStateException("Iteration in progress.");
        }
        this.reader = new FastqReader(IOUtil.openFileForBufferedReading(path));
        return new RecordIterator();
    }

    @Override
    public SAMRecordIterator query(String sequence, int start, int end, boolean contained) {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public SAMRecordIterator queryOverlapping(String sequence, int start, int end) {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public SAMRecordIterator queryContained(String sequence, int start, int end) {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public SAMRecordIterator query(QueryInterval[] intervals, boolean contained) {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public SAMRecordIterator queryOverlapping(QueryInterval[] intervals) {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public SAMRecordIterator queryContained(QueryInterval[] intervals) {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public SAMRecordIterator queryUnmapped() {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public SAMRecordIterator queryAlignmentStart(String sequence, int start) {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public SAMRecord queryMate(SAMRecord rec) {
        throw new UnsupportedOperationException(QUERY_FASTQ_FILES_ERROR);
    }

    @Override
    public void close() throws IOException {
        // TODO: should we close the reader here?
    }

    private class RecordIterator implements SAMRecordIterator {

        @Override
        public SAMRecordIterator assertSorted(SAMFileHeader.SortOrder sortOrder) {
            // TODO: log a warning or set a real assertion for sort order
            return this;
        }

        @Override
        public boolean hasNext() {
            return reader.hasNext();
        }

        @Override
        public SAMRecord next() {
            // TODO: this should be encoded in a different way (as in the FastqGATKWriter
            // TODO: we also can add some more interesting stuff, such as validation of read names
            // TODO: assuming the same ReadName encoding to sped up processing (and change if it fails)
            return FastqEncoder.asSAMRecord(reader.next(), header);
        }

        @Override
        public void close() {
            // TODO: should we close the reader here?
        }
    }
}
