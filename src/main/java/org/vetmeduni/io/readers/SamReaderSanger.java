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
package org.vetmeduni.io.readers;

import htsjdk.samtools.*;
import htsjdk.samtools.util.FastqQualityFormat;
import org.vetmeduni.utils.fastq.QualityUtils;
import org.vetmeduni.utils.record.SAMRecordUtils;

import java.io.File;
import java.io.IOException;

/**
 * SamReader implementation that returns only records with Sanger formatting. It wraps a {@link
 * htsjdk.samtools.SamReader} It only could be constructed by a File
 *
 * @author Daniel Gómez-Sánchez
 */
public class SamReaderSanger implements SamReader {

	private final SamReader reader;

	private final FastqQualityFormat encoding;

	/**
	 * Creates a SamReaderSanger from a file, with default SamReaderFactory
	 *
	 * @param file the file
	 */
	public SamReaderSanger(File file) {
		this(file, SamReaderFactory.makeDefault());
	}

	/**
	 * Creates a SamReaderSanger with the default SamReaderFactory and the provided validation stringency
	 *
	 * @param file       the file
	 * @param stringency the validation stringency
	 */
	public SamReaderSanger(File file, ValidationStringency stringency) {
		this(file, SamReaderFactory.makeDefault().validationStringency(stringency));
	}

	/**
	 * Creates a SamReaderSanger with the provided factory (it only open the file)
	 *
	 * @param file    the file
	 * @param factory the factory
	 */
	public SamReaderSanger(File file, SamReaderFactory factory) {
		this.reader = factory.open(file);
		this.encoding = QualityUtils.getFastqQualityFormat(file);
	}

	@Override
	public SAMFileHeader getFileHeader() {
		return reader.getFileHeader();
	}

	@Override
	public Type type() {
		return reader.type();
	}

	@Override
	public String getResourceDescription() {
		return reader.getResourceDescription();
	}

	@Override
	public boolean hasIndex() {
		return reader.hasIndex();
	}

	@Override
	public Indexing indexing() {
		return reader.indexing();
	}

	@Override
	public SAMRecordIterator iterator() {
		return toReturnIterator(reader.iterator());
	}

	@Override
	public SAMRecordIterator query(String sequence, int start, int end, boolean contained) {
		return toReturnIterator(reader.query(sequence, start, end, contained));
	}

	@Override
	public SAMRecordIterator queryOverlapping(String sequence, int start, int end) {
		return toReturnIterator(reader.queryOverlapping(sequence, start, end));
	}

	@Override
	public SAMRecordIterator queryContained(String sequence, int start, int end) {
		return toReturnIterator(reader.queryContained(sequence, start, end));
	}

	@Override
	public SAMRecordIterator query(QueryInterval[] intervals, boolean contained) {
		return toReturnIterator(reader.query(intervals, contained));
	}

	@Override
	public SAMRecordIterator queryOverlapping(QueryInterval[] intervals) {
		return toReturnIterator(reader.queryOverlapping(intervals));
	}

	@Override
	public SAMRecordIterator queryContained(QueryInterval[] intervals) {
		return toReturnIterator(reader.queryContained(intervals));
	}

	@Override
	public SAMRecordIterator queryUnmapped() {
		return toReturnIterator(reader.queryUnmapped());
	}

	@Override
	public SAMRecordIterator queryAlignmentStart(String sequence, int start) {
		return toReturnIterator(reader.queryAlignmentStart(sequence, start));
	}

	@Override
	public SAMRecord queryMate(SAMRecord rec) {
		return (QualityUtils.isStandard(encoding)) ?
			reader.queryMate(rec) :
			SAMRecordUtils.copyToSanger(reader.queryMate(rec));
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	/**
	 * Get the iterator that should be returned depending on the encoding in the original file
	 *
	 * @param iterator the iterator in the original file
	 *
	 * @return the iterator, either wrapped or not
	 */
	private SAMRecordIterator toReturnIterator(final SAMRecordIterator iterator) {
		return (QualityUtils.isStandard(encoding)) ? iterator : SAMRecordSangerIterator.of(iterator);
	}

	/**
	 * Class that back a {@link htsjdk.samtools.SAMRecordIterator} and only return Sanger encoded records
	 */
	public static class SAMRecordSangerIterator implements SAMRecordIterator {

		private SAMRecordIterator iterator;

		static SAMRecordSangerIterator of(final SAMRecordIterator iterator) {
			return new SAMRecordSangerIterator(iterator);
		}

		/**
		 * Construct an iterator with an underlying iterator
		 *
		 * @param iterator
		 */
		private SAMRecordSangerIterator(SAMRecordIterator iterator) {
			this.iterator = iterator;
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

		@Override
		public SAMRecord next() {
			return SAMRecordUtils.copyToSanger(iterator.next());
		}
	}
}
