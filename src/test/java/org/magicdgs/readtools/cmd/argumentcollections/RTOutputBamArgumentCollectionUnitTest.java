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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.exceptions.RTUserExceptions;
import org.magicdgs.readtools.BaseTest;

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

    @Test(dataProvider = "badOutputNames", expectedExceptions = RTUserExceptions.InvalidOutputFormat.class)
    public void testIllegalOutputName(final String outputName) throws Exception {
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = outputName;
        args.outputWriter(new SAMFileHeader(), null, true, null);
    }

    @Test(expectedExceptions = UserException.MissingReference.class)
    public void testCramOutputWithoutReference() throws Exception {
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = "example.cram";
        args.outputWriter(new SAMFileHeader(), () -> new SAMProgramRecord("ID"), true, null);
    }

    @DataProvider(name = "outputWriterProvider")
    public Object[][] getOutputWriterData() {
        final File testDir = createTestTempDir(this.getClass().getSimpleName());
        final SAMProgramRecord record = new SAMProgramRecord("test");
        record.setCommandLine("command line");
        return new Object[][] {
                // TODO: test cram
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

    @Test(dataProvider = "outputWriterProvider")
    public void testWritingHeader(final File outputFile, final SAMProgramRecord record,
            final boolean addProgramGroup) throws Exception {
        Assert.assertFalse(outputFile.exists(),
                "broken test: test output file exists " + outputFile);
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = outputFile.getAbsolutePath();
        args.addOutputSAMProgramRecord = addProgramGroup;
        final GATKReadWriter writer =
                args.outputWriter(new SAMFileHeader(), (record == null) ? null : () -> record, true,
                        null
                );
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

    @DataProvider
    public Object[][] outputWithSuffix() throws Exception {
        return new Object[][] {
                {"example.bam", ".empty", "example.empty.bam"},
                {"example.sam", "_suffix", "example_suffix.sam"},
                {"example.otherSuffix.cram", ".newSuffix", "example.otherSuffix.newSuffix.cram"}
        };
    }

    @Test(dataProvider = "outputWithSuffix")
    public void testGetOutputNameWithSuffix(final String outputName, final String suffix,
            final String expectedOutputName) throws Exception {
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = outputName;
        Assert.assertEquals(args.getOutputNameWithSuffix(suffix), expectedOutputName);
    }

    @Test(dataProvider = "badOutputNames", expectedExceptions = RTUserExceptions.InvalidOutputFormat.class)
    public void testIllegalOutputNameWithSuffix(final String outputName) throws Exception {
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = outputName;
        args.getOutputNameWithSuffix("_wrong");
    }

    @DataProvider
    public Object[][] getMetricsNames() {
        return new Object[][] {
                {"example.bam", null, new File("example.metrics")},
                {"example.2.sam", null, new File("example.2.metrics")},
                {"example.cram", "", new File("example.metrics")},
                {"example.bam", "_suffix", new File("example_suffix.metrics")}
        };
    }

    @Test(dataProvider = "getMetricsNames")
    public void testMakeMetricsFile(final String outputName, final String suffix,
            final File expectedMetricsFile) throws Exception {
        final RTOutputBamArgumentCollection args = new RTOutputBamArgumentCollection();
        args.outputName = outputName;
        Assert.assertEquals(args.makeMetricsFile(suffix).toFile(), expectedMetricsFile);
    }
}