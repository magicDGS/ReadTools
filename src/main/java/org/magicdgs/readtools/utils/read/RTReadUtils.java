/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.utils.read;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.fastq.RTFastqContstants;

import htsjdk.samtools.SAMTag;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Collections;
import java.util.List;

/**
 * Static utils for handling {@link GATKRead} in ReadTools.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTReadUtils {

    /** Cannot be instantiated. */
    private RTReadUtils() {}

    /** Zero quality character. */
    public final static char ZERO_QUALITY_CHAR = '!';

    // this is for avoid re-instantation
    private final static List<String> RAW_BARCODE_TAGS =
            Collections.singletonList(SAMTag.BC.name());

    /**
     * Extract and remove the barcode from the read name, splitting the barcodes in the read name
     * using {@link RTDefaults#BARCODE_INDEX_DELIMITER}. The read will
     *
     * The barcode should be encoded in the Illumina format (not pair-end information included),
     * for example (barcode delimiter in the example is the default):
     *
     * - readName will return an empty array.
     * - readName#ACGT will return (ACGT).
     * - readName#ACGT-TTTT will return (ACGT, TTTT)
     *
     * @return the barcodes in the read name if any; empty array otherwise.
     */
    public static String[] extractBarcodesFromReadName(final GATKRead read) {
        Utils.nonNull(read, "null read");
        final String originalName = read.getName();
        final int barcodeStartIndex = originalName
                .indexOf(RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER);
        // if not found, return an empty array
        if (barcodeStartIndex == -1) {
            return new String[0];
        }
        read.setName(originalName.substring(0, barcodeStartIndex));
        return originalName.substring(barcodeStartIndex + 1, originalName.length())
                .split(RTDefaults.BARCODE_INDEX_DELIMITER);
    }

    /**
     * Returns an array that contains the barcodes stored in the provided tags. The tags are
     * splitted by {@link RTDefaults#BARCODE_INDEX_DELIMITER} to check if there are more than 1
     * sequence. If the value for a tag is {@code null} (does not exists), this tag is skipped.
     *
     * Note: no validation is performed to check if the tags are real barcodes.
     *
     * @param read the read to extract the barcodes from.
     * @param tags the tags containing the barcodes, in order.
     *
     * @return the barcodes in the provided tags (in order) if any; empty array otherwise.
     *
     * @see #getBarcodesAndQualitiesFromTags(GATKRead, List, List) if qualities are also needed.
     */
    public static String[] getBarcodesFromTags(final GATKRead read, final List<String> tags) {
        Utils.nonNull(read, "null read");
        Utils.nonEmpty(tags, "empty tags");
        String[] barcodes = new String[0];
        for (int i = 0; i < tags.size(); i++) {
            final String value = read.getAttributeAsString(tags.get(i));
            if (value != null) {
                barcodes = ArrayUtils
                        .addAll(barcodes, value.split(RTDefaults.BARCODE_INDEX_DELIMITER));
            }
        }
        return barcodes;
    }

    /**
     * Returns an pair of arrays of the same length that contains the barcodes/qualities stored in
     * the provided tags. The tags are splitted by {@link RTDefaults#BARCODE_INDEX_DELIMITER} to
     * check if there are more than 1 sequence/quality. If the value for a tag is {@code null}
     * (does not exists), this tag is skipped (also the quality without checking). If the quality
     * is empty for the corresponding tag, the quality is filled with {@link #ZERO_QUALITY_CHAR} to
     * match the same length.
     *
     * Note: no validation is performed to check if the tags are real barcodes/qualities.
     *
     * @param read        the read to extract the barcodes from.
     * @param barcodeTags the tags containing the barcodes, in order.
     * @param qualityTags the tags containing the qualities, in order.
     *
     * @return the barcodes in the provided tags (in order) if any; empty array otherwise.
     *
     * @throws IllegalStateException if for a concrete barcode/quality tag, the length is
     *                               different.
     * @see #getBarcodesFromTags(GATKRead, List) (GATKRead, List) if qualities are not needed.
     */
    public static Pair<String[], String[]> getBarcodesAndQualitiesFromTags(final GATKRead read,
            final List<String> barcodeTags, final List<String> qualityTags) {
        Utils.nonNull(read, "null read");
        Utils.nonEmpty(barcodeTags, "empty barcodeTags");
        Utils.nonEmpty(qualityTags, "empty qualityTags");
        Utils.validateArg(barcodeTags.size() == qualityTags.size(), "tags lenghts should be equal");
        String[] barcodes = new String[0];
        String[] qualities = new String[0];
        for (int i = 0; i < barcodeTags.size(); i++) {
            final String bcValue = read.getAttributeAsString(barcodeTags.get(i));
            if (bcValue != null) {
                final String[] bcSplit = bcValue.split(RTDefaults.BARCODE_INDEX_DELIMITER);
                final String qualVal = read.getAttributeAsString(qualityTags.get(i));
                final String[] qualSplit;
                if (qualVal == null) {
                    qualSplit = new String[bcSplit.length];
                    // fill in with '!'
                    for (int j = 0; j < bcSplit.length; j++) {
                        qualSplit[0] = StringUtils.repeat(ZERO_QUALITY_CHAR, bcSplit[0].length());
                    }
                } else if (bcValue.length() != qualVal.length()) {
                    throwExceptionForDifferentBarcodeQualityLenghts(barcodeTags.get(i), bcValue,
                            qualityTags.get(i), qualVal);
                    // this break should be included because qualSplit is not initialized
                    break;
                } else {
                    qualSplit = qualVal.split(RTDefaults.BARCODE_INDEX_DELIMITER);
                    if (bcSplit.length != qualSplit.length) {
                        throwExceptionForDifferentBarcodeQualityLenghts(barcodeTags.get(i), bcValue,
                                qualityTags.get(i), qualVal);
                    }
                }
                barcodes = ArrayUtils.addAll(barcodes, bcSplit);
                qualities = ArrayUtils.addAll(qualities, qualSplit);
            }
        }
        return Pair.of(barcodes, qualities);
    }

    /**
     * Returns the raw barcodes from the {@link SAMTag#BC} tag.
     *
     * @param read the read to extract the barcodes from.
     *
     * @return the barcodes in the provided tags (in order) if any; empty array otherwise.
     */
    public static String[] getRawBarcodes(final GATKRead read) {
        return getBarcodesFromTags(read, RAW_BARCODE_TAGS);
    }

    /**
     * Sets the {@link SAMTag#BC} tag for a read, joining the barcodes with {@link
     * RTDefaults#BARCODE_INDEX_DELIMITER}.
     *
     * @param read     the read to update with the barcodes.
     * @param barcodes barcodes to use. If the array is empty, no update will be performed.
     */
    public static void addBarcodesTagToRead(final GATKRead read, final String[] barcodes) {
        Utils.nonNull(read, "null read");
        Utils.nonNull(barcodes, "null barcodes");
        // only update if there are barcodes
        if (barcodes.length != 0) {
            read.setAttribute(SAMTag.BC.name(),
                    String.join(RTDefaults.BARCODE_INDEX_DELIMITER, barcodes));
        }
    }


    /**
     * Sets the {@link SAMTag#BC} and {@link SAMTag#QT} tags for a read, joining the
     * barcodes/qualities with {@link RTDefaults#BARCODE_INDEX_DELIMITER}.
     *
     * Note: barcodes and qualities should have the same length.
     *
     * @param read      the read to update with the barcodes.
     * @param barcodes  barcodes to use. If the array is empty, no update will be performed.
     * @param qualities associated qualities for the barcodes.
     */
    public static void addBarcodeWithQualitiesTagsToRead(final GATKRead read,
            final String[] barcodes, final String[] qualities) {
        Utils.nonNull(read, "null read");
        Utils.nonNull(barcodes, "null barcodes");
        Utils.nonNull(qualities, "null qualities");
        Utils.validateArg(barcodes.length == qualities.length,
                "barcodes.length != qualities.length");
        // only ipdate if there are barcodes
        if (barcodes.length != 0) {
            final String barcodeString = String.join(RTDefaults.BARCODE_INDEX_DELIMITER, barcodes);
            final String qualityString = String.join(RTDefaults.BARCODE_INDEX_DELIMITER, qualities);
            // perform extra validation of lengths
            if (barcodeString.length() != qualityString.length()) {
                throwExceptionForDifferentBarcodeQualityLenghts(SAMTag.BC.name(), barcodeString,
                        SAMTag.QT.name(), qualityString);
            }
            read.setAttribute(SAMTag.BC.name(), barcodeString);
            read.setAttribute(SAMTag.QT.name(), qualityString);
        }
    }

    private static void throwExceptionForDifferentBarcodeQualityLenghts(final String barcodeTag,
            final String bcValue, final String qualityTag, final String qualVal) {
        throw new IllegalArgumentException(
                "Barcodes and qualities have different lengths: "
                        + barcodeTag + "=" + bcValue + " vs. " + qualityTag + "=" + qualVal);
    }

}
