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
 * SOFTWARE.
 */

package org.magicdgs.readtools.utils.fastq;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.read.RTReadUtils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enum for encoding of the read name in the FASTQ format. Some of this names contain important
 * information that cannot be lost in the processing step. Because there are very strict formatting
 * of headers in the BAM file, processing read names from FASTQ files is important for lossy
 * transformation.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public enum FastqReadNameEncoding {

    // CASAVA should go first because ILLUMINA detects all kind of read names even without barcode pair/information
    // regexp for CASAVA is as following:
    // first group  = ([\\S]+)   -> any word character is the read name
    // no group     = \\s+       -> any white character for separate the comment
    // the following groups are separated by ':'
    // second group = ([012])    -> the pair-information ('1', '2', or '0')
    // third group  = ([YN])     -> PF flag (vendors quality)
    // no group     = [0-9]+     -> numeric value that does not contain important information in our framework
    // fourth group = ([ATCGN]+) -> the barcode information (restricted to nucleotides)
    CASAVA("([\\S]+)\\s+([012]):([YN]):[0-9]+:([ATCGN]+).?", 2, 4, 3),
    // this ILLUMINA pattern match with/without barcodes
    // first group  = ([^#/]+)                 -> any character that is not the marker of barcode or pair-info separator
    // second group = (#([^/\\s]+))?           -> one or none of # followed by something that is not a / or white space
    // third group  = nested group in previous -> the string between # and / or space (barcode)
    // fourth group = (/([012]){1})?           -> match '/0', '/1' or '/2' or nothing
    // fifth group  = nested group in previous -> match '0', '1' or '2'
    ILLUMINA("([^#/]+)(" + RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER
            + "([^/\\s]+))?(/([012]){1})?.?", 5, 3, -1);

    private static Logger logger = LogManager.getLogger(FastqReadNameEncoding.class);

    // pre-compiled pattern
    private final Pattern pattern;
    // the group where the pair information, barcode or PF flag is stored within the pattern
    private final int pairInfoGroup;
    private final int barcodeGroup;
    private final int pfGroup;

    /** Enum constructor, which indicates in which group is the information (-1 if not available). */
    FastqReadNameEncoding(final String pattern, final int pairInfoGroup, final int barcodeGroup,
            final int pfGroup) {
        this.pattern = Pattern.compile(pattern);
        this.pairInfoGroup = pairInfoGroup;
        this.barcodeGroup = barcodeGroup;
        this.pfGroup = pfGroup;
    }

    /**
     * Normalize the read name with this encoding. Includes the barcode if it can be detected.
     *
     * @return illumina read name without comments; {@code null} if the read name is not encoded
     * with this format.
     */
    @VisibleForTesting
    String getPlainName(final String readName) {
        final Matcher matcher = pattern.matcher(readName);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Wrong encoded read name for " + name() + " encoding: " + readName);
        }
        return matcher.group(1);
    }

    /**
     * Gets the PF flag from the read name.
     *
     * Note: this is only encoded int the {@link #CASAVA} formatting.
     *
     * @param readName the read name.
     *
     * @return {@code true} if 'Y' is found in CASAVA formatting; {@code false} otherwise.
     */
    public boolean isPF(final String readName) {
        if (pfGroup != -1) {
            final Matcher matcher = pattern.matcher(readName);
            if (matcher.find()) {
                return matcher.group(pfGroup).equals("Y");
            }
        }
        return false;
    }

    /**
     * Returns the paired state for this read (0, 1 or 2).
     *
     * @param readName the read name to extract the pair state.
     *
     * @return the pair state 1 or 2; 0 if not information.
     */
    public String getPairedState(final String readName) {
        if (pairInfoGroup == -1) {
            return "0";
        }
        final Matcher matcher = pattern.matcher(readName);
        String pairInfo = null;
        if (matcher.find()) {
            pairInfo = matcher.group(pairInfoGroup);
        }
        return pairInfo == null ? "0" : pairInfo;
    }

    /**
     * Returns the second of pair status of the read.
     *
     * Note: {@code false} does not mean that the read is not paired or have the '1' mark.
     *
     * @param readName the read name to extract the information.
     *
     * @return {@code true} if the read have the '2' mark; {@code false} otherwise.
     */
    public boolean isSecondOfPair(final String readName) {
        return getPairedState(readName).equals("2");
    }

    /**
     * Returns the first of pair status of the read.
     *
     * Note: {@code false} does not mean that the read is not paired or have the '1' mark.
     *
     * @param readName the read name to extract the information.
     *
     * @return {@code true} if the read have the '2' mark; {@code false} otherwise.
     */
    public boolean isFirstOfPair(final String readName) {
        return getPairedState(readName).equals("1");
    }

    /**
     * Returns the barcodes in the read name (splitted by {@link RTDefaults#BARCODE_INDEX_DELIMITER}).
     *
     * @param readName the read name to extract the information.
     *
     * @return the barcodes in the read name; empty array if information is not present.
     */
    public String[] getBarcodes(final String readName) {
        if (barcodeGroup != -1) {
            final Matcher matcher = pattern.matcher(readName);
            if (matcher.find()) {
                final String barcode = matcher.group(barcodeGroup);
                if (barcode != null) {
                    return barcode.split(RTDefaults.BARCODE_INDEX_DELIMITER);
                }
            }
        }
        return new String[0];
    }

    /**
     * Detects the format for the read name, and update the read with the following information:
     *
     * - Read name according to SAM specs (Illumina-like read name with barcode but without pair
     * information).
     * - Second of pair information detected with {@link #isSecondOfPair(String)}
     * - PF information if {@link #isPF(String)}
     *
     * @param read     the read to update.
     * @param readName the read name from a FASTQ file.
     */
    public static void updateReadFromReadName(final GATKRead read, final String readName) {
        // gets the firt encoding that match, in the order of the enum
        final FastqReadNameEncoding encoding = Arrays.stream(FastqReadNameEncoding.values())
                .filter(e -> e.pattern.matcher(readName).find())
                .findFirst().orElse(null);
        if (encoding == null) {
            throw new GATKException.ShouldNeverReachHereException("Encoding should not be null.");
        } else {
            logger.debug("Detected encoding: {}", encoding);
            read.setName(encoding.getPlainName(readName));
            final String pairState = encoding.getPairedState(readName);
            switch (pairState) {
                case "0":
                    read.setIsPaired(false);
                    break;
                case "1":
                    read.setIsFirstOfPair();
                    break;
                case "2":
                    read.setIsSecondOfPair();
                    break;
                default:
                    throw new GATKException.ShouldNeverReachHereException(
                            "Incorrect detection of pair-state: " + pairState);
            }
            read.setFailsVendorQualityCheck(encoding.isPF(readName));
            RTReadUtils.addBarcodesTagToRead(read, encoding.getBarcodes(readName));
        }
    }

}
