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

package org.magicdgs.readtools.tools.quality;

import org.magicdgs.readtools.utils.tests.CommandLineProgramTest;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Iterator;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class StandardizeReadsIntegrationTest extends CommandLineProgramTest {

    private final File testDir = createTestTempDir(this.getClass().getSimpleName());

    private final File expectedPaired = getTestFile("expected_paired_standard.sam");
    private final File expectedSingle = getTestFile("expected_single_standard.sam");

    @DataProvider(name = "badArgs")
    public Object[][] getBadArguments() {
        final ArgumentsBuilder builder = new ArgumentsBuilder()
                .addInput(getTestFile("small_1.illumina.fq"))
                .addOutput(new File(testDir, "example.bam"));
        return new Object[][] {
                // only quality
                {builder.addArgument("rawBarcodeQualityTag", "B2")
                        .getArgsArray()},
                // different length of arguments
                {builder.addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeQualityTag", "B3")
                        .getArgsArray()},
                {builder.addBooleanArgument("barcodeInReadName", true)
                        .addArgument("rawBarcodeSequenceTags", "B4")
                        .getArgsArray()}
        };
    }

    @Test(dataProvider = "badArgs", expectedExceptions = UserException.CommandLineException.class)
    public void testBadArguments(final String[] args) throws Exception {
        runCommandLine(args);
    }

    @DataProvider(name = "toStandardize")
    public Object[][] tesStandardizeReadsData() {
        return new Object[][] {
                {"StandardizeReads_paired_SAM",
                        new ArgumentsBuilder()
                                .addInput(getTestFile("small.paired.sam"))
                                .addBooleanArgument("interleaved", true)
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2"),
                        expectedPaired},
                {"StandardizeReads_single_SAM",
                        new ArgumentsBuilder()
                                .addInput(getTestFile("small.single.sam"))
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2"),
                        expectedSingle},
                {"StandardizeReads_paired_FASTQ",
                        new ArgumentsBuilder()
                                .addInput(getTestFile("small_1.illumina.fq"))
                                .addFileArgument("input2", getTestFile("small_2.illumina.fq")),
                        expectedPaired},
                {"StandardizeReads_single_FASTQ",
                        new ArgumentsBuilder()
                                .addInput(getTestFile("small_se.illumina.fq")),
                        expectedSingle},
                {"StandardizeReads_single_SAM_names",
                        new ArgumentsBuilder()
                                .addInput(getTestFile("small.single.name.sam"))
                                .addBooleanArgument("barcodeInReadName", true),
                        expectedSingle},
                {"StandardizeReads_single_SAM_quals",
                        new ArgumentsBuilder()
                                .addInput(getTestFile("small.single.quals.sam"))
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2")
                                .addArgument("rawBarcodeQualityTag", "QT")
                                .addArgument("rawBarcodeQualityTag", "Q2"),
                        getTestFile("expected_single_standard_quals.sam")}
        };
    }

    @Test(dataProvider = "toStandardize")
    public void tesStandardizeReads(final String name, final ArgumentsBuilder args,
            final File expectedOutput)
            throws Exception {
        final File output = new File(testDir, name + ".sam");
        args.addOutput(output);
        runCommandLine(args);
        final SamReader actual = SamReaderFactory.makeDefault().open(output);
        final SamReader expected = SamReaderFactory.makeDefault().open(expectedOutput);
        final Iterator<SAMRecord> actualIt = actual.iterator();
        // we ignore the header in this test
        for (final SAMRecord record : expected) {
            Assert.assertTrue(actualIt.hasNext(), "output file has less reads than expected");
            Assert.assertEquals(actualIt.next(), record, "different reads");
        }
        Assert.assertFalse(actualIt.hasNext(), "output file has more reads than expected");
        actual.close();
        expected.close();
    }

}