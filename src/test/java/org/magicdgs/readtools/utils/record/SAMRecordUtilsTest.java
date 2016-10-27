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

import org.magicdgs.readtools.utils.fastq.QualityUtilsTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * @author Daniel Gomez-Sanchez
 */
public class SAMRecordUtilsTest {

    private SAMRecord illuminaRecord;

    private SAMRecord sangerRecord;

    private static final SAMFileHeader emptyHeader = new SAMFileHeader();

    public static SAMRecord createSamRecord(String readName, byte base, String quality) {
        SAMRecord record = new SAMRecord(emptyHeader);
        record.setReadName(readName);
        byte[] bases = new byte[quality.length()];
        Arrays.fill(bases, base);
        record.setReadBases(bases);
        record.setBaseQualityString(quality);
        return record;
    }

    @BeforeMethod
    public void setUp() throws Exception {
        illuminaRecord =
                createSamRecord("Read1#ACTG/1", (byte) 'A', QualityUtilsTest.illuminaQuality);
        sangerRecord = createSamRecord("Read1#ACTG/1", (byte) 'A', QualityUtilsTest.sangerQuality);
    }

    @Test
    public void testToFastqRecord() throws Exception {
        // create a sequence with the length
        final byte[] bases = new byte[QualityUtilsTest.illuminaQuality.length()];
        Arrays.fill(bases, (byte) 'A');
        final FastqRecord illuminaFastq = new FastqRecord("Read1#ACTG/1", new String(bases), "",
                QualityUtilsTest.illuminaQuality);
        final FastqRecord converted = SAMRecordUtils.toFastqRecord(illuminaRecord, null);
        Assert.assertEquals(converted, illuminaFastq);
    }

    @Test
    public void testAddOChangeBarcode() throws Exception {
        // does not change the barcode name if present
        Assert.assertFalse(SAMRecordUtils.addBarcodeToNameIfAbsent(illuminaRecord, "TTT"));
        Assert.assertEquals(illuminaRecord.getReadName(), "Read1#ACTG/1");
        // change the barcode
        SAMRecordUtils.changeBarcodeInName(illuminaRecord, "TTT");
        Assert.assertEquals(illuminaRecord.getReadName(), "Read1#TTT");
        // adding a new name
        illuminaRecord.setReadName("NewName");
        SAMRecordUtils.addBarcodeToName(illuminaRecord, "ACTG");
        Assert.assertEquals(illuminaRecord.getReadName(), "NewName#ACTG");
        // removing the name and adding if absent
        illuminaRecord.setReadName("SecondName");
        Assert.assertTrue(SAMRecordUtils.addBarcodeToNameIfAbsent(illuminaRecord, "TTT"));
        Assert.assertEquals(illuminaRecord.getReadName(), "SecondName#TTT");
    }

    @Test
    public void testToSanger() throws Exception {
        SAMRecordUtils.toSanger(illuminaRecord, false);
        Assert.assertEquals(illuminaRecord, sangerRecord);
    }

    @Test
    public void testGetBarcodeInName() throws Exception {
        final String barcode = SAMRecordUtils.getBarcodeInName(sangerRecord);
        Assert.assertEquals(barcode, "ACTG");
        // assert that the original is not changed
        Assert.assertEquals(
                createSamRecord("Read1#ACTG/1", (byte) 'A', QualityUtilsTest.sangerQuality),
                sangerRecord);
    }

    @Test
    public void testGetBarcodesInName() throws Exception {
        final String[] barcode = SAMRecordUtils.getBarcodesInName(sangerRecord);
        Assert.assertEquals(barcode, new String[] {"ACTG"});
        // assert that the original is not changed
        Assert.assertEquals(
                createSamRecord("Read1#ACTG/1", (byte) 'A', QualityUtilsTest.sangerQuality),
                sangerRecord);
        final String[] doubleBarcode = SAMRecordUtils
                .getBarcodesInName(createSamRecord("Read1#ACTG_TTTT/1", (byte) 'A',
                        QualityUtilsTest.sangerQuality));
        Assert.assertEquals(doubleBarcode, new String[] {"ACTG", "TTTT"});
    }

    @Test
    public void testGetReadNameWithoutBarcode() throws Exception {
        String readName = SAMRecordUtils.getReadNameWithoutBarcode(illuminaRecord);
        Assert.assertEquals(readName, "Read1");
        // assert that the original is not changed
        Assert.assertEquals(
                createSamRecord("Read1#ACTG/1", (byte) 'A', QualityUtilsTest.illuminaQuality),
                illuminaRecord);
    }
}