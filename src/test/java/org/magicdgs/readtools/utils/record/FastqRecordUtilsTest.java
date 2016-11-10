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

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.readtools.utils.fastq.QualityUtilsTest;
import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.fastq.FastqRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * @author Daniel Gomez-Sanchez
 */
public class FastqRecordUtilsTest extends BaseTest {

    private FastqRecord illumina1;

    private FastqRecord sanger1;

    private FastqPairedRecord illuminaPaired;


    @BeforeMethod
    public void setUp() throws Exception {
        illumina1 =
                new FastqRecord("Record1#ACGT/1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
                        QualityUtilsTest.illuminaQuality);
        illuminaPaired = new FastqPairedRecord(illumina1,
                new FastqRecord("Record1#ACGT/2", "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT", "",
                        QualityUtilsTest.illuminaQuality));
        sanger1 = new FastqRecord("Record1#ACGT/1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
                QualityUtilsTest.sangerQuality);
    }

    @Test
    public void testCutRecord() throws Exception {
        int length = QualityUtilsTest.illuminaQuality.length();
        int offset = 5;
        // cutting the end
        char[] a = new char[length - offset];
        Arrays.fill(a, 'A');
        final FastqRecord cut1 = FastqRecordUtils.cutRecord(illumina1, 0, length - offset);
        Assert.assertEquals(cut1.length(), length - offset);
        Assert.assertEquals(cut1.getReadString(), new String(a));
        Assert.assertEquals(cut1.getBaseQualityString().charAt(cut1.length() - 1),
                QualityUtilsTest.illuminaQuality.charAt(length - offset - 1));
        Assert.assertEquals(cut1.getBaseQualityString().charAt(0),
                QualityUtilsTest.illuminaQuality.charAt(0));
        // cutting the start
        FastqRecord cut2 = FastqRecordUtils.cutRecord(illumina1, offset, length);
        Assert.assertEquals(cut2.length(), length - offset);
        Assert.assertEquals(cut2.getReadString(), new String(a));
        Assert.assertEquals(cut2.getBaseQualityString().charAt(cut2.length() - 1),
                QualityUtilsTest.illuminaQuality.charAt(length - 1));
        Assert.assertEquals(cut2.getBaseQualityString().charAt(0),
                QualityUtilsTest.illuminaQuality.charAt(offset));
        // cutting both start and end
        final FastqRecord cut3 = FastqRecordUtils.cutRecord(illumina1, offset, length - offset);
        a = Arrays.copyOf(a, length - offset - offset);
        Assert.assertEquals(cut3.length(), length - offset - offset);
        Assert.assertEquals(cut3.getReadString(), new String(a));
        Assert.assertEquals(cut3.getBaseQualityString().charAt(cut3.length() - 1),
                QualityUtilsTest.illuminaQuality.charAt(length - offset - 1));
        Assert.assertEquals(cut3.getBaseQualityString().charAt(0),
                QualityUtilsTest.illuminaQuality.charAt(offset));
        // null cuts
        Assert.assertNull(FastqRecordUtils.cutRecord(illumina1, length, 0));
        Assert.assertNull(FastqRecordUtils.cutRecord(illumina1, 5, 5));
    }

    @Test
    public void testGetBarcodeInName() throws Exception {
        final String barcodeSingle = FastqRecordUtils.getBarcodeInName(sanger1);
        Assert.assertEquals(barcodeSingle, "ACGT");
        final String barcodePaired = FastqRecordUtils.getBarcodeInName(illuminaPaired);
        Assert.assertEquals(barcodePaired, "ACGT");
    }

    @Test
    public void testGetReadNameWithoutBarcode() throws Exception {
        final String readNameSingle = FastqRecordUtils.getReadNameWithoutBarcode(sanger1);
        Assert.assertEquals(readNameSingle, "Record1");
        final String readNamePaired = FastqRecordUtils.getReadNameWithoutBarcode(illuminaPaired);
        Assert.assertEquals(readNamePaired, "Record1");
    }

    @Test
    public void testChangeBarcode() throws Exception {
        FastqRecord singleChanged = FastqRecordUtils.changeBarcodeInSingle(sanger1, "TTTT");
        Assert.assertEquals(singleChanged.getReadHeader(), "Record1#TTTT/0");
        singleChanged = FastqRecordUtils.changeBarcode(sanger1, "AAA", 1);
        Assert.assertEquals(singleChanged.getReadHeader(), "Record1#AAA/1");
        FastqPairedRecord pairedChanged =
                FastqRecordUtils.changeBarcodeInPaired(illuminaPaired, "TTT");
        Assert.assertEquals(pairedChanged.getRecord1().getReadHeader(), "Record1#TTT/1");
        Assert.assertEquals(pairedChanged.getRecord2().getReadHeader(), "Record1#TTT/2");
    }
}