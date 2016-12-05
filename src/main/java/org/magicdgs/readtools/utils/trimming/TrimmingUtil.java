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

package org.magicdgs.readtools.utils.trimming;

import org.broadinstitute.hellbender.utils.BaseUtils;
import org.broadinstitute.hellbender.utils.Utils;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * Static class with trimming methods for qualities (phred) and bases
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingUtil {

    private TrimmingUtil() {}

    /**
     * Implements quality trimming with the Mott algorithm. Takes in an array of quality values as
     * byte[] and return two indexes where the byte array should be clipped, such as that the
     * caller can then invoke things like:
     * int[] retval = trimPointsMott(quals, trimQual)
     * final byte[] trimmedQuals = Array.copyOfRange(quals, retval[0], retval[1])
     * final String trimmedBases = bases.substring(retval[0], retval[1])
     *
     * If the entire read is of low quality this function may return [1uals.length, quals.length]!
     * It is left to the caller to decide whether or not to trim reads down to 0-bases, or to
     * enforce some minimum length.
     *
     * @param quals    a byte[] of quality scores in phred scaling (i.e. integer values between 0
     *                 and ~60)
     * @param trimQual the lowest quality that is considered "good"
     *
     * @return the zero-base indexes which should be trimmed. When no trimming is required,
     * [0, quals.length] will be returned.
     */
    public static int[] trimPointsMott(final byte[] quals, final int trimQual) {
        Utils.nonNull(quals, "null quals");
        Utils.validateArg(trimQual >= 0, "negative trimQual");
        final TreeMap<Integer, int[]> hsps = new TreeMap<>();
        int highScore = 0;
        int activeScore = 0;
        final int[] positions = new int[] {-1, 0};
        for (int i = 0; i < quals.length; i++) {
            activeScore += (quals[i] - trimQual);
            if (activeScore > 0) {
                if (activeScore > highScore) {
                    highScore = activeScore;
                    positions[1] = i;
                }
                if (positions[0] == -1) {
                    positions[0] = i;
                }
            } else {
                positions[1]++;
                if (highScore > 0) {
                    hsps.put(highScore, Arrays.copyOf(positions, positions.length));
                }
                positions[0] = -1;
                positions[1] = 0;
                activeScore = 0;
                highScore = 0;
            }
        }
        if (highScore > 0) {
            positions[1]++;
            hsps.put(highScore, positions); // this array is not going to be modified anymore
        }
        if (hsps.isEmpty()) {
            return new int[] {quals.length, quals.length};
        }
        return hsps.get(hsps.lastKey());
    }

    /**
     * Implements trailing Ns (unknown nucleotide) trimming. Tajes in an array of sequence value as
     * byte[] and return two indexes where the byte array should be clipped, such as that the
     * caller can then invoke things like:
     * int[] retval = trimPointsTrailingNs(bases)
     * final byte[] trimmedQuals = Array.copyOfRange(quals, retval[0], retval[1])
     * final String trimmedBases = bases.substring(retval[0], retval[1])
     *
     * @param bases a byte[] of bases (ACTGN)
     *
     * @return the zero-base indexes which should be trimmed. When no trimming is required,
     * [0, quals.length] will be returned.
     */
    public static int[] trimPointsTrailingNs(final byte[] bases) {
        Utils.nonNull(bases, "null bases");
        final int[] positions = new int[] {0, bases.length};
        // first check if trimming is required for the
        for (int i = 0; i < bases.length; i++) {
            if (!BaseUtils.isNBase(bases[i])) {
                break;
            }
            positions[0]++;
        }
        if (positions[0] != bases.length) {
            for (int i = bases.length - 1; i > positions[0]; i--) {
                if (!BaseUtils.isNBase(bases[i])) {
                    break;
                }
                positions[1]--;
            }
        }
        return positions;
    }

}
