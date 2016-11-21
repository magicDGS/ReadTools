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
     * sequence. If the value for a tag is null, this tag is skipped.
     *
     * Note: no validation is performed to check if the tags are real barcodes.
     *
     * @param read the read to extract the barcodes from.
     * @param tags the tags that contains the barcodes, in order.
     *
     * @return the barcodes in the provided tags (in order) if any; empty array otherwise.
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
     * Sets the {@link SAMTag#BC} tag for a read joining the barcodes with {@link
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

}
