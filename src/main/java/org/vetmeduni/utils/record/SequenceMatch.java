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

import htsjdk.samtools.util.SequenceUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static methods for match DNA sequences
 *
 * @author Daniel Gómez-Sánchez
 */
public class SequenceMatch {

	/**
	 * Pattern for sequence starting by Ns
	 */
	public static final Pattern startN = Pattern.compile("^N+", Pattern.CASE_INSENSITIVE);

	/**
	 * Pattern for sequences ending with Ns
	 */
	public static final Pattern endN = Pattern.compile("N+$", Pattern.CASE_INSENSITIVE);

	/**
	 * Pattern for sequences containing Ns in any place
	 */
	public static final Pattern Ns = Pattern.compile("N", Pattern.CASE_INSENSITIVE);

	/**
	 * Check if a sequence starts with Ns
	 *
	 * @param sequence the sequence
	 *
	 * @return <code>true</code> if it starts with Ns; <code>false</code> otherwise
	 */
	public static boolean sequenceStartByN(String sequence) {
		Matcher matchStart = startN.matcher(sequence);
		return matchStart.find();
	}

	/**
	 * Check if a sequence ends with Ns
	 *
	 * @param sequence the sequence
	 *
	 * @return <code>true</code> if it ends with Ns; <code>false</code> otherwise
	 */
	public static boolean sequenceEndByNs(String sequence) {
		Matcher matchEnd = endN.matcher(sequence);
		return matchEnd.find();
	}

	/**
	 * Check if a sequence contains Ns
	 *
	 * @param sequence the sequence
	 *
	 * @return <code>true</code> if it contains with Ns; <code>false</code> otherwise
	 */
	public static boolean sequenceContainNs(String sequence) {
		Matcher ns = Ns.matcher(sequence);
		return ns.find();
	}

	/**
	 * Count the number of mismatches between a test barcode and a target barcode counting Ns as mismatch
	 *
	 * @param testBarcode   the test barcode
	 * @param targetBarcode the target barcode
	 *
	 * @return the number of mistmatches between barcodes
	 */
	public static int mismatchesCount(String testBarcode, String targetBarcode) {
		return mismatchesCount(testBarcode, targetBarcode, true);
	}

	/**
	 * Count the number of mismatches between a test barcode and a target barcode
	 *
	 * @param testBarcode   the test barcode
	 * @param targetBarcode the target barcode
	 * @param nAsMismatch   if <code>true</code> N and other bases are counted as mismatch; if <code>false</code> it is
	 *                      ignored
	 *
	 * @return the number of mismatches between barcodes
	 */
	public static int mismatchesCount(String testBarcode, String targetBarcode, boolean nAsMismatch) {
		// logger.debug("Testing ", testBarcode, " against ", targetBarcode);
		// if(testBarcode.length() != barcode.length()) return testBarcode.length();
		int mmCnt = 0;
		for (int i = 0; i < testBarcode.length(); i++) {
			final byte testBase = (byte) testBarcode.charAt(i);
			final byte targetBase = (byte) targetBarcode.charAt(i);
			// only count if n is a mismatch of none of the test/target bases is an N
			final boolean shouldCount =
				nAsMismatch || !(SequenceUtil.isNoCall(testBase) || SequenceUtil.isNoCall(targetBase));
			// case-insensitive mismatches count for N or not N
			if (shouldCount && !SequenceUtil.basesEqual(testBase, targetBase)) {
				mmCnt++;
			}
		}
		return mmCnt;
	}
}
