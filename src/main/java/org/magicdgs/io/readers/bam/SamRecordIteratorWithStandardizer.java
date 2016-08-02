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
package org.magicdgs.io.readers.bam;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import org.magicdgs.utils.fastq.StandardizerAndChecker;

/**
 * Class that back a {@link htsjdk.samtools.SAMRecordIterator} and either cheks or standardize the quality
 *
 * @author Daniel Gómez-Sánchez
 */
public abstract class SamRecordIteratorWithStandardizer implements SAMRecordIterator {

	protected SAMRecordIterator iterator;

	protected final StandardizerAndChecker standardizer;

	/**
	 * Construct an iterator with an underlying iterator
	 *
	 * @param iterator     the underlying iterator
	 * @param standardizer the standardizer/checker to use
	 */
	private SamRecordIteratorWithStandardizer(SAMRecordIterator iterator, final StandardizerAndChecker standardizer) {
		this.iterator = iterator;
		this.standardizer = standardizer;
	}

	@Override
	public SAMRecordIterator assertSorted(SAMFileHeader.SortOrder sortOrder) {
		iterator = iterator.assertSorted(sortOrder);
		return this;
	}

	@Override
	public void close() {
		iterator.close();
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	/**
	 * Get a SAMRecord iterator for ReadTools
	 *
	 * @param iterator     the underlying iterator
	 * @param standardizer the standardizer/checker to use
	 * @param standardize  <code>true</code> for standardize; <code>false</code> for only checks
	 *
	 * @return
	 */
	public static SAMRecordIterator of(final SAMRecordIterator iterator, StandardizerAndChecker standardizer,
		boolean standardize) {
		if (standardize) {
			return new SAMRecordSangerIterator(iterator, standardizer);
		} else {
			return new SAMRecordCheckIterator(iterator, standardizer);
		}
	}

	/**
	 * Only return Sanger encoded records
	 */
	private static class SAMRecordSangerIterator extends SamRecordIteratorWithStandardizer {

		private SAMRecordSangerIterator(SAMRecordIterator iterator, StandardizerAndChecker standardizer) {
			super(iterator, standardizer);
		}

		@Override
		public SAMRecord next() {
			return standardizer.standardize(iterator.next());
		}
	}

	/**
	 * Only checks for correctly encoded records
	 */
	private static class SAMRecordCheckIterator extends SamRecordIteratorWithStandardizer {

		private SAMRecordCheckIterator(SAMRecordIterator iterator, StandardizerAndChecker standardizer) {
			super(iterator, standardizer);
		}

		@Override
		public SAMRecord next() {
			SAMRecord toReturn = iterator.next();
			standardizer.checkMisencoded(toReturn);
			return toReturn;
		}
	}
}
