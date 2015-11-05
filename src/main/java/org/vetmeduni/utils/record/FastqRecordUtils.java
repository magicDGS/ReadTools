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
package org.vetmeduni.utils.record;

import htsjdk.samtools.fastq.FastqRecord;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.utils.fastq.QualityUtils;

import static htsjdk.samtools.SAMUtils.phredToFastq;

/**
 * Utils for FASTQ records
 *
 * @author Daniel Gómez-Sánchez
 */
public class FastqRecordUtils {

	/**
	 * Cut a record and return it; if length equals 0 or start >= end, return null
	 *
	 * @param record the record to cut
	 * @param start  where to start the new record
	 * @param end    where to end the new record
	 *
	 * @return the record if it still have bases; <code>null</code> otherwise
	 */
	public static FastqRecord cutRecord(FastqRecord record, int start, int end) {
		if (start >= end) {
			return null;
		}
		String nucleotide = record.getReadString().substring(start, end);
		if (nucleotide.length() == 0) {
			return null;
		}
		String quality = record.getBaseQualityString().substring(start, end);
		return new FastqRecord(record.getReadHeader(), nucleotide, record.getBaseQualityHeader(), quality);
	}

	/**
	 * Return a new FastqRecord with a new quality encoding
	 *
	 * WARNING: the quality encoding is not checked
	 *
	 * @param record the record to update
	 *
	 * @return a new record with the sanger encoding
	 */
	public static FastqRecord copyToSanger(FastqRecord record) {
		// TODO: check if this is correct
		byte[] qualities = record.getBaseQualityString().getBytes();
		byte[] newQualities = new byte[qualities.length];
		for (int i = 0; i < qualities.length; i++) {
			newQualities[i] = QualityUtils.toSanger(qualities[i]);
		}
		// TODO: check if the phreadToFastq method is working properly
		return new FastqRecord(record.getReadHeader(), record.getReadString(), record.getBaseQualityHeader(),
			phredToFastq(newQualities));
	}

	/**
	 * Return a new FastqRecord with a new quality encoding
	 *
	 * WARNING: the quality encoding is not checked
	 *
	 * @param record the record to update
	 *
	 * @return a new record with the sanger encoding
	 */
	public static FastqPairedRecord copyToSanger(FastqPairedRecord record) {
		// transform the first record
		FastqRecord record1 = copyToSanger(record.getRecord1());
		FastqRecord record2 = copyToSanger(record.getRecord2());
		return new FastqPairedRecord(record1, record2);
	}
}
