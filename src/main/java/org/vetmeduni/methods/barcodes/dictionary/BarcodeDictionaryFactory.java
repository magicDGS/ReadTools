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
import org.vetmeduni.io.readers.SpaceDelimitedReader;
import org.vetmeduni.methods.barcodes.dictionary.decoder.BarcodeMatch;
import org.vetmeduni.readtools.ProjectProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class to create different barcode dictionaries dependending on the application
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeDictionaryFactory {

	/**
	 * The read group information for default tags
	 */
	public static final SAMReadGroupRecord UNKNOWN_READGROUP_INFO;

	// initialize the unknown read group information
	static {
		UNKNOWN_READGROUP_INFO = new SAMReadGroupRecord(BarcodeMatch.UNKNOWN_STRING);
		UNKNOWN_READGROUP_INFO.setProgramGroup(ProjectProperties.getName());
		UNKNOWN_READGROUP_INFO.setSample(BarcodeMatch.UNKNOWN_STRING);
	}

	/**
	 * Create a barcode dictionary from a tab-delimited file, with the first column being the barcode and the subsequent
	 * the barcodes. Each of the barcodes is stored independently
	 *
	 * @param run              the run name; <code>null</code> is allowed
	 * @param barcodeFile      the file
	 * @param readGroupInfo    read group record where tags for other barcodes will be used (except ID and SN)
	 * @param numberOfBarcodes the expected number of barcodes; if < 0, it is computed for the first line in the file
	 *
	 * @return the barcode dictionary
	 * @throws java.io.IOException if the file have some problem
	 */
	public static BarcodeDictionary createDefaultDictionary(String run, File barcodeFile,
		final SAMReadGroupRecord readGroupInfo, int numberOfBarcodes) throws IOException {
		SpaceDelimitedReader reader = new SpaceDelimitedReader(barcodeFile);
		// read the first line
		String[] nextLine = reader.next();
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
		ArrayList<ArrayList<String>> barcodes = new ArrayList<>(numberOfBarcodes);
		// initialize all the barcodes
		for (int i = 0; i < numberOfBarcodes; i++) {
			barcodes.add(new ArrayList<>());
		}
		ArrayList<String> samples = new ArrayList<>();
		// reading the rest of the lines
		while (nextLine != null) {
			if (numberOfBarcodes != nextLine.length - 1) {
				throw new IOException(
					"Each sample should have the same number of barcodes. Check the formatting of " + barcodeFile
						.getAbsolutePath());
			}
			// the first item is the sample name
			samples.add(nextLine[0]);
			// get the barcodes
			for (int i = 1; i <= numberOfBarcodes; i++) {
				barcodes.get(i - 1).add(nextLine[i]);
			}
			nextLine = reader.next();
		}
		reader.close();
		// construct the barcode dictionary
		// TODO: add library info
		return new BarcodeDictionary(run, samples, barcodes, null, readGroupInfo);
	}

	/**
	 * Create a barcode dictionary from a tab-delimited file, with the first column being the barcode and the subsequent
	 * the barcodes. Each of the barcodes is stored independently
	 *
	 * @param barcodeFile      the file
	 * @param numberOfBarcodes the expected number of barcodes; if < 0, it is computed for the first line in the file
	 *
	 * @return the barcode dictionary
	 * @throws java.io.IOException if the file have some problem
	 */
	public static BarcodeDictionary createDefaultDictionary(File barcodeFile, int numberOfBarcodes) throws IOException {
		return createDefaultDictionary(null, barcodeFile, UNKNOWN_READGROUP_INFO, numberOfBarcodes);
	}

	/**
	 * Creates a barcode dictionary from a tab-delimited file, with the first column being the barcode and the
	 * subsequent the barcodes. All the barcodes are stored together as an "unique" barcode.
	 *
	 * @param run           the run name; <code>null</code> is allowed
	 * @param barcodeFile   the file
	 * @param readGroupInfo read group record where tags for other barcodes will be used (except ID and SN)
	 *
	 * @return the barcode dictionary
	 * @throws IOException
	 */
	public static BarcodeDictionary createCombinedDictionary(String run, File barcodeFile,
		final SAMReadGroupRecord readGroupInfo) throws IOException {
		SpaceDelimitedReader reader = new SpaceDelimitedReader(barcodeFile);
		// read the first line
		String[] nextLine = reader.next();
		if (nextLine == null) {
			throw new IOException("No data in the barcode file");
		}
		// compute the number of barcodes in the file
		int numberOfBarcodes = nextLine.length - 1;
		// the number of barcodes in the dictionary will be one
		ArrayList<ArrayList<String>> barcodes = new ArrayList<>(1);
		barcodes.add(new ArrayList<>());
		// create the sample array
		ArrayList<String> samples = new ArrayList<>();
		// reading the rest of the lines
		while (nextLine != null) {
			if (numberOfBarcodes != nextLine.length - 1) {
				throw new IOException(
					"Each sample should have the same number of barcodes. Check the formatting of " + barcodeFile
						.getAbsolutePath());
			}
			// the first item is the sample name
			samples.add(nextLine[0]);
			// get the unique barcode
			String uniqueBarcode = String.join("", Arrays.copyOfRange(nextLine, 1, nextLine.length));
			barcodes.get(0).add(uniqueBarcode);
			nextLine = reader.next();
		}
		reader.close();
		// construct the barcode dictionary
		// TODO: add library info
		return new BarcodeDictionary(run, samples, barcodes, null, readGroupInfo);
	}

	/**
	 * Creates a barcode dictionary from a tab-delimited file, with the first column being the barcode and the
	 * subsequent the barcodes. All the barcodes are stored together as an "unique" barcode.
	 *
	 * @param barcodeFile the file
	 *
	 * @return the barcode dictionary
	 * @throws IOException
	 */
	public static BarcodeDictionary createCombinedDictionary(File barcodeFile) throws IOException {
		return createCombinedDictionary(null, barcodeFile, UNKNOWN_READGROUP_INFO);
	}
}
