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
package org.vetmeduni.io.readers.single;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.vetmeduni.utils.record.FastqRecordUtils;

import java.io.BufferedReader;
import java.io.File;

/**
 * FastqReader that only returns Sanger formatted reads
 *
 * @author Daniel Gómez-Sánchez
 */
public class FastqReaderSingleSanger extends FastqReaderWrapper implements FastqReaderSingleInterface {

	public FastqReaderSingleSanger(File file) {
		super(file);
	}

	public FastqReaderSingleSanger(File file, boolean skipBlankLines) {
		super(file, skipBlankLines);
	}

	public FastqReaderSingleSanger(BufferedReader reader) {
		super(reader);
	}

	public FastqReaderSingleSanger(File file, BufferedReader reader, boolean skipBlankLines) {
		super(file, reader, skipBlankLines);
	}

	public FastqReaderSingleSanger(File file, BufferedReader reader) {
		super(file, reader);
	}

	/**
	 * Next always return a Sanger formatted record
	 *
	 * @return the next record
	 */
	@Override
	public FastqRecord next() {
		if(encoding.equals(FastqQualityFormat.Standard)) {
			return super.next();
		}
		return FastqRecordUtils.copyToSanger(super.next());
	}

	@Override
	public FastqQualityFormat getFastqQuality() {
		return FastqQualityFormat.Standard;
	}
}
