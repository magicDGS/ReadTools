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
package org.magicdgs.readtools.utils.record;

import org.magicdgs.readtools.utils.fastq.BarcodeMethods;
import org.magicdgs.readtools.utils.fastq.QualityUtils;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMUtils;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.SequenceUtil;
import htsjdk.samtools.util.StringUtil;

/**
 * Class with utils for SAM records
 *
 * @author Daniel Gómez-Sánchez
 */
public class SAMRecordUtils {

    /**
     * Convert a SAMRecord to a FastqRecord (reverse complement if this flag is set)
     *
     * @param record     the record to convert
     * @param mateNumber the number of the mate to add to the record; <code>null</code> if not
     *                   wanted
     *
     * @return the record converted into fastq
     */
    public static FastqRecord toFastqRecord(SAMRecord record, Integer mateNumber) {
        String seqName = (mateNumber == null) ?
                record.getReadName() :
                String.format("%s/%d", record.getReadName(), mateNumber);
        String readString = record.getReadString();
        String qualityString = record.getBaseQualityString();
        if (record.getReadNegativeStrandFlag()) {
            readString = SequenceUtil.reverseComplement(readString);
            qualityString = StringUtil.reverseString(qualityString);
        }
        return new FastqRecord(seqName, readString, "", qualityString);
    }

    /**
     * Check the flags for two records and assert that one of them have the first of pair and the
     * other the second
     *
     * @param record1 one of the pairs
     * @param record2 second of the pairs
     */
    public static void assertPairedMates(final SAMRecord record1, final SAMRecord record2) {
        if (!(record1.getFirstOfPairFlag() && record2.getSecondOfPairFlag()
                || record2.getFirstOfPairFlag() && record1
                .getSecondOfPairFlag())) {
            throw new SAMException("Illegal mate state: " + record1.getReadName());
        }
    }

    /**
     * Add a barcode to a SAMRecord in the format recordName#barcode, but include previous barcodes
     * that are already in
     * recordName; use {@link #addBarcodeToNameIfAbsent} to check if it is present and don't
     * override or {@link
     * #changeBarcodeInName} for override the barcode
     *
     * @param record  the record to update
     * @param barcode the barcode
     */
    public static void addBarcodeToName(SAMRecord record, String barcode) {
        String recordName = String
                .format("%s%s%s", record.getReadName(), BarcodeMethods.NAME_BARCODE_SEPARATOR,
                        barcode);
        record.setReadName(recordName);
    }

    /**
     * Add a barcode in the name if it is not present
     *
     * @param record  the record to update
     * @param barcode the barcode
     *
     * @return <code>true</code> if the barcode is changed; <code>false</code> otherwise
     */
    public static boolean addBarcodeToNameIfAbsent(SAMRecord record, String barcode) {
        if (record.getReadName().contains(BarcodeMethods.NAME_BARCODE_SEPARATOR)) {
            return false;
        }
        addBarcodeToName(record, barcode);
        return true;
    }

    /**
     * Change the barcode in the name if it is present or set it if not; the mate number is removed
     *
     * @param record  the record to update
     * @param barcode the barcode
     */
    public static void changeBarcodeInName(SAMRecord record, String barcode) {
        record.setReadName(getReadNameWithoutBarcode(record));
        addBarcodeToName(record, barcode);
    }

    /**
     * Update the quality encoding for a record to sanger. Checks if the record is correctly
     * formatted on the fly
     *
     * @param record the record to update
     *
     * @deprecated use {@link #toSanger(SAMRecord, boolean)} instead
     */
    public static void toSanger(final SAMRecord record) {
        try {
            // get the base qualities as ascii bytes
            byte[] qualities = record.getBaseQualityString().getBytes();
            byte[] newQualities = new byte[qualities.length];
            for (int i = 0; i < qualities.length; i++) {
                // it is suppose to be checked here
                newQualities[i] = (byte) SAMUtils
                        .fastqToPhred((char) QualityUtils.byteToSanger(qualities[i]));
            }
            record.setBaseQualities(newQualities);
        } catch (IllegalArgumentException e) {
            throw new QualityUtils.QualityException(e);
        }
    }

    /**
     * Update the quality encoding for a record to sanger. Checks if the record is correctly
     * formatted on the fly
     *
     * @param record the record to update
     */
    public static void toSanger(final SAMRecord record, final boolean allowHighQualities) {
        try {
            // get the base qualities as ascii bytes
            byte[] qualities = record.getBaseQualityString().getBytes();
            byte[] newQualities = new byte[qualities.length];
            for (int i = 0; i < qualities.length; i++) {
                final byte sangerQual = QualityUtils.byteToSanger(qualities[i]);
                QualityUtils.checkStandardEncoding(sangerQual, allowHighQualities);
                newQualities[i] = (byte) SAMUtils.fastqToPhred((char) sangerQual);
            }
            record.setBaseQualities(newQualities);
        } catch (IllegalArgumentException e) {
            throw new QualityUtils.QualityException(e);
        }
    }

    /**
     * Get the barcode in the name from a SAMRecord
     *
     * @param record the record to extract the barcode from
     *
     * @return the barcode without read information; <code>null</code> if no barcode is found
     */
    public static String getBarcodeInName(SAMRecord record) {
        return BarcodeMethods.getOnlyBarcodeFromName(record.getReadName());
    }

    /**
     * Get the barcode(s) in the name from a SAMRecord
     *
     * @param record the record to extract the barcode from
     *
     * @return an array with the barcode(s) without read information; <code>null</code> if no
     * barcode is found
     */
    public static String[] getBarcodesInName(SAMRecord record) {
        return BarcodeMethods.getSeveralBarcodesFromName(record.getReadName());
    }

    /**
     * Get the barcode(s) in the name from a SAMRecord
     *
     * @param record    the record to extract the barcode from
     * @param separator the separator between barcodes
     *
     * @return an array with the barcode(s) without read information; <code>null</code> if no
     * barcode is found
     */
    public static String[] getBarcodesInName(SAMRecord record, String separator) {
        return BarcodeMethods.getSeveralBarcodesFromName(record.getReadName(), separator);
    }

    /**
     * Get the read name for a record without the record
     *
     * @param record the record to extract the name from
     *
     * @return the read name without the barcode information
     */
    public static String getReadNameWithoutBarcode(SAMRecord record) {
        return BarcodeMethods.getNameWithoutBarcode(record.getReadName());
    }
}
