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
package org.vetmeduni.fastq;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;

import java.io.File;

/**
 * Class with utils for work with quality
 *
 * TODO: implement convert methods
 *
 * @author Daniel Gómez Sánchez
 */
public class QualityUtils {

	/**
	 * QualityException for errors in Quality Encoding
	 */
	public static class QualityException extends RuntimeException {

		public QualityException() {
		}

		;

		public QualityException(final String s) {
			super(s);
		}

		public QualityException(final String s, final Throwable throwable) {
			super(s, throwable);
		}

		public QualityException(final Throwable throwable) {
			super(throwable);
		}
	}

	public static int getIlluminaQuality(char qual) {
		return qual - 64;
	}

	public static int getSangerQuality(char qual) {
		return qual - 33;
	}

	public static int getQuality(char qual, FastqQualityFormat format) {
		if (format == FastqQualityFormat.Illumina) {
			return getIlluminaQuality(qual);
		}
		if (format == FastqQualityFormat.Standard) {
			return getSangerQuality(qual);
		}
		throw new QualityException(format + " format not supported");
	}

	/**
	 * Get the quality enconding for a FastqFile
	 *
	 * @param file the filte to get the encoding
	 *
	 * @return the format
	 */
	public static FastqQualityFormat getEncoding(File file) {
		FastqReader reader = new FastqReader(file);
		FastqQualityFormat format = QualityEncodingDetector.detect(reader);
		if (format.equals(FastqQualityFormat.Solexa)) {
			throw new RuntimeException(format + " format not supported");
		}
		reader.close();
		return format;
	}

}
