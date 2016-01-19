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
package org.vetmeduni.io.writers.fastq;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import org.vetmeduni.io.FastqPairedRecord;

import java.util.Hashtable;
import java.util.NoSuchElementException;

/**
 * Interface for split writer for fastq files
 *
 * @author Daniel Gómez-Sánchez
 */
public interface SplitFastqWriter extends ReadToolsFastqWriter {

	/**
	 * The mapping between the identifier and the corresponding FastqWriter. Several identifiers could be associated
	 * with the same writer
	 *
	 * @return the mapping identifier-writer
	 */
	public Hashtable<String, ? extends FastqWriter> getMapping();

	/**
	 * Get the number of times that each of the identifiers' writers was used
	 *
	 * @return the map between the identifier and the current report
	 */
	public Hashtable<String, Integer> getCurrentCount();

	/**
	 * Get the report for each identifier (the report could be whatever object
	 *
	 * @return the map between the identifier and the current report
	 */
	public Hashtable<String, Object> getCurrentReport();

	/**
	 * Write a FastqRecord into the split fastq writer
	 *
	 * @param identifier the identifier for the record
	 * @param record     the record to write
	 *
	 * @throws java.util.NoSuchElementException        if the identifier is not in the mapping (or <code>null</code>)
	 * @throws java.lang.UnsupportedOperationException if the writer does not allow a FastqRecords
	 */
	public void write(String identifier, FastqRecord record)
		throws NoSuchElementException, UnsupportedOperationException;

	/**
	 * Write a FastqRecord that includes some information for the identifier, or in the default writer depending on the
	 * implementation
	 *
	 * @param record the record to write
	 *
	 * @throws java.util.NoSuchElementException        if the detected identifier is not in the mapping or there are no
	 *                                                 default writer
	 * @throws java.lang.UnsupportedOperationException if the writer does noa allow FastqRecords
	 */
	public void write(FastqRecord record);

	/**
	 * Write a FastqPairedRecord into the split fastq writer
	 *
	 * @param identifier the identifier for the record
	 * @param record     the record to write
	 *
	 * @throws java.util.NoSuchElementException        if the identifier is not in the mapping (or <code>null</code>)
	 * @throws java.lang.UnsupportedOperationException if the writer does not allow a FastqPairedRecords
	 */
	public void write(String identifier, FastqPairedRecord record)
		throws NoSuchElementException, UnsupportedOperationException;

	/**
	 * Write a FastqPairedRecord that includes some information for the identifier, or in the default writer depending
	 * on the implementation
	 *
	 * @param record the record to write
	 *
	 * @throws java.util.NoSuchElementException        if the detected identifier is not in the mapping or there are no
	 *                                                 default writer
	 * @throws java.lang.UnsupportedOperationException if the writer does noa allow FastqPairedRecords
	 */
	public void write(FastqPairedRecord record);
}
