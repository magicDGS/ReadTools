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
import org.vetmeduni.methods.barcodes.BarcodeMethods;

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
	 * Array which contains the barcodes. The lenght is the number of barcodes used, and the internal array contain the
	 * associated barcode for each sample
	 */
	private final ArrayList<ArrayList<String>> barcodes;

	/**
	 * Cached map between combined barcodes and read groups
	 */
	private final Hashtable<String, SAMReadGroupRecord> barcodeRGmap = new Hashtable<>();

	/**
	 * Cached barcode set(s) for fast access
	 */
	private final ArrayList<HashSet<String>> barcodesSets = new ArrayList<>();

	/**
	 * Protected constructor. For construct an instance, use {@link org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionaryFactory}
	 *
	 * @param samples  the sample names
	 * @param barcodes the barcodes
	 */
	protected BarcodeDictionary(ArrayList<SAMReadGroupRecord> samples, ArrayList<ArrayList<String>> barcodes) {
		this.sampleRecord = samples;
		this.barcodes = barcodes;
	}

	/**
	 * Protected constructor. For get an instance of a dictionary, use {@link org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionaryFactory}
	 *
	 * @param samples       the sample names
	 * @param barcodes      the barcodes
	 * @param readGroupInfo the additional information for the read group
	 *
	 * @deprecated use {@link #BarcodeDictionary(String, ArrayList, ArrayList, ArrayList, SAMReadGroupRecord)} instead
	 */
	@Deprecated
	protected BarcodeDictionary(ArrayList<String> samples, ArrayList<ArrayList<String>> barcodes,
		SAMReadGroupRecord readGroupInfo) {
		this(null, samples, barcodes, null, readGroupInfo);
	}

	/**
	 * Protected constructor. For get an instance of a dictionary, use {@link org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionaryFactory}
	 *
	 * @param run           the run name for the samples; if <code>null</code> it will be igonored
	 * @param samples       the sample names
	 * @param barcodes      the barcodes
	 * @param libraries     the library for each barcode; if <code>null</code>, the library is the
	 *                      samples_combinedBarcodes
	 * @param readGroupInfo the additional information for the read group
	 */
	protected BarcodeDictionary(String run, ArrayList<String> samples, ArrayList<ArrayList<String>> barcodes,
		ArrayList<String> libraries, SAMReadGroupRecord readGroupInfo) {
		this.barcodes = barcodes;
		this.sampleRecord = new ArrayList<>(samples.size());
		initReadGroups(run, samples, libraries, readGroupInfo);
	}

	/**
	 * Create the read group for the samples: the sample name will be in the SM tag, the sampleName_combinedBarcode the
	 * ID and the rest of tags will come from the read group. The barcode field should be initialized before calling
	 * this method
	 *
	 * @param run           the run name for the samples; if <code>null</code> it will be igonored
	 * @param libraries     the library for each barcode; if <code>null</code> or empty, the library is the
	 *                      samples_combinedBarcodes
	 * @param samples       the sample name
	 * @param readGroupInfo the read group information
	 */
	private void initReadGroups(String run, ArrayList<String> samples, ArrayList<String> libraries,
		SAMReadGroupRecord readGroupInfo) {
		for (int i = 0; i < samples.size(); i++) {
			final String sampleBarcode = String.format("%s_%s", samples.get(i), getCombinedBarcodesFor(i));
			final SAMReadGroupRecord rg = new SAMReadGroupRecord(
				(run == null) ? sampleBarcode : String.format("%s_%s", run, sampleBarcode), readGroupInfo);
			rg.setSample(samples.get(i));
			if (libraries != null && !libraries.isEmpty()) {
				rg.setLibrary(libraries.get(i));
			} else {
				rg.setLibrary(sampleBarcode);
			}
			sampleRecord.add(rg);
		}
	}

	/**
	 * Initialize the barcode-RG map for the dictionary to cached
	 */
	private void initBarcodeRGmap() {
		// init the barcode-rg map
		for (int i = 0; i < numberOfSamples(); i++) {
			barcodeRGmap.put(getCombinedBarcodesFor(i), getReadGroupFor(i));
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
	 * Get the name for a concrete sample
	 *
	 * @param sampleIndex the sample index
	 *
	 * @return the read group of the sample
	 */
	public SAMReadGroupRecord getReadGroupFor(int sampleIndex) {
		return sampleRecord.get(sampleIndex);
	}

	@Deprecated
	protected Hashtable<String, SAMReadGroupRecord> getRGmap() {
		if (barcodeRGmap.isEmpty()) {
			initBarcodeRGmap();
		}
		return barcodeRGmap;
	}

	/**
	 * Get the read group for a combined barcode
	 *
	 * @param combinedBarcode the combined barcode
	 *
	 * @return the read group associated with that barcode; {@link org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionaryFactory#UNKNOWN_READGROUP_INFO}
	 * if not found
	 */
	public SAMReadGroupRecord getReadGroupFor(String combinedBarcode) {
		if (barcodeRGmap.isEmpty()) {
			initBarcodeRGmap();
		}
		return (barcodeRGmap.containsKey(combinedBarcode)) ?
			barcodeRGmap.get(combinedBarcode) :
			BarcodeDictionaryFactory.UNKNOWN_READGROUP_INFO;
	}

	/**
	 * Get the barcodes associated with certain sample pasted together
	 *
	 * @param sampleIndex the sample index
	 *
	 * @return the combined barcodes for the sample
	 */
	public String getCombinedBarcodesFor(int sampleIndex) {
		// TODO: changed the combination method!!!!
		// TODO: be sure that it is working!!!
		return BarcodeMethods.joinBarcodes(getBarcodesFor(sampleIndex));
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
		if (barcodesSets.isEmpty()) {
			initSets();
		}
		return barcodesSets.get(index);
	}

	/**
	 * Initialize the sets for the barcodes
	 */
	private void initSets() {
		for (ArrayList<String> l : barcodes) {
			barcodesSets.add(new HashSet<>(l));
		}
	}

	/**
	 * String representation of the dictionary, that is the mapping between the combined barcode (result of {@link
	 * #getCombinedBarcodesFor(int)}) and the samples as {@link htsjdk.samtools.SAMReadGroupRecord}
	 *
	 * @return the short representation
	 */
	public String toString() {
		if (barcodeRGmap.isEmpty()) {
			initBarcodeRGmap();
		}
		return barcodeRGmap.toString();
	}
}
