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
import htsjdk.samtools.fastq.FastqRecord;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.methods.barcodes.BarcodeMethods;
import org.vetmeduni.utils.fastq.QualityUtils;

import java.util.Arrays;

import static htsjdk.samtools.SAMUtils.fastqToPhred;
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
		byte[] asciiQualities = fastqToPhred(record.getBaseQualityString());
		byte[] newQualities = new byte[asciiQualities.length];
		for (int i = 0; i < asciiQualities.length; i++) {
			newQualities[i] = QualityUtils.phredToSanger(asciiQualities[i]);
		}
		// TODO: check if the phreadToFastq method is working properly
		try {
			return new FastqRecord(record.getReadHeader(), record.getReadString(), record.getBaseQualityHeader(),
				phredToFastq(newQualities));
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("Cannot convert qualities for "+record.getReadHeader()+". Error: "+e.getMessage());
		}
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

	/**
	 * Get the barcode in the name from a FastqRecord
	 *
	 * @param record the record to extract the barcode from
	 *
	 * @return the barcode without read information; <code>null</code> if no barcode is found
	 */
	public static String getBarcodeInName(FastqRecord record) {
		return BarcodeMethods.getOnlyBarcodeFromName(record.getReadHeader());
	}

	/**
	 * Get the barcode in the name from a FastqPairedRecord. If only one is present or both match, return the first one;
	 * if they do not match, thrown an error
	 *
	 * @param record the record to extract the barcode from
	 *
	 * @return the barcode without read information; <code>null</code> if not barcode is found in either record
	 * @throws htsjdk.samtools.SAMException if both records have a barcode that do not match
	 */
	public static String getBarcodeInName(FastqPairedRecord record) throws SAMException {
		String barcode1 = getBarcodeInName(record.getRecord1());
		String barcode2 = getBarcodeInName(record.getRecord2());
		if (barcode1 == null) {
			return barcode2;
		}
		if (barcode1.equals(barcode2)) {
			return barcode1;
		}
		throw new SAMException("Barcodes from FastqPairedRecord do not match: " + barcode1 + "-" + barcode2);
	}

	/**
	 * Get the read name for a record without the record
	 *
	 * @param record the record to extract the name from
	 *
	 * @return the read name without the barcode information
	 */
	public static String getReadNameWithoutBarcode(FastqRecord record) {
		return BarcodeMethods.getNameWithoutBarcode(record.getReadHeader());
	}

	/**
	 * Get the read name for a record without the barcode
	 *
	 * @param record the record to extract the name from
	 *
	 * @return the read name without the barcode information
	 * @throws htsjdk.samtools.SAMException   if both record names do not match
	 * @throws java.lang.NullPointerException if one of the names is null
	 */
	public static String getReadNameWithoutBarcode(FastqPairedRecord record) {
		String name1 = getReadNameWithoutBarcode(record.getRecord1());
		String name2 = getReadNameWithoutBarcode(record.getRecord2());
		if (name1 == null || name2 == null) {
			throw new NullPointerException("Names from FastqPaired record could not be null");
		}
		if (name1.equals(name2)) {
			return name1;
		}
		throw new SAMException("Names from FastqPairedRecord do not match: " + name1 + "-" + name2);
	}

	/**
	 * Change the barcode name in a FASTQ record, adding the number of pair provided
	 *
	 * @param record       the record to update
	 * @param newBarcode   the new barcode to add
	 * @param numberOfPair the number of read for this record
	 *
	 * @return the updated record
	 */
	public static FastqRecord changeBarcode(FastqRecord record, String newBarcode, int numberOfPair) {
		return new FastqRecord(String
			.format("%s%s%s%s%s", getReadNameWithoutBarcode(record), BarcodeMethods.BARCODE_SEPARATOR, newBarcode,
				BarcodeMethods.READ_PAIR_SEPARATOR, numberOfPair), record.getReadString(),
			record.getBaseQualityHeader(), record.getBaseQualityString());
	}

	/**
	 * Change the barcode name in a FASTQ record, adding the \0 indicating that is a single read
	 *
	 * @param record     the record to update
	 * @param newBarcode the new barcode to add
	 *
	 * @return the updated record
	 */
	public static FastqRecord changeBarcodeInSingle(FastqRecord record, String newBarcode) {
		return changeBarcode(record, newBarcode, 0);
	}

	/**
	 * Change the barcode name in a pair record, adding the \1 and \2 indicating that they are paired
	 *
	 * @param record     the record to update
	 * @param newBarcode the new barcode to add
	 *
	 * @return the updated record
	 */
	public static FastqPairedRecord changeBarcodeInPaired(FastqPairedRecord record, String newBarcode) {
		return new FastqPairedRecord(changeBarcode(record.getRecord1(), newBarcode, 1),
			changeBarcode(record.getRecord2(), newBarcode, 2));
	}
}
