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
package org.vetmeduni.utils.fastq;

import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;
import org.vetmeduni.utils.misc.IOUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class with utils for work with quality
 *
 * TODO: implement convert methods
 *
 * @author Daniel Gómez Sánchez
 */
public class QualityUtils {

	private static final byte phredToSangerOffset = (byte) 31;

	private static final byte asciiToSangerOffset = (byte) 64;

	/**
	 * Supported quality formats for this program
	 */
	public static final Set<FastqQualityFormat> SUPPORTED_FORMATS = Collections
		.unmodifiableSet(new HashSet<FastqQualityFormat>() {{
			add(FastqQualityFormat.Illumina);
			add(FastqQualityFormat.Standard);
		}});

	/**
	 * QualityException for errors in Quality Encoding
	 */
	public static class QualityException extends RuntimeException {

		public QualityException() {
		}

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
	 * @deprecated this is only for FASTQ files; use the more general {@link #getFastqQualityFormat}
	 */
	@Deprecated
	public static FastqQualityFormat getEncoding(File file) {
		FastqReader reader = new FastqReader(file);
		FastqQualityFormat format = QualityEncodingDetector.detect(reader);
		if (format.equals(FastqQualityFormat.Solexa)) {
			throw new RuntimeException(format + " format not supported");
		}
		reader.close();
		return format;
	}

	/**
	 * Get the quality encoding for a FASTQ or BAN file
	 *
	 * @param input the file to check
	 *
	 * @return the quality encoding
	 * @throws org.vetmeduni.utils.fastq.QualityUtils.QualityException if the quality is not one of the supported
	 */
	public static FastqQualityFormat getFastqQualityFormat(File input) {
		return getFastqQualityFormat(input, QualityEncodingDetector.DEFAULT_MAX_RECORDS_TO_ITERATE);
	}

	/**
	 * Get the quality encoding for a FASTQ or BAM file
	 *
	 * @param input    the file to check
	 * @param maxReads the maximum number of reads to iterate
	 *
	 * @return the quality encoding
	 * @throws org.vetmeduni.utils.fastq.QualityUtils.QualityException if the quality is not one of the supported
	 */
	public static FastqQualityFormat getFastqQualityFormat(File input, long maxReads) {
		FastqQualityFormat encoding;
		if (IOUtils.isBamOrSam(input)) {
			SAMRecordIterator reader = SamReaderFactory.makeDefault().open(input).iterator();
			encoding = getFastqQualityFormat(reader, maxReads);
			reader.close();
		} else {
			FastqReader reader = new FastqReader(input);
			encoding = getFastqQualityFormat(reader, maxReads);
			reader.close();
		}
		if (!SUPPORTED_FORMATS.contains(encoding)) {
			throw new QualityException(encoding + " format not supported");
		}
		return encoding;
	}

	/**
	 * Get quality format from BAM reader
	 *
	 * @param bamReader the reader for the BAM file
	 * @param maxReads  the maximum number of reads to iterate
	 *
	 * @return the quality encoding
	 */
	private static FastqQualityFormat getFastqQualityFormat(SAMRecordIterator bamReader, long maxReads) {
		return QualityEncodingDetector.detect(maxReads - 1, bamReader);
	}

	/**
	 * Get quality format from FASTQ reader
	 *
	 * @param fastqReader the reader for the FASTQ file
	 * @param maxReads    the maximum number of reads to iterate
	 *
	 * @return the quality encoding
	 */
	private static FastqQualityFormat getFastqQualityFormat(FastqReader fastqReader, long maxReads) {
		return QualityEncodingDetector.detect(maxReads, fastqReader);
	}

	/**
	 * Convert a byte illumina quality to a sanger quality
	 *
	 * @param illuminaQual the quality in illumina encoding
	 *
	 * @return the byte representing the illumina quality
	 */
	public static byte byteToSanger(byte illuminaQual) {
		return (byte) (illuminaQual - phredToSangerOffset);
	}

	/**
	 * Check if the provided encoding is Standard
	 *
	 * @param encoding the encoding
	 *
	 * @return <code>true</code> if is standard; <code>false</code> otherwise
	 */
	public static boolean isStandard(FastqQualityFormat encoding) {
		return encoding.equals(FastqQualityFormat.Standard);
	}
}
