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
package org.magicdgs.io.writers.fastq;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import org.magicdgs.io.FastqPairedRecord;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.NoSuchElementException;

/**
 * Abstract implementation of FastqWriter for whatever implementation
 *
 * @author Daniel Gómez-Sánchez
 */
public abstract class SplitFastqWriterAbstract implements SplitFastqWriter {

	/**
	 * Hashtable mapping the identifier and the writer
	 */
	protected final Hashtable<String, ? extends FastqWriter> mapping;

	/**
	 * Hashtable mapping the identifier and how many times you perform a write on it with the methods implemented in
	 * this class
	 */
	protected final Hashtable<String, Integer> counts;

	/**
	 * All instances for the class will be performed by {@link ReadToolsFastqWriterFactory}
	 *
	 * @param mapping the mapping between the identifier and the FastqWriter
	 */
	protected SplitFastqWriterAbstract(Hashtable<String, ? extends FastqWriter> mapping) {
		this.mapping = mapping;
		this.counts = new Hashtable<>(mapping.size());
	}

	@Override
	public Hashtable<String, ? extends FastqWriter> getMapping() {
		return mapping;
	}

	@Override
	public Hashtable<String, Integer> getCurrentCount() {
		return counts;
	}

	@Override
	public void write(String identifier, FastqRecord record)
		throws NoSuchElementException, UnsupportedOperationException {
		FastqWriter writer = this.mapping.get(identifier);
		if (writer == null) {
			throw new NoSuchElementException("Identifier " + identifier + " is not included in this writer");
		}
		writer.write(record);
		Integer c = counts.putIfAbsent(identifier, 1);
		if (c != null) {
			counts.put(identifier, c + 1);
		}
	}

	@Override
	public void write(String identifier, FastqPairedRecord record)
		throws NoSuchElementException, UnsupportedOperationException {
		FastqWriter writer = this.mapping.get(identifier);
		if (writer == null) {
			throw new NoSuchElementException("Identifier " + identifier + " is not included in this writer");
		} else if (writer instanceof PairFastqWriters) {
			((PairFastqWriters) writer).write(record);
			Integer c = counts.putIfAbsent(identifier, 1);
			if (c != null) {
				counts.put(identifier, c + 1);
			}
		} else {
			throw new UnsupportedOperationException("This writer does not allow writing of FastqPairedRecords");
		}
	}

	@Override
	public void close() {
		HashSet<FastqWriter> writersSet = new HashSet<>(mapping.values());
		for (FastqWriter writer : writersSet) {
			writer.close();
		}
	}
}
