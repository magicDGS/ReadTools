package org.magicdgs.utils;

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
     * byte[] and return two indexes where the byte array should be clipped, such as that the caller
     * can then invoke things like:
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
}
