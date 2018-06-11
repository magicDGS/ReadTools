/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

import org.magicdgs.readtools.utils.read.RTReadUtils;

import htsjdk.samtools.SAMRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class RTFastqCodec {

    private RTFastqCodec() {}

    private static Logger logger = LogManager.getLogger(RTFastqCodec.class);

    /**
     * Detects the format for the read name provided, and updates the read information.
     *
     * <p>The following information will be updated:
     * <ul>
     * <li>Read name according to SAM specs (no barcode or pair-end information), without
     * white-space.</li>
     * <li>Pair-end information in the bitwise flag (using {@link FastqReadNameEncoding#getPairedState(String)}).</li>
     * <li>PF information in the bitwise flag (using {@link FastqReadNameEncoding#isPF(String)}).</li>
     * <li>Barcode information in the default tag (using {@link FastqReadNameEncoding#getBarcodes(String)}).</li>
     * </ul>
     *
     * @param read     the read to update.
     * @param readName the read name from a FASTQ file.
     */
    public static void updateReadFromReadName(final GATKRead read, final String readName) {
        // gets the firt encoding that match, in the order of the enum
        final FastqReadNameEncoding encoding = FastqReadNameEncoding.detectReadNameEncoding(readName).orElse(null);
        if (encoding == null) {
            throw new GATKException.ShouldNeverReachHereException("Encoding should not be null.");
        } else {
            logger.debug("Detected encoding: {}", encoding);
            read.setName(encoding.getPlainName(readName));
            encoding.getPairedState(readName).setFlags(read);
            read.setFailsVendorQualityCheck(encoding.isPF(readName));
            RTReadUtils.addBarcodesTagToRead(read, encoding.getBarcodes(readName));
        }
    }

    /**
     * Detects the format for the read name provided, and updates the read information.
     *
     * <p>The following information will be updated:
     * <ul>
     * <li>Read name according to SAM specs (no barcode or pair-end information), without
     * white-space.</li>
     * <li>Pair-end information in the bitwise flag (using {@link FastqReadNameEncoding#getPairedState(String)}).</li>
     * <li>PF information in the bitwise flag (using {@link FastqReadNameEncoding#isPF(String)}).</li>
     * <li>Barcode information in the default tag (using {@link FastqReadNameEncoding#getBarcodes(String)}).</li>
     * </ul>
     *
     * @param record   the read to update.
     * @param readName the read name from a FASTQ file.
     */
    public static void updateReadFromReadName(final SAMRecord record, final String readName) {
        // gets the firt encoding that match, in the order of the enum
        final FastqReadNameEncoding encoding = FastqReadNameEncoding.detectReadNameEncoding(readName).orElse(null);
        if (encoding == null) {
            throw new GATKException.ShouldNeverReachHereException("Encoding should not be null.");
        } else {
            logger.debug("Detected encoding: {}", encoding);
            record.setReadName(encoding.getPlainName(readName));
            encoding.getPairedState(readName).setFlags(record);
            record.setReadFailsVendorQualityCheckFlag(encoding.isPF(readName));
            // TODO: this is not yet implemented for SAMRecords!
            // TODO: maybe we should add a class for Barcodes!
            // RTReadUtils.addBarcodesTagToRead(read, encoding.getBarcodes(readName));
        }
    }
}
