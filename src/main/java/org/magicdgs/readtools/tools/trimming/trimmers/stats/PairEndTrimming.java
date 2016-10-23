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
package org.magicdgs.readtools.tools.trimming.trimmers.stats;

import htsjdk.samtools.metrics.Header;

/**
 * Header for pair-end trimming
 *
 * @author Daniel Gómez-Sánchez
 */
public class PairEndTrimming implements Header {

    /**
     * Read pairs trimmed in pairs
     */
    public int IN_PAIR;

    /**
     * Read pairs trimmed as single
     */
    public int AS_SINGLE;

    @Override
    public void parse(String in) {
        String[] items = in.split("\t");
        items[0] = items[0].replace("Trimmed in pairs: ", "");
        items[1] = items[1].replace("Trimmed as singles: ", "");
        IN_PAIR = Integer.parseInt(items[0]);
        AS_SINGLE = Integer.parseInt(items[1]);
    }

    @Override
    public String toString() {
        return String.format("Trimmed in pairs: %d\tTrimmed as singles: %d", IN_PAIR, AS_SINGLE);
    }
}
