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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static methods for barcode matching
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeMethods {

	/**
	 * The separator between the read name and the barcode
	 */
	public static final String NAME_BARCODE_SEPARATOR = "#";

	/**
	 * The separator between the read name (and barcode, if present) and the read pair information (0, 1, 2)
	 */
	public static final String READ_PAIR_SEPARATOR = "/";

	/**
	 * The separator between several barcodes
	 */
	public static final String BARCODE_BARCODE_SEPARATOR = "_";

	/**
	 * The pattern to match a barcode in a read name including everything after the {@link #NAME_BARCODE_SEPARATOR}
	 */
	public static final Pattern BARCODE_COMPLETE_PATTERN = Pattern.compile(NAME_BARCODE_SEPARATOR + "(.+)");

	/**
	 * The pattern to match a barcode in a read name removing also the read pair info (after {@link
	 * #READ_PAIR_SEPARATOR})
	 */
	public static final Pattern BARCODE_WITH_READPAIR_PATTERN = Pattern
		.compile(NAME_BARCODE_SEPARATOR + "(.+)" + READ_PAIR_SEPARATOR);

	/**
	 * Get the barcode from a read name
	 *
	 * @param readName the readName to extract the name from
	 *
	 * @return the barcode without pair information if found; <code>null</code> otherwise
	 */
	public static String getOnlyBarcodeFromName(String readName) {
		Matcher matcher;
		if (readName.contains(READ_PAIR_SEPARATOR)) {
			matcher = BARCODE_WITH_READPAIR_PATTERN.matcher(readName);
		} else {
			matcher = BARCODE_COMPLETE_PATTERN.matcher(readName);
		}
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	/**
	 * Get several barcodes encoding after {@link #NAME_BARCODE_SEPARATOR}
	 *
	 * @param readName         the readName to extract the name from
	 * @param barcodeSeparator the separator between the barcodes
	 *
	 * @return the array with the barcodes
	 */
	public static String[] getSeveralBarcodesFromName(String readName, String barcodeSeparator) {
		String combined = getOnlyBarcodeFromName(readName);
		return combined.split(barcodeSeparator);
	}

	/**
	 * Get several barcodes, encoding after {@link #NAME_BARCODE_SEPARATOR}, and separated between each other with
	 * {@link #BARCODE_BARCODE_SEPARATOR}
	 *
	 * @param readName the readName to extract the name from
	 *
	 * @return the array with the barcodes
	 */
	public static String[] getSeveralBarcodesFromName(String readName) {
		return getSeveralBarcodesFromName(readName, BARCODE_BARCODE_SEPARATOR);
	}

	/**
	 * Get the readName removing everything from the '#' to the end
	 *
	 * @param readName the readName to extract the name from
	 *
	 * @return the readName without the barcode information (if present)
	 */
	public static String getNameWithoutBarcode(String readName) {
		int index = readName.indexOf(NAME_BARCODE_SEPARATOR);
		if (index != -1) {
			return readName.substring(0, index);
		}
		return readName;
	}
}
