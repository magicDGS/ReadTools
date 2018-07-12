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

import avro.shaded.com.google.common.annotations.Beta;
import org.apache.commons.lang3.Range;

import java.util.Map;

/**
 * Header from the gem-mappability file format.
 *
 * <p>NOTE: this class is a component of a beta feature.
 *
 * <p>Includes information about:
 *
 * <ol>
 *     <li>Parameters used for the gem-mapper/mappability algorithm.</li>
 *     <li>Character-encoding for the values of mappability (range of number of mappings)</li>
 * </ol>
 *
 * <p>This class expose getters for the parameters and {@link Range<Integer>} represented by a
 * concree character.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@Beta
public class GemMappabilityHeader {

    // information contained in the header (algorithm params)
    private final int kmerLength;
    private final int approximationThreshold;
    private final int maxMismatches;
    private final int maxErrors;
    private final int maxBigIndelLength;
    private final int minMatchedBases;
    private final int strataAfterBest;

    // map of byte to range of integers
    private final Map<Byte, Range<Integer>> encoding;

    /**
     * Default constructor.
     *
     * @param kmerLength                read/k-mer length (mappability argument).
     * @param approximationThreshold    approximation threshold (mappability argument).
     * @param maxMismatches             maximum mismatches (mapper argument).
     * @param maxErrors                 maximum errors (mapper argument).
     * @param maxBigIndelLength         maximum indel length (mapper argument).
     * @param minMatchedBases           minimum matched bases (mapper argument).
     * @param strataAfterBest           strata after best (mapper argument).
     * @param encoding                  mapping of encodings to integer range.
     */
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

    /**
     * Gets the k-mer length.
     *
     * @return length of the k-mer/read.
     */
    public int getKmerLength() {
        return kmerLength;
    }

    /**
     * Gets the approximation threshold.
     *
     * @return approximation threshold.
     */
    public int getApproximationThreshold() {
        return approximationThreshold;
    }

    /**
     * Gets the maximum mismatches.
     *
     * @return maximum mismatches.
     */
    public int getMaxMismatches() {
        return maxMismatches;
    }

    /**
     * Gets the maximum errors.
     *
     * @return maximum errors.
     */
    public int getMaxErrors() {
        return maxErrors;
    }

    /**
     * Gets the maximum indel length.
     *
     * @return maximum indel length.
     */
    public int getMaxBigIndelLength() {
        return maxBigIndelLength;
    }

    /**
     * Gets the minimum matched bases.
     *
     * @return minimum matched bases.
     */
    public int getMinMatchedBases() {
        return minMatchedBases;
    }

    /**
     * Gets the strata after best.
     *
     * @return strata after best.
     */
    public int getStrataAfterBest() {
        return strataAfterBest;
    }

    /**
     * Gets the range of mappings for a concrete character.
     *
     * @param c character to convert to a range.
     * @return the range of mappings for the character; {@code null} if not present.
     */
    public Range<Integer> getEncodedValues(final byte c) {
        return encoding.get(c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GemMappabilityHeader)) {
            return false;
        }

        GemMappabilityHeader that = (GemMappabilityHeader) o;

        if (kmerLength != that.kmerLength) {
            return false;
        }
        if (approximationThreshold != that.approximationThreshold) {
            return false;
        }
        if (maxMismatches != that.maxMismatches) {
            return false;
        }
        if (maxErrors != that.maxErrors) {
            return false;
        }
        if (maxBigIndelLength != that.maxBigIndelLength) {
            return false;
        }
        if (minMatchedBases != that.minMatchedBases) {
            return false;
        }
        if (strataAfterBest != that.strataAfterBest) {
            return false;
        }

        return encoding.equals(that.encoding);
    }

    @Override
    public int hashCode() {
        int result = kmerLength;
        result = 31 * result + approximationThreshold;
        result = 31 * result + maxMismatches;
        result = 31 * result + maxErrors;
        result = 31 * result + maxBigIndelLength;
        result = 31 * result + minMatchedBases;
        result = 31 * result + strataAfterBest;
        result = 31 * result + encoding.hashCode();
        return result;
    }
}
