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

import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;

import java.util.NoSuchElementException;

/**
 * Interface for split writer for BAM/SAM files
 *
 * @author Daniel Gómez-Sánchez
 */
public interface SplitSAMFileWriter extends SAMFileWriter {

    /**
     * Write a SAMRecord that includes some information for the identifier, or in the default writer
     * depending on the
     * implementation
     *
     * @param alignment the record to write
     *
     * @throws java.util.NoSuchElementException if the detected identifier is not in the mapping or
     *                                          there are no default
     *                                          writer
     */
    void addAlignment(SAMRecord alignment);

    /**
     * Write a SAMRecord into the split fastq writer
     *
     * @param identifier the identifier for the record
     * @param alignment  the record to write
     *
     * @throws java.util.NoSuchElementException if the identifier is not in the mapping (or
     *                                          <code>null</code>)
     */
    void addAlignment(String identifier, SAMRecord alignment) throws NoSuchElementException;
}
