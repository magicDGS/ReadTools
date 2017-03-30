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

package org.magicdgs.readtools.utils.read;

import org.magicdgs.readtools.BaseTest;

import htsjdk.samtools.SAMTag;
import htsjdk.samtools.fastq.FastqRecord;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqGATKReadUnitTest extends BaseTest {

    @DataProvider(name = "fastqRecordData")
    public Iterator<Object[]> fastqRecordDataProvider() {
        final String baseQualities = "FFGCHI5";
        final String bases = "ACTGTTAG";
        final GATKRead baseRecord = ArtificialReadUtils
                .createArtificialUnmappedRead(null,
                        new byte[] {'A', 'C', 'T', 'G', 'T', 'T', 'A', 'G'},
                        new byte[] {37, 37, 38, 34, 39, 40, 20});
        baseRecord.setName("baseRecord");
        final List<Object[]> data = new ArrayList<>();
        // simple case test
        data.add(new Object[] {new FastqRecord(baseRecord.getName(), bases, null, baseQualities),
                baseRecord.deepCopy()});
        // case with comment information
        baseRecord.setAttribute(SAMTag.CO.name(), "quality comment");
        data.add(new Object[] {
                new FastqRecord(baseRecord.getName(), bases, "quality comment", baseQualities),
                baseRecord.deepCopy()});
        // case with a read name with pair-end information
        baseRecord.setIsSecondOfPair();
        baseRecord.setIsUnmapped();
        data.add(new Object[] {
                new FastqRecord(baseRecord.getName() + "/2", bases, "quality comment",
                        baseQualities),
                baseRecord.deepCopy()});
        // case with read name as CASAVA format
        baseRecord.setName("baseRecord");
        baseRecord.setAttribute("BC", "ATCG");
        data.add(new Object[] {
                new FastqRecord("baseRecord 2:N:3:ATCG", bases, "quality comment", baseQualities),
                baseRecord.deepCopy()});
        // case with PF flag
        baseRecord.setFailsVendorQualityCheck(true);
        data.add(new Object[] {
                new FastqRecord("baseRecord 2:Y:3:ATCG", bases, "quality comment", baseQualities),
                baseRecord.deepCopy()});
        return data.iterator();
    }

    @Test(dataProvider = "fastqRecordData")
    public void testConstructor(final FastqRecord record, final GATKRead expected)
            throws Exception {
        // create the read the read
        final FastqGATKRead fastqRead = new FastqGATKRead(record);
        Assert.assertFalse(fastqRead.hasHeader());
        // test the information which provides from the name
        // this is handled by FastqReaNameEncoding
        Assert.assertEquals(fastqRead.getName(), expected.getName());
        Assert.assertEquals(fastqRead.isSecondOfPair(), expected.isSecondOfPair());
        Assert.assertEquals(fastqRead.failsVendorQualityCheck(),
                expected.failsVendorQualityCheck());
        // check other settings from the FastqGATKRead: bases, qualities
        Assert.assertEquals(fastqRead.getBases(), expected.getBases());
        Assert.assertEquals(fastqRead.getBaseQualities(), expected.getBaseQualities());
        // the comment is not important, but if it is in the FastqGATKRead it should be updated
        Assert.assertEquals(fastqRead.getAttributeAsString("CO"),
                expected.getAttributeAsString("CO"));
        // assert that it is unmapped always
        Assert.assertTrue(fastqRead.isUnmapped());
        // assert that the mate unmapped state is the same
        if (expected.isPaired()) {
            Assert.assertEquals(fastqRead.mateIsUnmapped(), expected.mateIsUnmapped());
        }
        // assert that the BC tag is the same
        Assert.assertEquals(fastqRead.getAttributeAsString("BC"),
                expected.getAttributeAsString("BC"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadConstructorArgs() throws Exception {
        new FastqGATKRead(null);
    }


}