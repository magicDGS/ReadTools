/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.utils.mappability.gem;

import org.apache.commons.lang3.Range;

import java.util.Map;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GemMappabilityHeader {

    private final int kmerLength;

    private final int approximationThreshold;

    private final int maxMismatches;

    private final int maxErrors;

    private final int maxBigIndelLength;

    private final int minMatchedBases;

    private final int strataAfterBest;

    private final Map<Byte, Range<Integer>> encoding;

    public GemMappabilityHeader(int kmerLength, int approximationThreshold, int maxMismatches,
            int maxErrors, int maxBigIndelLength, int minMatchedBases, int strataAfterBest,
            Map<Byte, Range<Integer>> encoding) {
        this.kmerLength = kmerLength;
        this.approximationThreshold = approximationThreshold;
        this.maxMismatches = maxMismatches;
        this.maxErrors = maxErrors;
        this.maxBigIndelLength = maxBigIndelLength;
        this.minMatchedBases = minMatchedBases;
        this.strataAfterBest = strataAfterBest;
        this.encoding = encoding;
    }

    public int getKmerLength() {
        return kmerLength;
    }

    public int getApproximationThreshold() {
        return approximationThreshold;
    }

    public int getMaxMismatches() {
        return maxMismatches;
    }

    public int getMaxErrors() {
        return maxErrors;
    }

    public int getMaxBigIndelLength() {
        return maxBigIndelLength;
    }

    public int getMinMatchedBases() {
        return minMatchedBases;
    }

    public int getStrataAfterBest() {
        return strataAfterBest;
    }

    public Range<Integer> getEncodedValues(final byte c) {
        return encoding.get(c);
    }
}
