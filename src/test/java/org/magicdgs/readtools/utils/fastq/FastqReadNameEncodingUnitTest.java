/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
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

import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqReadNameEncodingUnitTest extends RTBaseTest {

    @DataProvider
    public Object[][] encodingData() throws Exception {
        // data provider order:
        // 1. encoding format (enum)
        // 2. original read name
        // 3. expected plain name
        // 4. first in pair (true/false)
        // 5. second of pair (true/false)
        // 6. PF flag (true/false)
        // 7. barcode sequence (String[])
        return new Object[][] {
                // illumina encodings
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/1",
                        "@HWUSI-EAS100R:6:73:941:1973", true, false, false,
                        new String[] {"ACTG"}},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/2",
                        "@HWUSI-EAS100R:6:73:941:1973", false, true, false,
                        new String[] {"ACTG"}},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG-TTT/1",
                        "@HWUSI-EAS100R:6:73:941:1973", true, false, false,
                        new String[] {"ACTG", "TTT"}},
                // illumina encodings without barcode
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973/1",
                        "@HWUSI-EAS100R:6:73:941:1973", true, false, false,
                        new String[0]},
                // with comments
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973/2 comment",
                        "@HWUSI-EAS100R:6:73:941:1973", false, true, false,
                        new String[0]},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ATCGA comment",
                        "@HWUSI-EAS100R:6:73:941:1973", false, false, false,
                        new String[] {"ATCGA"}},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/2 comment",
                        "@HWUSI-EAS100R:6:73:941:1973", false, true, false,
                        new String[] {"ACTG"}},
                // with information after the comments
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973 comment/2",
                        "@HWUSI-EAS100R:6:73:941:1973", false, true, false,
                        new String[0]},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973 comment#ATCGA",
                        "@HWUSI-EAS100R:6:73:941:1973", false, false, false,
                        new String[] {"ATCGA"}},
                {FastqReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973 comment#ACTG/2",
                        "@HWUSI-EAS100R:6:73:941:1973", false, true, false,
                        new String[] {"ACTG"}},
                // casava formatting
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 1:N:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573", true, false, false,
                        new String[] {"NTGATTAC"}},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 2:N:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573", false, true, false,
                        new String[] {"NTGATTAC"}},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 1:Y:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573", true, false, true,
                        new String[] {"NTGATTAC"}},
                {FastqReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 2:Y:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573", false, true, true,
                        new String[] {"NTGATTAC"}},
                // casava with numbers instead of barcodes
                {FastqReadNameEncoding.CASAVA,
                        "@EAS139:136:FC706VJ:2:2104:15343:197393 1:N:18:1",
                        "@EAS139:136:FC706VJ:2:2104:15343:197393", true, false, false,
                        new String[] {"1"}},
                // first of pair Casava format dual indexing with + separator (found in at least one provider)
                {FastqReadNameEncoding.CASAVA,
                        "@E00514:354:HLH2VCCXY:4:1101:21968:1784 1:N:0:CCCCCCCC+CCCCCCCC",
                        // expected name without space
                        "@E00514:354:HLH2VCCXY:4:1101:21968:1784",
                        // first (true/false) and do not pass control (false)
                        true, false, false,
                        // barcode combined into one - could be modified by the java property
                        new String[]{"CCCCCCCC+CCCCCCCC"}
                },
                // second of pair Casava format dual indexing with + separator (found in at least one provider)
                {FastqReadNameEncoding.CASAVA,
                        "@E00514:354:HLH2VCCXY:4:1101:21968:1784 2:N:0:CCCCCCCC+CCCCCCCC",
                        // expected name without space
                        "@E00514:354:HLH2VCCXY:4:1101:21968:1784",
                        // second (false/true) and do not pass control (false)
                        false, true, false,
                        // barcode combined into one - could be modified by the java property
                        new String[]{"CCCCCCCC+CCCCCCCC"}
                },
                // previous Casava formatting but with hyphen as barcode separator (default)
                {FastqReadNameEncoding.CASAVA,
                        "@E00514:354:HLH2VCCXY:4:1101:21968:1784 2:N:0:CCCCCCCC-CCCCCCCC",
                        // expected name without space
                        "@E00514:354:HLH2VCCXY:4:1101:21968:1784",
                        // second (false/true) and do not pass control (false)
                        false, true, false,
                        // barcode combined into one - could be modified by the java property
                        new String[]{"CCCCCCCC", "CCCCCCCC"}
                },
                // Nanopore name format - see https://github.com/nanopore-wgs-consortium/NA12878
                {FastqReadNameEncoding.ILLUMINA,
                        "@455ce49b-a59e-4c03-8639-5be6272eb928_Basecall_Alignment_template MinION2_20160716_FNFAB23716_MN16454_sequencing_run_Chip86_Human_Genomic_1D_Rapid_Tuned3_99286_ch190_read287_strand1",
                        // expected value without space
                        "@455ce49b-a59e-4c03-8639-5be6272eb928_Basecall_Alignment_template",
                        // no pair-end, PF information
                        false, false, false,
                        // no barcode
                        new String[0]}
        };
    }

    @Test(dataProvider = "encodingData")
    public void testGetPlainName(final FastqReadNameEncoding encoding,
            final String readName, final String expectedPlainName, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.getPlainName(readName), expectedPlainName);
    }

    @Test(dataProvider = "encodingData")
    public void testUpdateReadFromReadName(final FastqReadNameEncoding encoding,
            final String readName, final String expectedPlainName, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(null, null, null);
        FastqReadNameEncoding.updateReadFromReadName(read, readName);
        Assert.assertEquals(read.getName(), expectedPlainName, "incorrect name");
        Assert.assertEquals(read.isPaired(), first || second, "incorrect paired flag");
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
            final String readName, final String expectedPlainName, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.isPF(readName), pf);
    }

    @Test(dataProvider = "encodingData")
    public void testIsFirstOfPair(final FastqReadNameEncoding encoding,
            final String readName, final String expectedPlainName, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.isFirstOfPair(readName), first);
    }

    @Test(dataProvider = "encodingData")
    public void testIsSecondOfPair(final FastqReadNameEncoding encoding,
            final String readName, final String expectedPlainName, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.isSecondOfPair(readName), second);
    }

    @Test(dataProvider = "encodingData")
    public void testGetBarcodes(final FastqReadNameEncoding encoding,
            final String readName, final String expectedPlainName, final boolean first,
            final boolean second, final boolean pf, final String[] barcode) throws Exception {
        Assert.assertEquals(encoding.getBarcodes(readName), barcode);
    }

}