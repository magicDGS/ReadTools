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

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Classs for store a barcode dictionary
 *
 * TODO: multi-thread
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeDictionary {

	// name of the samples
	private ArrayList<String> samples;

	// number of things for each sample
	private int[] samplesValue;

	// array with one array per barcode, and the size of each array internally is a sample
	private ArrayList<ArrayList<String>> barcodes;

	// backup of barcode sets
	private ArrayList<HashSet<String>> barcodesSets = null;

	/**
	 * Constructor for debugging
	 *
	 * @param samples  the sample names
	 * @param barcodes the barcodes
	 */
	protected BarcodeDictionary(ArrayList<String> samples, ArrayList<ArrayList<String>> barcodes) {
		this.samples = samples;
		this.barcodes = barcodes;
	}

	/**
	 * Initialize a barcode file, that is tab-delimited
	 *
	 * @param barcodeFile      the file
	 * @param numberOfBarcodes the expected number of barcodes; if < 0, it is computed for the first line in the file
	 *
	 * @throws java.io.IOException if the file have some problem
	 */
	public BarcodeDictionary(File barcodeFile, int numberOfBarcodes) throws IOException {
		CSVReader reader = new CSVReader(new FileReader(barcodeFile), '\t');
		// read the first line
		String[] nextLine = reader.readNext();
		if (nextLine == null) {
			throw new IOException("No data in the barcode file");
		}
		// check the number of barcodes
		if (numberOfBarcodes < 1) {
			numberOfBarcodes = nextLine.length - 1;
		}
		if (numberOfBarcodes < 1) {
			throw new IOException("Barcode file wrongly formatted");
		}
		// at this point, we know the number of barcodes
		initBarcodes(numberOfBarcodes);
		this.samples = new ArrayList<>();
		// reading the rest of the lines
		while (nextLine != null) {
			if (numberOfBarcodes != nextLine.length - 1) {
				throw new IOException("Each sample should have the same number of barcodes in the file");
			}
			// the first item is the sample name
			samples.add(nextLine[0]);
			// get the barcodes
			for (int i = 1; i <= numberOfBarcodes; i++) {
				this.barcodes.get(i - 1).add(nextLine[i]);
			}
			nextLine = reader.readNext();
		}
		reader.close();
		samplesValue = new int[samples.size()];
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
	 * Get the sample names
	 *
	 * @return the sample names
	 */
	public List<String> getSampleNames() {
		return samples;
	}

	/**
	 * Get the number of samples in this dictionary
	 *
	 * @return the number of samples
	 */
	public int numberOfSamples() {
		return samples.size();
	}

	/**
	 * Get the barcodes associated with certain sample
	 *
	 * @param sample the sample name
	 *
	 * @return the barcodes for the sample
	 */
	public String[] getBarcodesFor(String sample) {
		int index = samples.indexOf(sample);
		return getBarcodesFor(index);
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

	public void addOneTo(String sample) {
		int index = samples.indexOf(sample);
		addOneTo(index);
	}

	public void addOneTo(int sampleIndex) {
		samplesValue[sampleIndex]++;
	}

	public int getValueFor(String sample) {
		int index = samples.indexOf(sample);
		return getValueFor(index);
	}

	public int getValueFor(int sampleIndex) {
		return samplesValue[sampleIndex];
	}

	/**
	 * Get the barcodes associated with certain sample pasted together
	 *
	 * @param sample the sample name
	 *
	 * @return the combined barcodes for the sample
	 */
	public String getCombinedBarcodesFor(String sample) {
		return String.join("", getBarcodesFor(sample));
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
	protected ArrayList<String> getBarcodesFromIndex(int index) {
		return barcodes.get(index);
	}

	/**
	 * Get the first, second... barcodes (0-indexed) but reducing the complexity
	 *
	 * @param index the index
	 *
	 * @return a set representation of the index barcodes
	 */
	protected Set<String> getSetBarcodesFromIndex(int index) {
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
