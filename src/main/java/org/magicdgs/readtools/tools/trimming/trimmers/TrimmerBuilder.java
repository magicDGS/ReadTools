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
package org.magicdgs.readtools.tools.trimming.trimmers;

/**
 * Trimmer builder for both paired-single end with default parameters
 *
 * @author Daniel Gómez-Sánchez
 */
public class TrimmerBuilder {

    private final boolean single;

    private boolean trimQuality = false;

    private int qualThreshold = 0;

    private int minLength = 0;

    private int maxLength = Integer.MAX_VALUE;

    private boolean discardRemainingNs = false;

    private boolean no5ptrim = false;

    /**
     * Construct a trimmer builder for single-end or paired-end data
     *
     * @param single <code>true</code> if single-end; <code>false</code> otherwise
     */
    public TrimmerBuilder(final boolean single) {
        this.single = single;
    }

    /**
     *
     * @param trimQuality
     * @return
     */
    public TrimmerBuilder setTrimQuality(final boolean trimQuality) {
        this.trimQuality = trimQuality;
        return this;
    }

    /**
     *
     * @param qualThreshold
     * @return
     */
    public TrimmerBuilder setQualityThreshold(final int qualThreshold) {
        this.qualThreshold = qualThreshold;
        return this;
    }

    /**
     *
     * @param minLength
     * @return
     */
    public TrimmerBuilder setMinLength(final int minLength) {
        this.minLength = minLength;
        return this;
    }

    /**
     *
     * @param maxLength
     * @return
     */
    public TrimmerBuilder setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    /**
     *
     * @param discardRemainingNs
     * @return
     */
    public TrimmerBuilder setDiscardRemainingNs(final boolean discardRemainingNs) {
        this.discardRemainingNs = discardRemainingNs;
        return this;
    }

    /**
     *
     * @param no5ptrim
     * @return
     */
    public TrimmerBuilder setNo5pTrimming(final boolean no5ptrim) {
        this.no5ptrim = no5ptrim;
        return this;
    }

    /**
     * Build a trimmer
     *
     * @return the trimmer (either single or paired)
     */
    public Trimmer build() {
        if (single) {
            return new TrimmerSingle(trimQuality, qualThreshold, minLength, maxLength,
                    discardRemainingNs, no5ptrim);
        } else {
            return new TrimmerPaired(trimQuality, qualThreshold, minLength, maxLength,
                    discardRemainingNs, no5ptrim);
        }
    }

}
