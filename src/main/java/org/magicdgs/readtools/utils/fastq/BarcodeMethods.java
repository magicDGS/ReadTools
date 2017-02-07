/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
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
 * SOFTWARE.
 */

package org.magicdgs.readtools.utils.fastq;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static methods for barcode matching.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @deprecated this will be replaced by the utility methods for GATKRead in
 * {@link org.magicdgs.readtools.utils.read.RTReadUtils}
 */
@Deprecated
public class BarcodeMethods {

    /**
     * The separator between the read name (and barcode, if present) and the read pair information
     * (0, 1, 2).
     */
    public static final String READ_PAIR_SEPARATOR = "/";

    /**
     * The separator between several barcodes.
     *
     * @deprecated use {@link org.magicdgs.readtools.RTDefaults#BARCODE_INDEX_DELIMITER} instead.
     * Maintained here til legacy tools are removed.
     */
    @Deprecated
    public static final String BARCODE_BARCODE_SEPARATOR = "_";

    /**
     * The pattern to match a barcode in a read name including everything after the {@link
     * RTFastqContstants#ILLUMINA_NAME_BARCODE_DELIMITER}.
     */
    public static final Pattern BARCODE_COMPLETE_PATTERN =
            Pattern.compile(RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER + "(.+)");

    /**
     * The pattern to match a barcode in a read name removing also the read pair info (after {@link
     * #READ_PAIR_SEPARATOR})
     */
    public static final Pattern BARCODE_WITH_READPAIR_PATTERN = Pattern
            .compile(RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER + "(.+)"
                    + READ_PAIR_SEPARATOR);

    /**
     * Get the barcode from a read name.
     *
     * @param readName the readName to extract the name from.
     *
     * @return the barcode without pair information if found; {@code null} otherwise.
     */
    public static String getOnlyBarcodeFromName(final String readName) {
        final Matcher matcher;
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
     * Join several barcodes together using teh barcode separator.
     *
     * @param barcodes the barcodes.
     *
     * @return the formatted barcode.
     */
    public static String joinBarcodes(final String[] barcodes, final String barcodeSeparator) {
        return String.join(barcodeSeparator, barcodes);
    }

    /**
     * Join several barcodes together usin {@link #BARCODE_BARCODE_SEPARATOR}.
     *
     * @param barcodes the barcodes.
     *
     * @return the formatted barcode.
     */
    public static String joinBarcodes(final String[] barcodes) {
        return joinBarcodes(barcodes, BARCODE_BARCODE_SEPARATOR);
    }

    /**
     * Get several barcodes encoding after {@link RTFastqContstants#ILLUMINA_NAME_BARCODE_DELIMITER}.
     *
     * @param readName         the readName to extract the name from.
     * @param barcodeSeparator the separator between the barcodes.
     *
     * @return the array with the barcodes.
     */
    public static String[] getSeveralBarcodesFromName(final String readName,
            final String barcodeSeparator) {
        final String combined = getOnlyBarcodeFromName(readName);
        return (combined == null) ? null : combined.split(barcodeSeparator);
    }

    /**
     * Get several barcodes, encoding after {@link RTFastqContstants#ILLUMINA_NAME_BARCODE_DELIMITER},
     * and separated between each other with {@link #BARCODE_BARCODE_SEPARATOR}.
     *
     * @param readName the readName to extract the name from.
     *
     * @return the array with the barcodes.
     */
    public static String[] getSeveralBarcodesFromName(final String readName) {
        return getSeveralBarcodesFromName(readName, BARCODE_BARCODE_SEPARATOR);
    }

    /**
     * Get the readName removing everything from the {@link RTFastqContstants#ILLUMINA_NAME_BARCODE_DELIMITER}
     * to the end.
     *
     * @param readName the readName to extract the name from.
     *
     * @return the readName without the barcode information (if present).
     */
    public static String getNameWithoutBarcode(final String readName) {
        int index = readName.indexOf(RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER);
        if (index != -1) {
            return readName.substring(0, index);
        }
        return readName;
    }
}
