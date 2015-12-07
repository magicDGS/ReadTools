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
package org.vetmeduni.methods.barcodes.dictionary.decoder;

/**
 * Class that contain a match for the barcode
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeMatch {

	/**
	 * The unknown tag for sample and barcode
	 */
	public static final String UNKNOWN_STRING = "UNKNOWN";

	/**
	 * The best barcode sequence or {@link #UNKNOWN_STRING} if there are no
	 */
	protected String barcode;

	/**
	 * The number of mismatches for the match
	 */
	protected int mismatches;

	/**
	 * The number of mismatches for the second best
	 */
	protected int mismatchesToSecondBest;

	/**
	 * The number of Ns in the barcode
	 */
	protected int numberOfNs;

	/**
	 * Creates a unknow match with the number of mismatches being equal to the maximum number of mismatches
	 *
	 * @param maxMismatches the maximum number of mismatches (barcode lenght)
	 */
	public BarcodeMatch(int maxMismatches) {
		barcode = UNKNOWN_STRING;
		mismatches = maxMismatches;
		mismatchesToSecondBest = maxMismatches;
		numberOfNs = maxMismatches;
	}

	/**
	 * Check if the barcode is matched
	 *
	 * @return <code>true</code> if it is match; <code>false</code> otherwise
	 */
	public boolean isMatch() {
		return !barcode.equals(UNKNOWN_STRING);
	}

	/**
	 * Get the difference in absolute value between the number of mismatches for the best and the second best match
	 *
	 * @return the differences in absolute value
	 */
	public int getDifferenceWithSecond() {
		return mismatchesToSecondBest - mismatches;
	}
}
