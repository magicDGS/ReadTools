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
package org.vetmeduni.methods.barcodes.dictionary;

import htsjdk.samtools.SAMReadGroupRecord;

import java.util.*;

/**
 * Classs for store a barcode dictionary
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeDictionary {

	/**
	 * New name for the samples
	 */
	private final ArrayList<SAMReadGroupRecord> sampleRecord;

	/**
	 * Value associated for each sample
	 */
	@Deprecated
	private final int[] samplesValue;

	/**
	 * Array which contains the barcodes. The lenght is the number of barcodes used, and the internal array contain the
	 * associated barcode for each sample
	 */
	private ArrayList<ArrayList<String>> barcodes;

	/**
	 * Cached barcode set(s) for fast access
	 */
	private ArrayList<HashSet<String>> barcodesSets = null;

	/**
	 * Protected constructor. For construct an instance, use {@link org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionaryFactory}
	 *
	 * @param samples  the sample names
	 * @param barcodes the barcodes
	 */
	protected BarcodeDictionary(ArrayList<SAMReadGroupRecord> samples, ArrayList<ArrayList<String>> barcodes) {
		this.sampleRecord = samples;
		this.samplesValue = new int[sampleRecord.size()];
		this.barcodes = barcodes;
	}

	/**
	 * Initialize the barcodes with this number of barcodes
	 *
	 * @param numberOfBarcodes the number of barcodes
	 */
	private void initBarcodes(int numberOfBarcodes) {
		this.barcodes = new ArrayList<>(numberOfBarcodes);
		for (int i = 0; i < numberOfBarcodes; i++) {
			barcodes.add(new ArrayList<>());
		}
	}

	/**
	 * Get the number of barcodes in this dictionary
	 *
	 * @return the number of barcodes
	 */
	public int getNumberOfBarcodes() {
		return barcodes.size();
	}

	/**
	 * Get the sample names in order
	 *
	 * @return the sample names
	 */
	public List<String> getSampleNames() {
		ArrayList<String> toReturn = new ArrayList<>();
		for (SAMReadGroupRecord s : sampleRecord) {
			toReturn.add(s.getSample());
		}
		return toReturn;
	}

	/**
	 * Get the sample names in order
	 *
	 * @return the sample names
	 */
	public List<SAMReadGroupRecord> getSampleReadGroups() {
		return sampleRecord;
	}

	/**
	 * Get the number of samples in this dictionary associated with a different barcode
	 *
	 * @return the number of samples
	 */
	public int numberOfSamples() {
		return sampleRecord.size();
	}

	/**
	 * Get the number of unique samples in this dictionary
	 *
	 * @return the effective number of samples
	 */
	public int numberOfUniqueSamples() {
		// will it be better to store this value??
		return new HashSet<>(sampleRecord).size();
	}

	/**
	 * Get the barcodes associated with certain sample
	 *
	 * @param sampleIndex the sample index
	 *
	 * @return the barcodes for the sample
	 */
	public String[] getBarcodesFor(int sampleIndex) {
		String[] toReturn = new String[getNumberOfBarcodes()];
		for (int i = 0; i < barcodes.size(); i++) {
			toReturn[i] = barcodes.get(i).get(sampleIndex);
		}
		return toReturn;
	}

	/**
	 * Add value to a concrete sample (by index)
	 *
	 * @param sampleIndex the sample index
	 */
	@Deprecated
	public void addOneTo(int sampleIndex) {
		samplesValue[sampleIndex]++;
	}

	/**
	 * Get the value for a concrete sample (by index)
	 *
	 * @param sampleIndex the sample index
	 *
	 * @return the value for the sample
	 */
	@Deprecated
	public int getValueFor(int sampleIndex) {
		return samplesValue[sampleIndex];
	}

	/**
	 * Get the name for a concrete sample
	 *
	 * @param sampleIndex the sample index
	 *
	 * @return the read group of the sample
	 */
	public SAMReadGroupRecord getReadGroupFor(int sampleIndex) {
		return sampleRecord.get(sampleIndex);
	}

	/**
	 * Get the name for a concrete sample
	 *
	 * @param sampleIndex the sample index
	 *
	 * @return the name of the sample
	 */
	public String getNameFor(int sampleIndex) {
		return getReadGroupFor(sampleIndex).getSample();
	}

	/**
	 * Get the barcodes associated with certain sample pasted together
	 *
	 * @param sampleIndex the sample index
	 *
	 * @return the combined barcodes for the sample
	 */
	public String getCombinedBarcodesFor(int sampleIndex) {
		return String.join("", getBarcodesFor(sampleIndex));
	}

	/**
	 * Check if the provided barcode is unique for that index
	 *
	 * @param barcode the barcode to test
	 * @param index   0-based index
	 *
	 * @return <code>true</code> if the barcode is unique; <code>false</code> otherwise
	 */
	public boolean isBarcodeUniqueInAt(String barcode, int index) {
		return Collections.frequency(barcodes.get(index), barcode) == 1;
	}

	/**
	 * Get the first, second... barcodes (0-indexed)
	 *
	 * @param index the index
	 *
	 * @return the list with the barcodes associated with each sample
	 */
	public ArrayList<String> getBarcodesFromIndex(int index) {
		return barcodes.get(index);
	}

	/**
	 * Get the first, second... barcodes (0-indexed) but reducing the complexity
	 *
	 * @param index the index
	 *
	 * @return a set representation of the index barcodes
	 */
	public Set<String> getSetBarcodesFromIndex(int index) {
		if (barcodesSets == null) {
			initSets();
		}
		return barcodesSets.get(index);
	}

	/**
	 * Initialize the sets for the barcodes
	 */
	private void initSets() {
		barcodesSets = new ArrayList<>();
		for (ArrayList<String> l : barcodes) {
			barcodesSets.add(new HashSet<>(l));
		}
	}
}
