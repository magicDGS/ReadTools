/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SamReaderFactory;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTOutputBamArgumentCollectionUnitTest extends BaseTest {

    @DataProvider(name = "badOutputNames")
    public Object[][] getBadOutputNames() {
        return new Object[][] {
                {"example"},
                {"example.bam.bai"},
                {"example.fastq"},
                {"example.fq.gz"}
        };
    }

    @Test(dataProvider = "badOutputNames", expectedExceptions = UserException.CouldNotCreateOutputFile.class)
    public void testIllegalOutputName(final String outputName) throws Exception {
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = outputName;
        args.outputWriter(null, new SAMFileHeader(), true, null);
    }

    @Test(expectedExceptions = UserException.MissingReference.class)
    public void testCramOutputWithoutReference() throws Exception {
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = "example.cram";
        args.outputWriter(null, new SAMFileHeader(), true, () -> new SAMProgramRecord("ID"));
    }

    @DataProvider(name = "outptuWriterProvider")
    public Object[][] getOutputWriterData() {
        final File testDir = createTestTempDir(this.getClass().getSimpleName());
        final SAMProgramRecord record = new SAMProgramRecord("test");
        record.setCommandLine("command line");
        return new Object[][] {
                // test cram
                // {new File(testDir, "example.empty.cram"), null},
                // {new File(testDir, "example.cram"), record},
                // test bam
                {new File(testDir, "example.empty.bam"), null, false},
                {new File(testDir, "example.empty2.bam"), null, true},
                {new File(testDir, "example.bam"), record, false},
                {new File(testDir, "example2.bam"), record, true},
                // test sam
                {new File(testDir, "example.empty.sam"), null, false},
                {new File(testDir, "example.empty2.sam"), null, true},
                {new File(testDir, "example.sam"), record, false},
                {new File(testDir, "example2.sam"), record, true},
        };
    }

    @Test(dataProvider = "outptuWriterProvider")
    public void testWritingHeader(final File outputFile, final SAMProgramRecord record,
            final boolean addProgramGroup) throws Exception {
        Assert.assertFalse(outputFile.exists(), "broken test: test output file exists " + outputFile);
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = outputFile.getAbsolutePath();
        args.addOutputSAMProgramRecord = addProgramGroup;
        final GATKReadWriter writer =
                args.outputWriter(null, new SAMFileHeader(), true,
                        (record == null) ? null : () -> record);
        writer.close();
        Assert.assertTrue(outputFile.exists(), "not output written");
        final SAMFileHeader writtenHeader =
                SamReaderFactory.makeDefault().getFileHeader(outputFile);
        final SAMFileHeader expectedHeader = new SAMFileHeader();
        expectedHeader.setSortOrder(SAMFileHeader.SortOrder.unsorted);
        if (addProgramGroup && record != null) {
            expectedHeader.addProgramRecord(record);
        }
        Assert.assertEquals(writtenHeader, expectedHeader);
    }


}