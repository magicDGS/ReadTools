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
package org.magicdgs.readtools.tools.barcodes.dictionary.decoder;

import com.google.common.annotations.VisibleForTesting;
import org.broadinstitute.hellbender.utils.BaseUtils;

import java.util.Set;

/**
 * Best match for barcode against a dictionary.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class BarcodeMatch {

    /** Unknown String for sample and barcode. */
    public static final String UNKNOWN_STRING = "UNKNOWN";

    // if barcode is null, it does not match anything
    private String barcode = null;

    // number of mismatches with the best match
    private int mismatches;

    // number of mismatches wrt second best
    private int mismatchesToSecondBest;

    // number of Ns in the barcode
    private int numberOfNs = 0;

    // 0-based
    private final int index;

    /**
     * Creates a unknown match with the number of mismatches being equal to the maximum number of
     * mismatches.
     *
     * @param maxMismatches the maximum number of mismatches (barcode length).
     */
    private BarcodeMatch(final int index, final int maxMismatches) {
        mismatches = maxMismatches;
        mismatchesToSecondBest = maxMismatches;
        this.index = index;
    }

    /**
     * If the barcode is matched, it was identified as belonging to the returned barcode.
     *
     * WARNING: this method returns {@code true} even if the barcode is ambiguous. This will be
     * changed in the future.
     *
     * @return {@code true} if it is match; {@code false} otherwise.
     */
    public boolean isMatch() {
        // TODO: change to check if is it ambiguous too.
        // TODO: check if this is correct -> it is a change with respect to the previous
        return barcode != null;
    }

    /** Gets the barcode for this match; {@link #UNKNOWN_STRING} if it is not available. */
    public String getBarcode() {
        return (barcode == null) ? UNKNOWN_STRING : barcode;
    }

    /**
     * Gets the index number for the barcode that matches. For instance, if dual-indexed barcodes
     * are applied, it returns 0 for the first and 1 for the second.
     *
     * @return 0-based index of the barcode.
     */
    public int getIndexNumber() {
        return index;
    }

    /** Gets the number of Ns in the sequenced barcode, only considering the matched part. */
    public int getNumberOfNs() {
        return numberOfNs;
    }

    /** Gets the number of mismatches of the sequenced vs. the used barcode. */
    public int getMismatches() {
        return mismatches;
    }

    /**
     * An assignable barcode is the one with:
     *
     * 1) Exact match with the barcode ({@code getMismatches()==0}) if there are no Ns.
     * 2) Less mismatches than the second within a certain threshold.
     *
     * WARNING: for compatibility, the threshold could be 0.
     *
     * @param threshold number of mismatches of difference between this match and the next one.
     *
     * @return {@code true} if the barcode is assignable; {@code false} otherwise.
     */
    public boolean isAssignable(final int threshold) {
        // TODO: we should change the implementation:
        // TODO: 1) does not allow thesholds of 0 -> this will break compatibility
        // TODO: 2) if it is unknown, we should return false directly
        return (numberOfNs == 0 && mismatches == 0)
                || Math.abs(mismatchesToSecondBest - mismatches) >= threshold;
    }

    /**
     * Returns {@code true} if the differences of mismatches with the second best barcode is 0;
     * {@code false} otherwise.
     */
    public boolean isAmbiguous() {
        // even if we do not expect negative values, we consider them ambiguous
        return mismatchesToSecondBest - mismatches <= 0;
    }

    /**
     * Gets the best barcode match.
     *
     * @param index          0-based index of the barcode (if only one, it should be 0).
     * @param barcodeToMatch the barcode to match against the set.
     * @param barcodeSet     the set of barcodes to match against.
     * @param nAsMismatches  if {@code true}, unknown sequences (Ns) count as mismatches.
     *
     * @return the best barcode matched and the information about it.
     */
    public static BarcodeMatch getBestBarcodeMatch(final int index, final String barcodeToMatch,
            final Set<String> barcodeSet, final boolean nAsMismatches) {
        final BarcodeMatch best = new BarcodeMatch(index, barcodeToMatch.length());
        for (final String b : barcodeSet) {
            final String subBarcode = barcodeToMatch.substring(0, b.length());
            final int currentMismatch = hammingDistance(subBarcode, b, nAsMismatches);
            // if the barcodeToMatch is longer but it is cut, this is not really the best barcode,
            // but the sorter one even if all of them have the same mismatches
            // we solve this outside the 'for' loop
            if (currentMismatch < best.mismatches) {
                // if the count of mismatches is better than the previous
                best.mismatchesToSecondBest = best.mismatches;
                best.mismatches = currentMismatch;
                best.barcode = b;
            } else if (currentMismatch < best.mismatchesToSecondBest) {
                // if it is the second best, track the result
                best.mismatchesToSecondBest = currentMismatch;
            }
        }
        // if the best barcode is not null, but the number of mismatches/mismatches to second best
        // is larger or equal than the barcode length, that means that we cannot find the real
        // barcode, and the shorter one is the detected one
        if (best.barcode != null
                && best.mismatches >= best.barcode.length()
                && best.mismatchesToSecondBest >= best.barcode.length()) {
            best.barcode = null;
            best.mismatches = barcodeToMatch.length();
            best.mismatchesToSecondBest = best.mismatches;
        }
        // count the number of Ns
        final String toCount = (best.barcode == null)
                ? barcodeToMatch : barcodeToMatch.substring(0, best.barcode.length());
        best.numberOfNs = (int) toCount.chars().filter(i -> BaseUtils.isNBase((byte) i)).count();

        return best;
    }

    /**
     * Computes Hamming distance (number of mismatches) between a test sequence and a target
     * sequence.
     *
     * @param testSequence   test sequence.
     * @param targetSequence target sequence.
     * @param nAsMismatches  if {@code true} N bases are counted as mismatch; otherwise they are
     *                       ignored.
     *
     * @return hamming distance between the two sequences.
     */
    @VisibleForTesting
    static int hammingDistance(final String testSequence, final String targetSequence,
            final boolean nAsMismatches) {
        // logger.debug("Testing ", testSequence, " against ", targetSequence);
        // if(testSequence.length() != barcode.length()) return testSequence.length();
        int measuredDistance = 0;
        for (int i = 0; i < testSequence.length(); i++) {
            final byte test = (byte) testSequence.charAt(i);
            final byte target = (byte) targetSequence.charAt(i);
            // only count if n is a mismatch of none of the test/target bases is an N
            final boolean shouldCount = nAsMismatches
                    || !(BaseUtils.isNBase(test) || BaseUtils.isNBase(target));
            // case-insensitive mismatches count for N or not N
            if (shouldCount && !BaseUtils.basesAreEqual(test, target)) {
                measuredDistance++;
            }
        }
        return measuredDistance;
    }
}
