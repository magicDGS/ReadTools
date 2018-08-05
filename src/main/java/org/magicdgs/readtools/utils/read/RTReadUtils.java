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

package org.magicdgs.readtools.utils.read;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.fastq.RTFastqContstants;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.util.SequenceUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

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

    /**
     * Default raw barcode tag (as defined in the SAM specs).
     * Corresponds to {@link SAMTag#BC}.
     */
    public final static String RAW_BARCODE_TAG = SAMTag.BC.name();

    /**
     * Default raw barcode tag for qualities (as defined in the SAM specs).
     * Corresponds to {@link SAMTag#QT}.
     */
    public final static String RAW_BARCODE_QUALITY_TAG = SAMTag.QT.name();

    /** Default raw barcode tag ({@link #RAW_BARCODE_TAG}) as a singleton list. */
    public final static List<String> RAW_BARCODE_TAG_LIST =
            Collections.singletonList(RAW_BARCODE_TAG);

    // default charset for read hash computation
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    /**
     * Computes the hash for the read using the following information:
     *
     * <ul>
     * <li>Read name</li>
     * <li>Paired flag</li>
     * <li>First and second of pair flags</li>
     * <li>Fails platform/vendor quality checks</li>
     * <li>Sequence bases (in the forward strand; reversed-complement if necessary)</li>
     * <li>Base qualities (in the forward strand; reversed if necessary)</li>
     * </ul>
     *
     * <p>Note: this method is useful for compared mapped/unmapped reads independently of flags
     * only set for mapped reads.
     *
     * @param read         the read to get the hash from.
     * @param hashFunction function to compute the hash.
     *
     * @return hash object to consume.
     */
    public static HashCode readHash(final GATKRead read, final HashFunction hashFunction) {
        return readHash(read, Collections.emptyList(), hashFunction);
    }

    /**
     * Computes the hash for the read using {@link #readHash(GATKRead, HashFunction)}, adding
     * information from the attributes in an order-independent way.
     *
     * @param read         the read to get the hash from.
     * @param attributes   attributes to include in the hash. May be empty.
     * @param hashFunction function to compute the hash.
     */
    public static HashCode readHash(final GATKRead read, final Collection<String> attributes,
            final HashFunction hashFunction) {
        Utils.nonNull(attributes, "null attributes");
        Utils.nonNull(read, "null read");
        Utils.nonNull(hashFunction, "null hashFunction");
        final Hasher hasher = hashFunction.newHasher().putObject(read, DEFAULT_READ_FUNNEL);
        // if there is no attributes, return just the read hash
        if (attributes.isEmpty()) {
            return hasher.hash();
        }
        // otherwise, compute a sum of the attributes
        long attHash = 0;
        boolean nonNull = false;
        for (final String att : attributes) {
            try {
                // try first with an integer value
                final Integer val = read.getAttributeAsInteger(att);
                // if it is null, do not add to the hash (not present)
                if (val != null) {
                    attHash += hashFunction.hashInt(val).asLong();
                    nonNull = true;
                }
            } catch (final GATKException.ReadAttributeTypeMismatch e) {
                // always non-null, because it failed while casting instead of returning null
                nonNull = true;
                // getAttributeAsByteArray() returns the bytes from a String with the default
                // charset (UTF-8), and like that we do not have to bother if the attribute is
                // a String or a byte[] originally
                attHash += hashFunction.hashBytes(read.getAttributeAsByteArray(att)).asLong();
            }
        }
        // if the attributeHash is zero, that means that no attribute was included
        // or that their hash sum is zero
        return (nonNull) ? hasher.putLong(attHash).hash() : hasher.hash();
    }



    // funnel for hassing a read, including the information in the following order:
    // - read name (UTF-8 encoding)
    // - paired
    // - first of pair
    // - second of pair
    // - fails vendor quality
    // -bases (reverse complement if necessary),
    // - qualities (reversed if necessary)
    private static final Funnel<GATKRead> DEFAULT_READ_FUNNEL = (read, sink) -> {
        // get the basese and qualities and reverse if necessary
        byte[] bases = read.getBases();
        final byte[] qualities = read.getBaseQualities();
        if (read.isReverseStrand()) {
            SequenceUtil.reverseComplement(bases);
            SequenceUtil.reverseQualities(qualities);
        }
        sink.putString(read.getName(), DEFAULT_CHARSET)
                .putBoolean(read.isPaired())
                .putBoolean(read.isFirstOfPair())
                .putBoolean(read.isSecondOfPair())
                .putBoolean(read.failsVendorQualityCheck())
                .putBytes(bases)
                .putBytes(qualities);
    };

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
        return tags.stream().map(read::getAttributeAsString)
                .filter(value -> value != null)
                .flatMap(value -> Stream.of(value.split(RTDefaults.BARCODE_INDEX_DELIMITER)))
                .toArray(String[]::new);
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
                    qualSplit = qualVal.split(RTDefaults.BARCODE_QUALITY_DELIMITER);
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
     * Returns the raw barcodes from the {@link #RAW_BARCODE_TAG} tag.
     *
     * @param read the read to extract the barcodes from.
     *
     * @return the barcodes in the provided tags (in order) if any; empty array otherwise.
     */
    public static String[] getRawBarcodes(final GATKRead read) {
        return getBarcodesFromTags(read, RAW_BARCODE_TAG_LIST);
    }

    /**
     * Sets the {@link #RAW_BARCODE_TAG} tag for a read, joining the barcodes with {@link
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
            read.setAttribute(RAW_BARCODE_TAG,
                    String.join(RTDefaults.BARCODE_INDEX_DELIMITER, barcodes));
        }
    }


    /**
     * Sets the {@link #RAW_BARCODE_TAG} and {@link #RAW_BARCODE_QUALITY_TAG} tags for a read,
     * joining the barcodes/qualities with {@link RTDefaults#BARCODE_INDEX_DELIMITER}.
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
            final String qualityString =
                    String.join(RTDefaults.BARCODE_QUALITY_DELIMITER, qualities);
            // perform extra validation of lengths
            if (barcodeString.length() != qualityString.length()) {
                throwExceptionForDifferentBarcodeQualityLenghts(RAW_BARCODE_TAG, barcodeString,
                        RAW_BARCODE_QUALITY_TAG, qualityString);
            }
            read.setAttribute(RAW_BARCODE_TAG, barcodeString);
            read.setAttribute(RAW_BARCODE_QUALITY_TAG, qualityString);
        }
    }

    private static void throwExceptionForDifferentBarcodeQualityLenghts(final String barcodeTag,
            final String bcValue, final String qualityTag, final String qualVal) {
        throw new IllegalArgumentException(
                "Barcodes and qualities have different lengths: "
                        + barcodeTag + "=" + bcValue + " vs. " + qualityTag + "=" + qualVal);
    }

    /**
     * Removes trimming point tags ({@link ReservedTags#ts} and {@link ReservedTags#te}) on the
     * read.
     *
     * Note: The completely trim tag ({@link ReservedTags#ct}) will be updated, but not removed to
     * keep track of completely trim reads.
     *
     * @param read the read to update.
     */
    public static void clearTrimmingPointTags(final GATKRead read) {
        updateCompletelyTrimReadFlag(read);
        read.clearAttribute(ReservedTags.ts);
        read.clearAttribute(ReservedTags.te);
    }

    /**
     * Updates the trimming tags ({@link ReservedTags#ts}, {@link ReservedTags#te} and
     * {@link ReservedTags#ct}) on a read, with the specified trimming points.
     *
     * @param read  the read to update.
     * @param start the first trimming point.
     * @param end   the last trimming point.
     *
     * @see #updateTrimmingStartPointTag(GATKRead, int)
     * @see #updateTrimmingEndPointTag(GATKRead, int)
     * @see #updateCompletelyTrimReadFlag(GATKRead)
     */
    public static void updateTrimmingPointTags(final GATKRead read, final int start,
            final int end) {
        updateTrimmingStartPointTag(read, start);
        updateTrimmingEndPointTag(read, end);
        updateCompletelyTrimReadFlag(read);
    }

    /**
     * Updates the start trimming tag ({@link ReservedTags#ts}) on a read, with the specified
     * trimming points.
     *
     * If the read already have the tag, it conserves the right-most trim point.
     *
     * Note: the completely trim tag ({@link ReservedTags#ct}) is not updated.
     *
     * @param read  the read to update.
     * @param start the first trimming point.
     */
    public static void updateTrimmingStartPointTag(final GATKRead read, final int start) {
        Utils.nonNull(read, "null read");
        Utils.validateArg(start >= 0, "negative start not allowed");
        // get the previous start point
        int startPoint = getTrimmingStartPoint(read);
        // if there is not start point, and/or
        if (start > startPoint) {
            startPoint = start;
        }
        read.setAttribute(ReservedTags.ts, startPoint);
    }

    /** Returns the trimming start point from the {@link ReservedTags#ts} if present; 0 otherwise. */
    public static int getTrimmingStartPoint(final GATKRead read) {
        Utils.nonNull(read, "null read");
        return getIntAttributeOrDefault(read, ReservedTags.ts, () -> 0);
    }

    /**
     * Updates the start trimming tag ({@link ReservedTags#ts}) on a read, with the specified
     * trimming points.
     *
     * If the read already have the tag, it conserves the left-most trim point.
     *
     * Note: the completely trim tag ({@link ReservedTags#ct}) is not updated.
     *
     * @param read the read to update.
     * @param end  the last trimming point.
     */
    public static void updateTrimmingEndPointTag(final GATKRead read, final int end) {
        Utils.nonNull(read, "null read");
        Utils.validateArg(end >= 0, "negative start not allowed");
        int endPoint = getTrimmingEndPoint(read);
        if (end < endPoint) {
            endPoint = end;
        }
        read.setAttribute(ReservedTags.te, endPoint);
    }

    /**
     * Returns the trimming end point from the {@link ReservedTags#ts} if present; read length
     * otherwise.
     */
    public static int getTrimmingEndPoint(final GATKRead read) {
        Utils.nonNull(read, "null read");
        return getIntAttributeOrDefault(read, ReservedTags.te, read::getLength);
    }

    /**
     * Returns {@code false} if the {@link ReservedTags#ct} is 0 or does not exists; {@code true}
     * otherwise.
     *
     * Warning: this method does not take into account the trimming points. If the read is
     * completely trimmed but the {@link ReservedTags#ct} was not updated, the method will return
     * {@code false}. Use the {@link #updateCompletelyTrimReadFlag(GATKRead)} for safe retrieval
     * of the tag.
     */
    public static boolean isCompletelyTrimRead(final GATKRead read) {
        Utils.nonNull(read, "null read");
        return getIntAttributeOrDefault(read, ReservedTags.ct, () -> 0) != 0;
    }

    /**
     * Updates the completely trim flag if the read with the start/end trim points, only if the
     * read does not have a value which indicates that is already completely trim
     * ({@code ReservedTags.ct != 0}).
     *
     * Based on the trim points, the read is completely trimmed if:
     *
     * - If the {@link ReservedTags#ct} is already set to a value different from 0.
     * - If the start point is equals to the read length ({@code ReservedTags.ct = 1}).
     * - If the end point is equals to 0 ({@code ReservedTags.ct = 2}.
     * - If the start and end points are exactly the same ({@code ReservedTags.ct = 3}.
     * - If the start point is after the end ({@code ReservedTags.ct = 3}.
     * - If the end point is before the start (({@code ReservedTags.ct = 3}.
     *
     * Otherwise, the read is not completely trimmed (ct=0).
     *
     * If the read is completely trimmed, it will be marked in-place with the
     * {@link ReservedTags#ct} tag; otherwise, it will be marked with 0.
     *
     * @param read the read to update.
     *
     * @return {@code true} if the read is completely trim; {@code false} otherwise.
     */
    public static boolean updateCompletelyTrimReadFlag(final GATKRead read) {
        // check first if the completely trim flag is already set to a different of 0 value
        if (isCompletelyTrimRead(read)) {
            return true;
        }
        // then, check the start point
        final int startPoint = getTrimmingStartPoint(read);
        // if the start is already larger, return
        if (startPoint == read.getLength()) {
            read.setAttribute(ReservedTags.ct, 1);
            return true;
        }
        // then, check the end point
        final int endPoint = getTrimmingEndPoint(read);
        // if the end is already smaller, return
        if (endPoint == 0) {
            read.setAttribute(ReservedTags.ct, 2);
            return true;
        }
        // now check the other condition
        final int comp = Integer.compare(startPoint, endPoint);
        if (comp >= 0) {
            read.setAttribute(ReservedTags.ct, 3);
            return true;
        }
        read.setAttribute(ReservedTags.ct, 0);
        return false;
    }

    // helper for return a default value from an integer tag
    private static int getIntAttributeOrDefault(final GATKRead read, final String tag,
            final IntSupplier defaultValue) {
        final Integer value = read.getAttributeAsInteger(tag);
        return (value == null) ? defaultValue.getAsInt() : value;
    }

    /**
     * Fix a SAM tag that should be in sync for the two reads in a read pair. If it is only present
     * in one of them, set that tag in the other.
     *
     * Note: If it is not set in any or it is in both, they keep this state.
     *
     * @param tag   the tag to fix.
     * @param read1 first read in the pair.
     * @param read2 second read in the pair.
     */
    public static void fixPairTag(final String tag, final GATKRead read1, final GATKRead read2) {
        final String tagVal1 = read1.getAttributeAsString(tag);
        final String tagVal2 = read2.getAttributeAsString(tag);
        if (tagVal1 == null && tagVal2 != null) {
            read1.setAttribute(tag, tagVal2);
        } else if (tagVal2 == null && tagVal1 != null) {
            read2.setAttribute(tag, tagVal1);
        }
        // TODO: should we also check if they are the same?
    }

    /**
     * Gets the read name with the raw barcode included into it if they are present (separated with
     * {@link RTFastqContstants#ILLUMINA_NAME_BARCODE_DELIMITER}) if present.
     *
     * @param read the read to get the information from.
     */
    public static String getReadNameWithIlluminaBarcode(final GATKRead read) {
        // get the raw barcode information
        final String[] barcodes = RTReadUtils.getRawBarcodes(read);
        // if not found, returns the name directly
        if (barcodes.length == 0) {
            return read.getName();
        }
        // if not, add them
        return read.getName()
                + RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER
                + String.join(RTDefaults.BARCODE_INDEX_DELIMITER, barcodes);
    }
}
