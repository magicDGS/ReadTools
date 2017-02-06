/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.metrics;

import htsjdk.samtools.metrics.MetricBase;

/**
 * Holds summary statistics for trimming processing.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmerMetric extends MetricBase {

    /** Name of the trimmer applied to the reads. */
    public String TRIMMER = "DEFAULT";

    /** Total number of reads passed to the trimmer. */
    public int TOTAL = 0;

    /** Trimmed reads in the 5 prime end. */
    public int TRIMMED_5_P = 0;

    /** Trimmed reads in the 3 prime end. */
    public int TRIMMED_3_P = 0;

    /** Completely trimmed reads. */
    public int TRIMMED_COMPLETE = 0;

    /** Constructor for default trimmer name. */
    public TrimmerMetric() { }

    /** Constructor for default trimmer name. */
    public TrimmerMetric(final String trimmer) {
        this.TRIMMER = trimmer;
    }
}
