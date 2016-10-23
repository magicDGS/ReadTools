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
package org.magicdgs.readtools.utils.fastq;

import org.magicdgs.readtools.utils.misc.IOUtils;

import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class with utils for work with quality
 *
 * @author Daniel Gómez Sánchez
 */
public class QualityUtils {

    /**
     * For all the tools that needs to detect the quality, iterate over 1M reads to be sure that the
     * quality is correct
     */
    public static final long DEFAULT_MAX_RECORDS_TO_DETECT_QUALITY = 1_000_000;

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

    /**
     * Get the integer quality from a character
     *
     * @param qual   the quality
     * @param format in which format is encoded
     *
     * @return the integer value of the encoded quality (phred score)
     */
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
     * Get the quality encoding for a FASTQ or BAN file
     *
     * @param input the file to check
     *
     * @return the quality encoding
     *
     * @throws org.magicdgs.readtools.utils.fastq.QualityUtils.QualityException if the quality is not one of
     *                                                                the supported
     */
    public static FastqQualityFormat getFastqQualityFormat(File input) {
        return getFastqQualityFormat(input, DEFAULT_MAX_RECORDS_TO_DETECT_QUALITY);
    }

    /**
     * Get the quality encoding for a FASTQ or BAM file
     *
     * @param input    the file to check
     * @param maxReads the maximum number of reads to iterate
     *
     * @return the quality encoding
     *
     * @throws org.magicdgs.readtools.utils.fastq.QualityUtils.QualityException if the quality is not one of
     *                                                                the supported
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
    private static FastqQualityFormat getFastqQualityFormat(SAMRecordIterator bamReader,
            long maxReads) {
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
    private static FastqQualityFormat getFastqQualityFormat(FastqReader fastqReader,
            long maxReads) {
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

    /**
     * Check if a base quality is well encoded
     *
     * @param quality                    the quality to check
     * @param encoding                   the encoding
     * @param allowHigherQualitiesSanger allow higher qualities when sanger encoding
     *
     * @throws org.magicdgs.readtools.utils.fastq.QualityUtils.QualityException if the quality is not well
     *                                                                encoded
     */
    public static void checkEncoding(final byte quality, final FastqQualityFormat encoding,
            final boolean allowHigherQualitiesSanger) {
        // there are no qualities smaller than 33
        if (quality < 33) {
            throw new QualityException(
                    "Found " + quality + " (" + (char) quality + ") encoded quality");
        }
        switch (encoding) {
            case Illumina:
                if (quality < 64) {
                    throw new QualityException(
                            "Found " + quality + " (" + (char) quality
                                    + ") in Illumina encoded base");
                }
                break;
            case Standard:
                // it is 74 and not 73 because of Illumina 1.8+
                if (quality > 74 && !allowHigherQualitiesSanger) {
                    throw new QualityException(
                            "Found " + quality + "(" + (char) quality + ") in Sanger encoded base");
                }
                break;
            default:
                throw new QualityException(encoding + " format not supported");
        }
    }

    /**
     * Check if a base quality is well encoded (strict form)
     *
     * @param quality  the quality to check
     * @param encoding the encoding
     *
     * @throws org.magicdgs.readtools.utils.fastq.QualityUtils.QualityException if the quality is not well
     *                                                                encoded
     * @deprecated use {@link #checkEncoding(byte, FastqQualityFormat, boolean)} instead to fine
     * control
     */
    @Deprecated
    public static void checkEncoding(final byte quality, final FastqQualityFormat encoding) {
        checkEncoding(quality, encoding, false);
    }

    /**
     * Check if a base quality is correctly standard encoded
     *
     * @param quality the quality to check
     *
     * @deprecated use {@link #checkStandardEncoding(byte, boolean)}
     */
    @Deprecated
    public static void checkStandardEncoding(byte quality) {
        checkEncoding(quality, FastqQualityFormat.Standard);
    }

    /**
     * Check if a base quality is correctly standard encoded
     *
     * @param quality              the quality to check
     * @param allowHigherQualities allow higher qualities
     */
    public static void checkStandardEncoding(final byte quality,
            final boolean allowHigherQualities) {
        checkEncoding(quality, FastqQualityFormat.Standard, allowHigherQualities);
    }

    /**
     * Check if a several base qualities are well encoded
     *
     * @param qualities the array of qualities to check
     * @param encoding  the encoding
     *
     * @throws org.magicdgs.readtools.utils.fastq.QualityUtils.QualityException if the quality is not well
     *                                                                encoded
     * @deprecated use {@link #checkEncoding(byte, FastqQualityFormat, boolean)}
     */
    @Deprecated
    public static void checkEncoding(byte[] qualities, FastqQualityFormat encoding) {
        for (byte qual : qualities) {
            checkEncoding(qual, encoding);
        }
    }

    /**
     * Check if a several base qualities are well encoded
     *
     * @param qualities                  the array of qualities to check
     * @param encoding                   the encoding
     * @param allowHigherQualitiesSanger if it is sanger encoding
     *
     * @throws org.magicdgs.readtools.utils.fastq.QualityUtils.QualityException if the quality is not well
     *                                                                encoded
     */
    public static void checkEncoding(final byte[] qualities, final FastqQualityFormat encoding,
            final boolean allowHigherQualitiesSanger) {
        for (byte qual : qualities) {
            checkEncoding(qual, encoding, allowHigherQualitiesSanger);
        }
    }

    /**
     * Check if a several base qualities are correctly standard encoded
     *
     * @param qualities the array of qualities to check
     *
     * @deprecated use {@link #checkStandardEncoding(byte[], boolean)}
     */
    @Deprecated
    public static void checkStandardEncoding(byte[] qualities) {
        checkEncoding(qualities, FastqQualityFormat.Standard);
    }

    /**
     * Check if a several base qualities are correctly standard encoded
     *
     * @param qualities            the array of qualities to check
     * @param allowHigherQualities allow higher qualities
     */
    public static void checkStandardEncoding(byte[] qualities, final boolean allowHigherQualities) {
        checkEncoding(qualities, FastqQualityFormat.Standard, allowHigherQualities);
    }
}
