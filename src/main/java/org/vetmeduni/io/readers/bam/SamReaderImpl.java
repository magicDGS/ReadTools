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
package org.vetmeduni.io.readers.bam;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.FastqQualityFormat;

import java.io.File;

/**
 * SamReader implementation for ReadTools
 *
 * @author Daniel Gómez-Sánchez
 */
public class SamReaderImpl extends SamReaderAbstract {

	public SamReaderImpl(File file) {
		super(file);
	}

	public SamReaderImpl(File file, ValidationStringency stringency) {
		super(file, stringency);
	}

	public SamReaderImpl(File file, SamReaderFactory factory) {
		super(file, factory);
	}

	@Override
	SAMRecordIterator toReturnIterator(SAMRecordIterator iterator) {
		return SamRecordIteratorWithStandardizer.of(iterator, checker, false);
	}

	/**
	 * Returns the original encoding
	 *
	 * @return the original encoding
	 */
	@Override
	public FastqQualityFormat getFastqQuality() {
		return getOriginalEncoding();
	}

	@Override
	public SAMRecord queryMate(SAMRecord rec) {
		SAMRecord toReturn = reader.queryMate(rec);
		checker.checkMisencoded(toReturn);
		return toReturn;
	}
}
