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
package org.magicdgs.io.writers.bam;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.ProgressLoggerInterface;

import java.util.Hashtable;
import java.util.NoSuchElementException;

/**
 * @author Daniel Gómez-Sánchez
 */
public abstract class SplitSAMFileWriterAbstract implements SplitSAMFileWriter {

    /**
     * Hashtable mapping the identifier and the writer
     */
    protected final Hashtable<String, ? extends SAMFileWriter> mapping;

    protected final SAMFileHeader header;

    protected SplitSAMFileWriterAbstract(SAMFileHeader commonHeader,
            Hashtable<String, ? extends SAMFileWriter> mapping) {
        this.mapping = mapping;
        this.header = commonHeader;
    }

    @Override
    public void addAlignment(String identifier, SAMRecord alignment) throws NoSuchElementException {
        mapping.get(identifier).addAlignment(alignment);
    }

    @Override
    public SAMFileHeader getFileHeader() {
        return header;
    }

    @Override
    public void setProgressLogger(ProgressLoggerInterface progress) {
        for (SAMFileWriter w : mapping.values()) {
            w.setProgressLogger(progress);
        }
    }

    @Override
    public void close() {
        for (SAMFileWriter w : mapping.values()) {
            w.close();
        }
    }
}
