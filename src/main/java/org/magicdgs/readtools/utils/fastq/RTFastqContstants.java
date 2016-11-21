/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.utils.fastq;

/**
 * FASTQ constants for ReadTools.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTFastqContstants {

    /** Cannot be instantiated. */
    private RTFastqContstants() {}

    /** Barcode delimiter in read names for Illumina encoding (e.g. readName#barcode). */
    public static final String ILLUMINA_NAME_BARCODE_DELIMITER = "#";

    // TODO: this will be in HTSJDK FastqConstants after https://github.com/samtools/htsjdk/pull/572
    public static final String FIRST_OF_PAIR = "/1";
    public static final String SECOND_OF_PAIR = "/2";

}
