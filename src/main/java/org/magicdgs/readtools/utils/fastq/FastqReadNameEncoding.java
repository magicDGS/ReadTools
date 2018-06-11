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

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.read.RTReadUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Arrays;
import java.util.Optional;
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
     * Gets the plain read name for this encoding, trimming everything after the white-space.
     *
     * @return illumina read name without comments.
     *
     * @throws IllegalArgumentException if the name is wrongly encoded.
     */
    public String getPlainName(final String readName) {
        final Matcher matcher = pattern.matcher(readName);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Wrong encoded read name for " + name() + " encoding: " + readName);
        }
        // remove trailing white spaces if present
        final String plainName = matcher.group(1);
        final int index = plainName.indexOf(" ");
        return (index == -1) ? plainName : plainName.substring(0, index);
    }

    /**
     * Gets the PF flag from the read name.
     *
     * <p>Note: this information is only encoded in the {@link #CASAVA} formatting.
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
    public RTFastqContstants.PairEndInfo getPairedState(final String readName) {
        if (pairInfoGroup == -1) {
            // TODO: to maintain current behavior this is single - but it should be unknown
            return RTFastqContstants.PairEndInfo.SINGLE;
        }
        final Matcher matcher = pattern.matcher(readName);
        String pairInfo = null;
        if (matcher.find()) {
            pairInfo = matcher.group(pairInfoGroup);
        }
        // TODO: this if clause maintains current behavior - remove to return unknown!
        if (pairInfo == null) {
            return RTFastqContstants.PairEndInfo.SINGLE;
        }
        return RTFastqContstants.PairEndInfo.fromString(pairInfo);
    }

    /**
     * Returns the second of pair status of the read.
     *
     * <p>Note: {@code false} does not mean that the read is not paired or have the '1' mark.
     *
     * @param readName the read name to extract the information.
     *
     * @return {@code true} if the read have the '2' mark; {@code false} otherwise.
     */
    public boolean isSecondOfPair(final String readName) {
        return RTFastqContstants.PairEndInfo.SECOND == getPairedState(readName);
    }

    /**
     * Returns the first of pair status of the read.
     *
     * <p>Note: {@code false} does not mean that the read is not paired or have the '2' mark.
     *
     * @param readName the read name to extract the information.
     *
     * @return {@code true} if the read have the '1' mark; {@code false} otherwise.
     */
    public boolean isFirstOfPair(final String readName) {
        return RTFastqContstants.PairEndInfo.FIRST == getPairedState(readName);
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

    // TODO: add documentation!
    public static Optional<FastqReadNameEncoding> detectReadNameEncoding(final String readName) {
        return Arrays.stream(FastqReadNameEncoding.values())
                .filter(e -> e.pattern.matcher(readName).find())
                .findFirst();
    }

    /**
     * Detects the format for the read name provided, and updates the read information.
     *
     * <p>The following information will be updated:
     * <ul>
     * <li>Read name according to SAM specs (no barcode or pair-end information), without
     * white-space.</li>
     * <li>Pair-end information in the bitwise flag (using {@link #getPairedState(String)}).</li>
     * <li>PF information in the bitwise flag (using {@link #isPF(String)}).</li>
     * <li>Barcode information in the default tag (using {@link #getBarcodes(String)}).</li>
     * </ul>
     *
     * @param read     the read to update.
     * @param readName the read name from a FASTQ file.
     */
    // TODO: should be deprecated in favor of the codec code!
    public static void updateReadFromReadName(final GATKRead read, final String readName) {
        updateReadFromReadName(read, readName);
    }
}
