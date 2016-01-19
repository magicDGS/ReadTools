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
package org.vetmeduni.utils.fastq;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.utils.record.SAMRecordUtils;

/**
 * Class to randomly check qualities in the records that are input
 *
 * @author Daniel Gómez-Sánchez
 */
public class StandardizerAndChecker {

	// each 1000 reads the quality will be checked
	protected static final int frequency = 1000;

	// The encoding for this checker
	private final FastqQualityFormat encoding;

	// the number of records that passed by this count
	protected int count = 0;

	/**
	 * Default constructor
	 *
	 * @param encoding the encoding associated with the detector
	 */
	public StandardizerAndChecker(final FastqQualityFormat encoding) {
		this.encoding = encoding;
	}

	/**
	 * Get the encoding for the checker
	 *
	 * @return the underlying encoding
	 */
	public FastqQualityFormat getEncoding() {
		return encoding;
	}

	/**
	 * If by sampling is time to check, check the quality of the record
	 *
	 * @param record the record to check
	 *
	 * @throws org.vetmeduni.utils.fastq.QualityUtils.QualityException if the quality is checked and misencoded
	 */
	public void checkMisencoded(FastqRecord record) {
		// TODO: this should be synchronized?
		if (++count >= frequency) {
			count = 0;
			checkMisencoded((Object) record);
		}
	}

	/**
	 * If by sampling is time to check, check the quality of the record
	 *
	 * @param record the record to check
	 *
	 * @throws org.vetmeduni.utils.fastq.QualityUtils.QualityException if the quality is checked and misencoded
	 */
	public void checkMisencoded(FastqPairedRecord record) {
		// TODO: this should be synchronized
		if (++count >= frequency) {
			count = 0;
			checkMisencoded((Object) record.getRecord1());
			checkMisencoded((Object) record.getRecord2());
		}
	}

	/**
	 * If by sampling is time to check, check the quality of the record
	 *
	 * @param record the record to check
	 *
	 * @throws org.vetmeduni.utils.fastq.QualityUtils.QualityException if the quality is checked and misencoded
	 */
	public void checkMisencoded(SAMRecord record) {
		// TODO: this should be synchronized?
		if (++count >= frequency) {
			count = 0;
			checkMisencoded((Object) record);
		}
	}

	/**
	 * Check an object (instance of SAMRecord, FastqRecord or FastqPairedRecord)
	 *
	 * @param record the record to check
	 */
	protected void checkMisencoded(Object record) {
		final byte[] quals;
		if (record instanceof SAMRecord) {
			quals = ((SAMRecord) record).getBaseQualityString().getBytes();
		} else if (record instanceof FastqRecord) {
			quals = ((FastqRecord) record).getBaseQualityString().getBytes();
		} else {
			throw new IllegalArgumentException("checkMisencoded only accepts FastqRecord/SAMRecord");
		}
		QualityUtils.checkEncoding(quals, encoding);
	}

	/**
	 * Standardize a record, checking at the same time the quality
	 *
	 * @param record the record to standardize
	 *
	 * @return a new record with the standard encoding or the same if the encoder is sanger
	 * @throws org.vetmeduni.utils.fastq.QualityUtils.QualityException if the conversion causes a misencoded quality
	 */
	public FastqRecord standardize(FastqRecord record) {
		if (QualityUtils.isStandard(encoding)) {
			checkMisencoded(record);
			return record;
		}
		byte[] asciiQualities = record.getBaseQualityString().getBytes();
		byte[] newQualities = new byte[asciiQualities.length];
		for (int i = 0; i < asciiQualities.length; i++) {
			newQualities[i] = QualityUtils.byteToSanger(asciiQualities[i]);
			QualityUtils.checkStandardEncoding(newQualities[i]);
		}
		return new FastqRecord(record.getReadHeader(), record.getReadString(), record.getBaseQualityHeader(),
			new String(newQualities));
	}

	/**
	 * Standardize a record, checking at the same time the quality
	 *
	 * @param record the record to standardize
	 *
	 * @return a new record with the standard encoding or the same if the encoder is sanger
	 * @throws org.vetmeduni.utils.fastq.QualityUtils.QualityException if the conversion causes a misencoded quality
	 */
	public FastqPairedRecord standardize(FastqPairedRecord record) {
		FastqRecord record1 = standardize(record.getRecord1());
		FastqRecord record2 = standardize(record.getRecord2());
		return new FastqPairedRecord(record1, record2);
	}

	/**
	 * Standardize a record, checking at the same time the quality
	 *
	 * @param record the record to standardize
	 *
	 * @return a new record with the standard encoding or the same if the encoder is sanger
	 * @throws org.vetmeduni.utils.fastq.QualityUtils.QualityException if the conversion causes a misencoded quality
	 */
	public SAMRecord standardize(SAMRecord record) {
		if (QualityUtils.isStandard(encoding)) {
			checkMisencoded(record);
			return record;
		}
		try {
			SAMRecord newRecord = (SAMRecord) record.clone();
			// relies on the checking of the record
			SAMRecordUtils.toSanger(newRecord);
			return newRecord;
		} catch (CloneNotSupportedException e) {
			// This should not happen, because it is suppose to be implemented
			throw new RuntimeException("Unreachable code: " + e.getMessage());
		}
	}
}
