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

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.SequenceUtil;
import htsjdk.samtools.util.StringUtil;

/**
 * @author Daniel Gómez-Sánchez
 */
public class SAMRecordUtils {



	/**
	 * Convert a SAMRecord to a FastqRecord (reverse complement if this flag is set)
	 *
	 * @param record
	 * @param mateNumber
	 *
	 * @return
	 */
	public static FastqRecord toFastqRecord(SAMRecord record, Integer mateNumber) {
		String seqName = (mateNumber == null) ?
			record.getReadName() :
			String.format("%s/%d", record.getReadName(), mateNumber);
		String readString = record.getReadString();
		String qualityString = record.getBaseQualityString();
		if (record.getReadNegativeStrandFlag()) {
			readString = SequenceUtil.reverseComplement(readString);
			qualityString = StringUtil.reverseString(qualityString);
		}
		return new FastqRecord(seqName, readString, "", qualityString);
	}

	/**
	 * Assert that both pairs are mates
	 *
	 * @param record1
	 * @param record2
	 */
	public static void assertPairedMates(final SAMRecord record1, final SAMRecord record2) {
		if (!(record1.getFirstOfPairFlag() && record2.getSecondOfPairFlag() || record2.getFirstOfPairFlag() && record1
			.getSecondOfPairFlag())) {
			throw new SAMException("Illegal mate state: " + record1.getReadName());
		}
	}

	/**
	 * Add a barcode to a SAMRecord in the format recordName#barcode
	 *
	 * @param record the record to update
	 * @param barcode the barcode
	 */
	public static void addBarcodeToName(SAMRecord record, String barcode) {
		String recordName = String.format("%s#%s", record.getReadName(), barcode);
		record.setReadName(recordName);
	}

}
