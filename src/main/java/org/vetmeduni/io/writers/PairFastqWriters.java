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
package org.vetmeduni.io.writers;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.Lazy;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.utils.misc.Formats;

/**
 * Simple class to have two writers in pairs. It also could be use to write in single-end mode
 *
 * @author Daniel Gómez-Sánchez
 */
public class PairFastqWriters implements ReadToolsFastqWriter {

	private final static FastqWriterFactory DEFAULT_FACTORY = new FastqWriterFactory();

	// Pairs are always initializer; single is not
	private final FastqWriter first, second;

	private final Lazy<FastqWriter> single;

	private long countPairs, countSingle;

	/**
	 * Default constructor for the package
	 *
	 * @param first  writer for the first pair
	 * @param second writer for the second pair
	 * @param single lazy writer for the single pair
	 */
	public PairFastqWriters(FastqWriter first, FastqWriter second, Lazy<FastqWriter> single) {
		this.first = first;
		this.second = second;
		this.single = single;
		this.countPairs = 0;
		this.countSingle = 0;
	}

	/**
	 * Write in the first writer (not accesible: must to be paired)
	 *
	 * @param record the record to write
	 */
	private void writeFirst(FastqRecord record) {
		first.write(record);
	}

	/**
	 * Write in the second writer (not accesible: must to be paired)
	 *
	 * @param record the record to write
	 */
	private void writeSecond(FastqRecord record) {
		second.write(record);
	}

	/**
	 * Write in the single writer (the only accessible, because does not need to be paired)
	 *
	 * @param record the record to write
	 */
	@Override
	public void write(FastqRecord record) {
		countSingle++;
		single.get().write(record);
	}

	/**
	 * Write a FastqPairedRecord in the first and second pair (writing in pairs)
	 *
	 * @param record the record to write
	 */
	public void write(FastqPairedRecord record) {
		countPairs++;
		writeFirst(record.getRecord1());
		writeSecond(record.getRecord2());
	}

	public String toString() {
		return String.format("%s reads in pairs; %s reads without mate", Formats.commaFmt.format(countPairs),
			Formats.commaFmt.format(countSingle));
	}

	/**
	 * Close all the readers
	 */
	public void close() {
		first.close();
		second.close();
		if (single.isInitialized()) {
			single.get().close();
		}
	}
}
