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
package org.magicdgs.io.readers.fastq.paired;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.readtools.utils.fastq.QualityUtils;
import org.magicdgs.readtools.utils.fastq.StandardizerAndChecker;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract reader for pair-end reader with two files
 *
 * @author Daniel Gómez-Sánchez
 */
public abstract class FastqReaderPairedAbstract implements FastqReaderPairedInterface {

    private final FastqReader reader1;

    private final FastqReader reader2;

    protected final StandardizerAndChecker checker;

    protected Log logger;

    /**
     * Default constructor with two readers
     *
     * @param reader1 the first pair reader
     * @param reader2 the second pair reader
     *
     * @throws QualityUtils.QualityException if both files are encoding
     *                                       differently
     */
    public FastqReaderPairedAbstract(final FastqReader reader1, final FastqReader reader2,
            final boolean allowHighQualities) throws QualityUtils.QualityException {
        logger = Log.getInstance(this.getClass());
        this.reader1 = reader1;
        this.reader2 = reader2;
        this.checker =
                new StandardizerAndChecker(QualityUtils.getFastqQualityFormat(reader1.getFile()),
                        allowHighQualities);
        if (checker.getEncoding() != QualityUtils.getFastqQualityFormat(reader2.getFile())) {
            throw new QualityUtils.QualityException(
                    "Pair-end encoding is different for both read pairs");
        }
        logger.debug("Encoding for the original FASTQ reader: ", checker.getEncoding());
    }

    /**
     * Constructor for two files
     *
     * @param reader1 the first pair file
     * @param reader2 the second pair file
     *
     * @throws QualityUtils.QualityException if both files are encoding
     *                                       differently
     */
    public FastqReaderPairedAbstract(final File reader1, final File reader2,
            final boolean allowHighQualities) throws QualityUtils.QualityException {
        this(new FastqReader(reader1), new FastqReader(reader2), allowHighQualities);
    }

    /**
     * Close the two readers
     *
     * @throws IOException if some error occurs when closing
     */
    @Override
    public void close() throws IOException {
        reader1.close();
        reader2.close();
    }

    /**
     * Return this object (it is not returning a real new iterator)
     *
     * @return this object
     */
    @Override
    public Iterator<FastqPairedRecord> iterator() {
        return this;
    }

    /**
     * Check if there are more records
     *
     * @return <code>true</code> if there are more records; <code>false</code> otherwise
     *
     * @throws htsjdk.samtools.SAMException if only one of the pairs have another record
     */
    @Override
    public boolean hasNext() {
        if (reader1.hasNext() && reader2.hasNext()) {
            return true;
        }
        if (reader1.hasNext() || reader2.hasNext()) {
            throw new SAMException("Paired end files do not have equal length");
        }
        return false;
    }

    /**
     * Get the next record without change
     *
     * @return the next record
     *
     * @throws java.util.NoSuchElementException if there are no next record
     */
    public FastqPairedRecord nextUnchangedRecord() {
        if (!hasNext()) {
            throw new NoSuchElementException("next() called when !hasNext()");
        }
        return new FastqPairedRecord(reader1.next(), reader2.next());
    }

    @Override
    public FastqQualityFormat getOriginalEncoding() {
        return checker.getEncoding();
    }
}
