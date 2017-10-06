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

package htsjdk.samtools;

import htsjdk.samtools.fastq.FastqEncoder;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO - maybe implement as a ReaderImplementation (File/Path-based)
public class FastqSamReader implements SamReader.PrimitiveSamReader {

    // TODO - maybe change the default extension to .fastq.gz
    private static final SamReader.Type FASTQ_TYPE = new SamReader.Type.TypeImpl("FASTQ", ".fastq", null);

    private final SAMFileHeader mHeader;


    private FastqReader mReader;
    private CloseableIterator<SAMRecord> mIterator;

    public FastqSamReader(final Path inputPath) {
        // initialize the header
        this.mHeader = new SAMFileHeader();
        this.mHeader.setSortOrder(SAMFileHeader.SortOrder.unsorted);
        // intialize the reader
        try {
            // TODO - handle GZIP and other stuff
            mReader = new FastqReader(Files.newBufferedReader(inputPath));
        } catch (IOException e) {
            throw new SAMException(e.getMessage(), e);
        }
    }

    @Override
    public SamReader.Type type() {
        return FASTQ_TYPE;
    }

    @Override
    public boolean hasIndex() {
        return false;
    }

    @Override
    public BAMIndex getIndex() {
        throw new UnsupportedOperationException("Cannot retrieve index for FASTQ files");
    }

    @Override
    public SAMFileHeader getFileHeader() {
        return mHeader;
    }

    @Override
    public CloseableIterator<SAMRecord> getIterator() {
        if (mReader == null) {
            throw new IllegalStateException("File reader is closed");
        }
        if (mIterator != null) {
            throw new IllegalStateException("Iteration in progress");
        }
        mIterator = new RecordIterator();
        return mIterator;
    }

    @Override
    public CloseableIterator<SAMRecord> getIterator(SAMFileSpan fileSpan) {
        throw new UnsupportedOperationException("Cannot iterate over SAM file pointers for FASTQ files");
    }

    @Override
    public SAMFileSpan getFilePointerSpanningReads() {
        throw new UnsupportedOperationException("Cannot retrieve file pointers for FASTQ files");
    }

    @Override
    public CloseableIterator<SAMRecord> query(QueryInterval[] intervals, boolean contained) {
        throw new UnsupportedOperationException("Cannot query intervals for FASTQ files");
    }

    @Override
    public CloseableIterator<SAMRecord> queryAlignmentStart(String sequence, int start) {
        throw new UnsupportedOperationException("Cannot query intervals for FASTQ files");
    }

    @Override
    public CloseableIterator<SAMRecord> queryUnmapped() {
        return getIterator();
    }

    @Override
    public void close() {
        if (mReader != null) {
            try {
                mReader.close();
            } finally {
                mReader = null;
            }
        }
    }

    @Override
    public ValidationStringency getValidationStringency() {
        // TODO - this should have a normal validation stringency
        return ValidationStringency.SILENT;
    }


    private final class RecordIterator implements CloseableIterator<SAMRecord> {

        @Override
        public void close() {
            FastqSamReader.this.close();
        }

        @Override
        public boolean hasNext() {
            return mReader.hasNext();
        }

        @Override
        public SAMRecord next() {
            // TODO - use a better conversor between FASTQ - SAM using our framework
            // TODO - here we can also use a validation stringency
            return FastqEncoder.asSAMRecord(mReader.next(), mHeader);
        }
    }
}
