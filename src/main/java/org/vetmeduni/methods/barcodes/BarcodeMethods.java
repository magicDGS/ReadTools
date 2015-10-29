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
package org.vetmeduni.methods.barcodes;

import htsjdk.samtools.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * Class for testing barcodes against a dictionary
 *
 * TODO: synchronize important methods!
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeMethods {

	private static final Log logger = Log.getInstance(BarcodeMethods.class);


	// the unknown tag for sample and barcode
	public static final String UNKNOWN_STRING = "unkown";

	// the barcode dictionary
	private BarcodeDictionary dictionary;

	// maps barcode (combined) with sample name; if one barcode returned for some of the methods is not in this map, it is unknown
	private Hashtable<String, String> barcodeSample = null;

	/**
	 * Initialize the object with a dictionary
	 *
	 * @param dictionary the dictionary with the barcodes
	 */
	public BarcodeMethods(BarcodeDictionary dictionary) {
		this.dictionary = dictionary;
		initBarcodeMap();
	}

	/**
	 * Initialize the barcode map
	 */
	private void initBarcodeMap() {
		if (barcodeSample == null) {
			barcodeSample = new Hashtable<>();
			for (int i = 0; i < dictionary.getNumberOfBarcodes(); i++) {
				String sampleName = dictionary.getSampleNames().get(i);
				barcodeSample.put(dictionary.getCombinedBarcodesFor(sampleName), sampleName);
			}
		}
	}

	/**
	 * Get the sample name associated with a barcode
	 *
	 * @param barcode the barcode
	 *
	 * @return the sample name or {@link #UNKNOWN_STRING} if the barcode does not exists
	 */
	public String getSampleFromBarcode(String barcode) {
		return (barcodeSample.contains(barcode)) ? barcodeSample.get(barcode) : UNKNOWN_STRING;
	}

	/**
	 * Get the barcode dictionary associated with this object
	 *
	 * @return the barcode dictionary
	 */
	public BarcodeDictionary getDictionary() {
		return dictionary;
	}

	/**
	 * Get the barcode that better match with the dictionary of barcodes. The name of the sample could be retrieved with
	 * {@link #getSampleFromBarcode(String)}
	 *
	 * @param mismatches the number of mismatches allowed
	 * @param barcode    the array of barcodes to match
	 *
	 * @return the best real barcode in the dictionary (pasted in order if there are more than one)
	 * @throws IllegalArgumentException if the length of the array does not match the number of barcodes in the
	 *                                  dictionary
	 */
	public String getBestBarcode(int mismatches, String... barcode) {
		if (barcode.length != dictionary.getNumberOfBarcodes()) {
			throw new IllegalArgumentException(
				"Asking for matching a number of barcodes that does not fit with the ones contained in the barcode dictionary");
		}
		if (barcode.length == 1) {
			// returns the unique one
			String best = getBestBarcode(mismatches, barcode[0], 0);
			if(!best.equals(UNKNOWN_STRING)) {
				int index = dictionary.getBarcodesFromIndex(0).indexOf(best);
				dictionary.addOneTo(index);
			}
			return best;
		}
		// more than one barcode
		// map sample indexes and number of times that it occurs
		HashMap<Integer, Integer> samples = new HashMap<Integer, Integer>();
		// we need check in order
		for (int i = 0; i < dictionary.getNumberOfBarcodes(); i++) {
			String current = getBestBarcode(mismatches, barcode[i], i);
			// if it is not unknown
			if (!current.equals(UNKNOWN_STRING)) {
				// barcodes for this index
				ArrayList<String> allBarcodes = dictionary.getBarcodesFromIndex(i);
				// we need the index of the sample
				int sampleIndex = allBarcodes.indexOf(current);
				// check if it is unique for this set
				if (dictionary.isBarcodeUniqueInAt(current, i)) {
					dictionary.addOneTo(sampleIndex);
					// return directly the barcode
					return dictionary.getCombinedBarcodesFor(sampleIndex);
				} else {
					for (; sampleIndex < allBarcodes.size(); sampleIndex++) {
						if (allBarcodes.get(sampleIndex).equals(current)) {
							Integer count = samples.get(sampleIndex);
							samples.put(sampleIndex, (count == null) ? 1 : count + 1);
						}
					}
				}
			}
		}
		if(samples.size() == 0) {
			return UNKNOWN_STRING;
		}
		// if we reach this point, there are non unique barcode that identifies the sample
		// obtain the maximum count
		int maxCount = Collections.max(samples.values());
		// if there are more than one sample that could be associated with the barcode
		if (Collections.frequency(samples.values(), maxCount) != 1) {
			// it is not determined
			return UNKNOWN_STRING;
		} else {
			for (Integer sampleIndex : samples.keySet()) {
				if (samples.get(sampleIndex) == maxCount) {
					dictionary.addOneTo(sampleIndex);
					return dictionary.getCombinedBarcodesFor(sampleIndex);
				}
			}
		}
		// in principle, this cannot be reached
		throw new IllegalStateException("Unreachable code");
	}

	/**
	 * Get the barcode that better match with the dictionary of barcodes. The name of the sample could be retrieved with
	 * {@link #getSampleFromBarcode(String)}
	 *
	 * @param mismatches the number of mismatches allowed (if the length is 1, call {@link #getBestBarcode(int, String, int)})
	 * @param barcode    the array of barcodes to match
	 *
	 * @return the best real barcode in the dictionary (pasted in order if there are more than one)
	 * @throws IllegalArgumentException if the length of the arrays does not match the number of barcodes in the
	 *                                  dictionary
	 */
	public String getBestBarcode(int[] mismatches, String[] barcode) {
		if (barcode.length != dictionary.getNumberOfBarcodes()) {
			throw new IllegalArgumentException(
				"Asking for matching a number of barcodes that does not fit with the ones contained in the barcode dictionary");
		}
		if(mismatches.length == 1) {
			return getBestBarcode(mismatches[0], barcode);
		}
		if (barcode.length != mismatches.length) {
			throw new IllegalArgumentException("Mismatch thresholds and barcodes should have the same length");
		}
		// more than one barcode
		// map sample indexes and number of times that it occurs
		HashMap<Integer, Integer> samples = new HashMap<Integer, Integer>();
		// we need check in order
		for (int i = 0; i < dictionary.getNumberOfBarcodes(); i++) {
			String current = getBestBarcode(mismatches[i], barcode[i], i);
			// if it is not unknown
			if (!current.equals(UNKNOWN_STRING)) {
				// barcodes for this index
				ArrayList<String> allBarcodes = dictionary.getBarcodesFromIndex(i);
				// we need the index of the sample
				int sampleIndex = allBarcodes.indexOf(current);
				// check if it is unique for this set
				if (dictionary.isBarcodeUniqueInAt(current, i)) {
					// return directly the barcode
					return dictionary.getCombinedBarcodesFor(sampleIndex);
				} else {
					for (; sampleIndex < allBarcodes.size(); sampleIndex++) {
						if (allBarcodes.get(sampleIndex).equals(current)) {
							Integer count = samples.get(sampleIndex);
							samples.put(sampleIndex, (count == null) ? 1 : count + 1);
						}
					}
				}
			}
		}
		// if we reach this point, there are non unique barcode that identifies the sample
		// obtain the maximum count
		int maxCount = Collections.max(samples.values());
		// if there are more than one sample that could be associated with the barcode
		if (Collections.frequency(samples.values(), maxCount) != 1) {
			// it is not determined
			return UNKNOWN_STRING;
		} else {
			for (Integer sampleIndex : samples.keySet()) {
				if (samples.get(sampleIndex) == maxCount) {
					return dictionary.getCombinedBarcodesFor(sampleIndex);
				}
			}
		}
		// in principle, this cannot be reached
		throw new IllegalStateException("Unreachable code");
	}

	/**
	 * Get the best barcode for a concrete index
	 *
	 * @param mismatches the number of mismatches allowed
	 * @param barcode    he barcode to test
	 * @param index      the index of the barcode
	 *
	 * @return the best barcode from the set of barcodes
	 */
	private String getBestBarcode(int mismatches, String barcode, int index) {
		String best = UNKNOWN_STRING;
		int minMismatch = barcode.length();
		for (String b : dictionary.getSetBarcodesFromIndex(index)) {
			int countMistmatch;
			if (barcode.length() > b.length()) {
				// cut the barcode if it is longer
				countMistmatch = mismatchesCount(barcode.substring(0, b.length()), b);
			} else {
				countMistmatch = mismatchesCount(barcode, b);
			}
			// if there are no mistmaches, this is the best
			if (countMistmatch == 0) {
				return b;
			}
			// if the count of mismatches is better than the previous, met the threshold and is lower than the actual length of the tested barcode
			if (countMistmatch < minMismatch && countMistmatch <= mismatches && countMistmatch < b.length()) {
				// update the barcode
				best = b;
				minMismatch = countMistmatch;
			}
		}
		return best;
	}

	/**
	 * Count the number of mismatches betwen a test barcode and a target barcode
	 *
	 * @param testBarcode   the test barcode
	 * @param targetBarcode the target barcode
	 *
	 * @return the number of mistmatches between barcodes
	 */
	public int mismatchesCount(String testBarcode, String targetBarcode) {
		// logger.debug("Testing ", testBarcode, " against ", targetBarcode);
		// if(testBarcode.length() != barcode.length()) return testBarcode.length();
		int mmCnt = 0;
		for (int i = 0; i < testBarcode.length(); i++) {
			if (testBarcode.charAt(i) != targetBarcode.charAt(i)) {
				mmCnt++;
			}
		}
		return mmCnt;
	}
}
