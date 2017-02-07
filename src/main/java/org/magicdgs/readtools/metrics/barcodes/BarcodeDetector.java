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

package org.magicdgs.readtools.metrics.barcodes;

import htsjdk.samtools.metrics.Header;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeDetector implements Header {

    /**
     * Store the number of barcodes discarded because they do not match
     */
    public int DISCARDED_NO_MATCH;

    /**
     * Store the number of barcodes discarded by N
     */
    public int DISCARDED_BY_N;

    /**
     * Store number of barcodes discarded by mismatches
     */
    public int DISCARDED_BY_MISMATCH;

    /**
     * Store the number of barcodes discarded by distance with the second
     */
    public int DISCARDED_BY_DISTANCE;

    @Override
    public void parse(String in) {
        String[] tokens = in.split("\t");
        tokens[0] = tokens[0].replace("No match: ", "");
        tokens[1] = tokens[1].replace("Discarded by N:", "");
        tokens[2] = tokens[2].replace("Discarded by mismatch: ", "");
        tokens[3] = tokens[3].replace("Discarded by distance: ", "");
        DISCARDED_NO_MATCH = Integer.valueOf(tokens[0]);
        DISCARDED_BY_N = Integer.valueOf(tokens[1]);
        DISCARDED_BY_MISMATCH = Integer.valueOf(tokens[2]);
        DISCARDED_BY_DISTANCE = Integer.valueOf(tokens[2]);
    }

    @Override
    public String toString() {
        return String
                .format("No match: %d\tDiscarded by N: %d\tDiscarded by mismatch: %d\tDiscarded by distance: %d",
                        DISCARDED_NO_MATCH, DISCARDED_BY_N, DISCARDED_BY_MISMATCH,
                        DISCARDED_BY_DISTANCE);
    }
}
