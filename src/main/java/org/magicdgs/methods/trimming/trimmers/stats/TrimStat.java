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
package org.magicdgs.methods.trimming.trimmers.stats;

import htsjdk.samtools.metrics.MetricBase;

/**
 * @author Daniel Gómez-Sánchez
 */
public class TrimStat extends MetricBase {

    public String PAIR;

    public int TOTAL;

    public int PASSED;

    public int POLY_N_TRIMMED;

    public int INTERNAL_N_DISCARDED;

    public int LENGTH_DISCARDED;

    public int QUALITY_TRIMMED;

    public TrimStat(String pair) {
        PAIR = pair;
        TOTAL = 0;
        POLY_N_TRIMMED = 0;
        INTERNAL_N_DISCARDED = 0;
        QUALITY_TRIMMED = 0;
        LENGTH_DISCARDED = 0;
        PASSED = 0;
    }
}
