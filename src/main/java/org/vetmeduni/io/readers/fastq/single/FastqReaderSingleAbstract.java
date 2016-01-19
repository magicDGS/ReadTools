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
package org.vetmeduni.io.readers.fastq.single;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.vetmeduni.utils.fastq.QualityUtils;
import org.vetmeduni.utils.fastq.StandardizerAndChecker;

import java.io.BufferedReader;
import java.io.File;

/**
 * Abstract wrapper for the {@link htsjdk.samtools.fastq.FastqReader} to include it in the ReadTools interface
 *
 * @author Daniel Gómez-Sánchez
 */
public abstract class FastqReaderSingleAbstract extends FastqReader implements FastqReaderSingleInterface {

	/**
	 * Checker for the reader
	 */
	protected StandardizerAndChecker checker;

	public FastqReaderSingleAbstract(File file) {
		this(file, false);
	}

	public FastqReaderSingleAbstract(File file, boolean skipBlankLines) {
		super(file, skipBlankLines);
		init();
	}

	public FastqReaderSingleAbstract(BufferedReader reader) {
		this(null, reader);
	}

	public FastqReaderSingleAbstract(File file, BufferedReader reader, boolean skipBlankLines) {
		super(file, reader, skipBlankLines);
		init();
	}

	public FastqReaderSingleAbstract(File file, BufferedReader reader) {
		this(file, reader, false);
	}

	/**
	 * Get the encoding for the file
	 */
	private void init() {
		checker = new StandardizerAndChecker(QualityUtils.getFastqQualityFormat(this.getFile()));
	}

	/**
	 * Get the next record directly from {@link htsjdk.samtools.fastq.FastqReader#next()}
	 *
	 * @return the record as is
	 */
	protected FastqRecord nextUnchangedRecord() {
		return super.next();
	}

	@Override
	public abstract FastqRecord next();

	@Override
	public FastqQualityFormat getOriginalEncoding() {
		return checker.getEncoding();
	}
}
