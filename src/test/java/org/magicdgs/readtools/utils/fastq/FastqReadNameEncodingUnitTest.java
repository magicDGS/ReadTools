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

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.util.SequenceUtil;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqReadNameEncodingUnitTest extends BaseTest {

    @DataProvider
    public Object[][] encodingData() throws Exception {
        return new Object[][] {
                // illumina encodings
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/1",
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/1", true, false, false,
                        new String[] {"ACTG"}},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/2",
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/2", false, true, false,
                        new String[] {"ACTG"}},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG-TTT/1",
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG-TTT/1", true, false, false,
                        new String[] {"ACTG", "TTT"}},
                // illumina encodings without barcode
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973/1",
                        "@HWUSI-EAS100R:6:73:941:1973/1", true, false, false,
                        new String[0]},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973/2 comment",
                        "@HWUSI-EAS100R:6:73:941:1973/2", false, true, false,
                        new String[0]},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ATCGA comment",
                        "@HWUSI-EAS100R:6:73:941:1973#ATCGA", false, false, false,
                        new String[] {"ATCGA"}},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 1:N:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/1", true, false, false,
                        new String[] {"NTGATTAC"}},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 2:N:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/2", false, true, false,
                        new String[] {"NTGATTAC"}},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 1:Y:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/1", true, false, true,
                        new String[] {"NTGATTAC"}},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 2:Y:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/2", false, true, true,
                        new String[] {"NTGATTAC"}},
        };
    }

    // helper for expected normalize
    private String plainNameFromExpectedNormalize(final String expectedNormalize) {
        // the expected normalized should not contain the trailing /1 or /2, neither the barcode
        final int indexOfbarcode = expectedNormalize.indexOf('#');
        return (indexOfbarcode == -1)
                ? SequenceUtil.getSamReadNameFromFastqHeader(expectedNormalize)
                : expectedNormalize.substring(0, indexOfbarcode);
    }

    @Test(dataProvider = "encodingData")
    public void testGetPlainName(final FastqReadNameEncoding encoding,
            final String readName, final String expectedNormalize, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.getPlainName(readName),
                plainNameFromExpectedNormalize(expectedNormalize));
    }

    @Test(dataProvider = "encodingData")
    public void testUpdateReadFromReadName(final FastqReadNameEncoding encoding,
            final String readName, final String expextedNormalize, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(null, null, null);
        FastqReadNameEncoding.updateReadFromReadName(read, readName);
        Assert.assertEquals(read.getName(),
                plainNameFromExpectedNormalize(expextedNormalize), "incorrect name");
        Assert.assertEquals(read.isPaired(), first || second, "incorrec paired flag");
        Assert.assertEquals(read.isFirstOfPair(), first, "incorrect first of pair flag");
        Assert.assertEquals(read.isSecondOfPair(), second, "incorrect second of pair flag");
        Assert.assertEquals(read.failsVendorQualityCheck(), pf, "incorrect PF flag");
        if (barcode.length != 0) {
            Assert.assertEquals(read.getAttributeAsString("BC"), String.join("-", barcode));
        } else {
            Assert.assertNull(read.getAttributeAsString("BC"));
        }

    }

    @Test(dataProvider = "encodingData")
    public void testIsPf(final FastqReadNameEncoding encoding,
            final String readName, final String expextedNormalize, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.isPF(readName), pf);
    }

    @Test(dataProvider = "encodingData")
    public void testIsFirstOfPair(final FastqReadNameEncoding encoding,
            final String readName, final String expextedNormalize, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.isFirstOfPair(readName), first);
    }

    @Test(dataProvider = "encodingData")
    public void testIsSecondOfPair(final FastqReadNameEncoding encoding,
            final String readName, final String expextedNormalize, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.isSecondOfPair(readName), second);
    }

    @Test(dataProvider = "encodingData")
    public void testGetBarcodes(final FastqReadNameEncoding encoding,
            final String readName, final String expextedNormalize, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.getBarcodes(readName), barcode);
    }

}