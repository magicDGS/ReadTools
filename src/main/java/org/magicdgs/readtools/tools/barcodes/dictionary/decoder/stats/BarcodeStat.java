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
 */
package org.magicdgs.readtools.tools.barcodes.dictionary.decoder.stats;

import htsjdk.samtools.metrics.MetricBase;

/**
 * Metrics for barcode detector
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeStat extends MetricBase {

    /**
     * The barcode sequence
     */
    public String SEQUENCE;

    /**
     * The number of barcodes that match
     */
    public int MATCHED;

    /**
     * Average number of mismatches per matched barcode
     */
    public double MEAN_MISMATCH;

    /**
     * Average number of Ns in the sequence
     */
    public double MEAN_N;

    /**
     * The number of barcodes discarded by the maximum number of mismatches
     */
    public int DISCARDED;

    public BarcodeStat(String sequence) {
        this.SEQUENCE = sequence;
        this.MATCHED = 0;
        this.MEAN_MISMATCH = 0;
        this.MEAN_N = 0;
        this.DISCARDED = 0;
    }
}
