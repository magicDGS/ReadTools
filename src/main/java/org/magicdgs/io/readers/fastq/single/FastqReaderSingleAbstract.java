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
package org.magicdgs.io.readers.fastq.single;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.magicdgs.utils.fastq.QualityUtils;
import org.magicdgs.utils.fastq.StandardizerAndChecker;

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

	public FastqReaderSingleAbstract(final File file, final boolean allowHigherQualities) {
		this(file, false, allowHigherQualities);
	}

	public FastqReaderSingleAbstract(final File file, boolean skipBlankLines, final boolean allowHigherQualities) {
		super(file, skipBlankLines);
		init(allowHigherQualities);
	}

	public FastqReaderSingleAbstract(final BufferedReader reader, final boolean allowHigherQualities) {
		this(null, reader, allowHigherQualities);
	}

	public FastqReaderSingleAbstract(final File file, final BufferedReader reader, final boolean skipBlankLines, final boolean allowHigherQualities) {
		super(file, reader, skipBlankLines);
		init(allowHigherQualities);
	}

	public FastqReaderSingleAbstract(final File file, final BufferedReader reader, final boolean allowHigherQualities) {
		this(file, reader, false, allowHigherQualities);
	}

	/**
	 * Get the encoding for the file
	 */
	private void init(final boolean allowHigherQualities) {
		checker = new StandardizerAndChecker(QualityUtils.getFastqQualityFormat(this.getFile()), allowHigherQualities);
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
