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
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/1", false, false},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/2",
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/2", true, false},
                // illumina encodings without barcode
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973/1",
                        "@HWUSI-EAS100R:6:73:941:1973/1", false, false},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973/2 comment",
                        "@HWUSI-EAS100R:6:73:941:1973/2", true, false},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ATCGA comment",
                        "@HWUSI-EAS100R:6:73:941:1973#ATCGA", false, false},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 1:N:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/1", false, false},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 2:N:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/2", true, false},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 1:Y:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/1", false, true},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 2:Y:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/2", true, true},
        };
    }

    @Test(dataProvider = "encodingData")
    public void testIlluminaReadNameWithoutComment(final FastqReadNameEncoding encoding,
            final String readName, final String expectedNormalize, final boolean second,
            final boolean pf) throws Exception {
        // the expected normalized should not contain the trailing /1 or /2
        Assert.assertEquals(encoding.getIlluminaReadNameWithoutComment(readName),
                SequenceUtil.getSamReadNameFromFastqHeader(expectedNormalize));
    }

    @Test(dataProvider = "encodingData")
    public void testNormalizeName(final FastqReadNameEncoding encoding,
            final String readName, final String expectedNormalize, final boolean second,
            final boolean pf) throws Exception {
        Assert.assertEquals(encoding.normalizeReadName(readName), expectedNormalize);
    }

    @Test(dataProvider = "encodingData")
    public void testUpdateReadFromReadName(final FastqReadNameEncoding encoding,
            final String readName, final String expextedNormalize, final boolean second,
            final boolean pf) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(null, null, null);
        FastqReadNameEncoding.updateReadFromReadName(read, readName);
        Assert.assertEquals(read.getName(),
                SequenceUtil.getSamReadNameFromFastqHeader(expextedNormalize), "incorrect name");
        Assert.assertEquals(read.isSecondOfPair(), second, "incorrect second of pair flag");
        Assert.assertEquals(read.failsVendorQualityCheck(), pf, "incorrect PF flag");
    }

    @Test(dataProvider = "encodingData")
    public void testIsPf(final FastqReadNameEncoding encoding, final String readName,
            final String expectedNormalize, final boolean second, final boolean pf)
            throws Exception {
        Assert.assertEquals(encoding.isPF(readName), pf);
    }

    @Test(dataProvider = "encodingData")
    public void testIsSecondOfPair(final FastqReadNameEncoding encoding, final String readName,
            final String expectedNormalize, final boolean second, final boolean pf)
            throws Exception {
        Assert.assertEquals(encoding.isSecondOfPair(readName), second);
    }

}