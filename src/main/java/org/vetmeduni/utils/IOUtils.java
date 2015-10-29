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

package org.vetmeduni.utils;

import htsjdk.samtools.BamFileIoUtils;

import java.io.File;

/**
 * Utils for the inputs FASTQ and BAM/SAM files
 *
 * @author Daniel Gómez-Sánchez
 */
public class IOUtils {

	public static final String DEFAULT_SAM_EXTENSION = ".sam";

	public static final String DEFAULT_FQ_EXTENSION = ".fq";

	public static final String DEFAULT_GZIP_EXTENSION = ".gz";


	public static boolean isBamOrSam(File input) {
		return BamFileIoUtils.isBamFile(input) || input.getName().endsWith(DEFAULT_SAM_EXTENSION);
	}

	public static String makeInputFastqWithDefaults(String prefix, boolean gzip) {
		return String.format("%s%s%s", prefix, IOUtils.DEFAULT_FQ_EXTENSION, (gzip) ? IOUtils.DEFAULT_GZIP_EXTENSION : "");
	}

}
