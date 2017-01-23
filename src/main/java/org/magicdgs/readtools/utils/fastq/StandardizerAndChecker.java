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
 * SOFTWARE.
 */
package org.magicdgs.readtools.utils.fastq;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.readtools.RTDefaults;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMUtils;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.hellbender.exceptions.GATKException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to randomly check qualities in the records that are input
 *
 * @author Daniel Gómez-Sánchez
 */
public class StandardizerAndChecker {

    // each 1000 reads the quality will be checked
    protected static final int frequency = RTDefaults.SAMPLING_QUALITY_CHECKING_FREQUENCY;

    // The encoding for this checker
    private final FastqQualityFormat encoding;

    // the number of records that passed by this count
    protected final AtomicInteger count;

    // if higher qualities are allowed
    protected final boolean allowHighQualities;

    /**
     * Default constructor
     *
     * @param encoding           the encoding associated with the detector
     * @param allowHighQualities should higher qualities throw an error?
     */
    public StandardizerAndChecker(final FastqQualityFormat encoding,
            final boolean allowHighQualities) {
        this.encoding = encoding;
        this.count = new AtomicInteger();
        this.allowHighQualities = allowHighQualities;
    }

    /**
     * Get the encoding for the checker
     *
     * @return the underlying encoding
     */
    public FastqQualityFormat getEncoding() {
        return encoding;
    }

    /**
     * If by sampling is time to check, check the quality of the record. Null records are ignored
     *
     * @param record the record to check
     *
     * @throws QualityUtils.QualityException if the quality is checked and
     *                                       misencoded
     */
    public void checkMisencoded(final FastqRecord record) {
        try {
            if (record != null && count.incrementAndGet() >= frequency) {
                count.set(0);
                checkMisencoded((Object) record);
            }
        } catch (QualityUtils.QualityException e) {
            throw new SAMException("Wrongly formatted quality string for " +
                    record.getReadHeader() + ": " + record.getBaseQualityString());
        }
    }

    /**
     * If by sampling is time to check, check the quality of the record. Null records are ignored
     *
     * @param record the record to check
     *
     * @throws QualityUtils.QualityException if the quality is checked and
     *                                       misencoded
     */
    public void checkMisencoded(final FastqPairedRecord record) {
        try {
            if (record != null && count.incrementAndGet() >= frequency) {
                count.set(0);
                checkMisencoded((Object) record.getRecord1());
                checkMisencoded((Object) record.getRecord2());
            }
        } catch (QualityUtils.QualityException e) {
            throw new SAMException("Wrongly formatted quality string for paired-record" +
                    record.getRecord1().getReadHeader() + ": " + record.getRecord1()
                    .getBaseQualityString() + " and "
                    + record.getRecord2().getBaseQualityString());
        }
    }

    /**
     * If by sampling is time to check, check the quality of the record. Null records are ignored
     *
     * @param record the record to check
     *
     * @throws QualityUtils.QualityException if the quality is checked and
     *                                       misencoded
     */
    public void checkMisencoded(final SAMRecord record) {
        try {
            if (record != null && count.incrementAndGet() >= frequency) {
                count.set(0);
                checkMisencoded((Object) record);
            }
        } catch (QualityUtils.QualityException e) {
            throw new SAMException("Wrongly formatted quality string for " +
                    record.getReadName() + ": " + record.getBaseQualityString());
        }
    }

    /**
     * Check an object (instance of SAMRecord, FastqRecord or FastqPairedRecord)
     *
     * @param record the record to check
     */
    protected void checkMisencoded(final Object record) {
        final byte[] quals;
        if (record instanceof SAMRecord) {
            quals = ((SAMRecord) record).getBaseQualityString().getBytes();
        } else if (record instanceof FastqRecord) {
            quals = ((FastqRecord) record).getBaseQualityString().getBytes();
        } else {
            throw new IllegalArgumentException(
                    "checkMisencoded only accepts FastqRecord/SAMRecord");
        }
        QualityUtils.checkEncoding(quals, encoding, allowHighQualities);
    }

    /**
     * Standardize a record, checking at the same time the quality
     *
     * @param record the record to standardize
     *
     * @return a new record with the standard encoding or the same if the encoder is sanger;
     * <code>null</code> if the
     * argument is null
     *
     * @throws QualityUtils.QualityException if the conversion causes a
     *                                       misencoded quality
     */
    public FastqRecord standardize(final FastqRecord record) {
        if (record == null) {
            return record;
        }
        try {
            if (QualityUtils.isStandard(encoding)) {
                checkMisencoded(record);
                return record;
            }
            byte[] asciiQualities = record.getBaseQualityString().getBytes();
            byte[] newQualities = new byte[asciiQualities.length];
            for (int i = 0; i < asciiQualities.length; i++) {
                newQualities[i] = QualityUtils.byteToSanger(asciiQualities[i]);
                QualityUtils.checkStandardEncoding(newQualities[i], allowHighQualities);
            }
            return new FastqRecord(record.getReadHeader(), record.getReadString(),
                    record.getBaseQualityHeader(),
                    new String(newQualities));
        } catch (QualityUtils.QualityException e) {
            throw new SAMException("Wrongly formatted quality string for " +
                    record.getReadHeader() + ": " + record.getBaseQualityString());
        }
    }

    /**
     * Standardize a record, checking at the same time the quality
     *
     * @param record the record to standardize
     *
     * @return a new record with the standard encoding or the same if the encoder is sanger;
     * <code>null</code> if the
     * argument is null
     *
     * @throws QualityUtils.QualityException if the conversion causes a
     *                                       misencoded quality
     */
    public FastqPairedRecord standardize(final FastqPairedRecord record) {
        if (record == null) {
            return record;
        }
        FastqRecord record1 = standardize(record.getRecord1());
        FastqRecord record2 = standardize(record.getRecord2());
        return new FastqPairedRecord(record1, record2);
    }

    /**
     * Standardize a record, checking at the same time the quality
     *
     * @param record the record to standardize
     *
     * @return a new record with the standard encoding or the same if the encoder is sanger;
     * <code>null</code> if the
     * argument is null
     *
     * @throws QualityUtils.QualityException if the conversion causes a
     *                                       misencoded quality
     */
    public SAMRecord standardize(final SAMRecord record) {
        if (record == null) {
            return record;
        }
        try {
            if (QualityUtils.isStandard(encoding)) {
                checkMisencoded(record);
                return record;
            }
            SAMRecord newRecord = (SAMRecord) record.clone();
            // relies on the checking of the record
            toSanger(newRecord, allowHighQualities);
            return newRecord;
        } catch (CloneNotSupportedException e) {
            // This should not happen, because it is suppose to be quality
            throw new GATKException.ShouldNeverReachHereException(e);
        } catch (QualityUtils.QualityException e) {
            throw new SAMException("Wrongly formatted quality string for " +
                    record.getReadName() + ": " + record.getBaseQualityString());
        }
    }

    /**
     * Update the quality encoding for a record to sanger. Checks if the record is correctly
     * formatted on the fly
     *
     * @param record the record to update
     */
    @VisibleForTesting
    static void toSanger(final SAMRecord record, final boolean allowHighQualities) {
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
}
