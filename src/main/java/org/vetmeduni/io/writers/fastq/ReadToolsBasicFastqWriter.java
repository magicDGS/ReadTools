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

/**
 * Wrapper for the htsjdk FastqWriters
 *
 * @author Daniel Gómez-Sánchez
 */
public class ReadToolsBasicFastqWriter implements ReadToolsFastqWriter {

	private final FastqWriter writer;

	protected ReadToolsBasicFastqWriter(FastqWriter writer) {
		this.writer = writer;
	}

	@Override
	public void write(FastqPairedRecord rec) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not allow FastqPairedRecords");
	}

	@Override
	public void write(FastqRecord rec) {
		writer.write(rec);
	}

	@Override
	public void close() {
		writer.close();
	}
}