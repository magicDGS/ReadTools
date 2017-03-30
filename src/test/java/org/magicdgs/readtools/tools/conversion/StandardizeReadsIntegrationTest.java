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

package org.magicdgs.readtools.tools.conversion;

import org.magicdgs.readtools.RTCommandLineProgramTest;

import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class StandardizeReadsIntegrationTest extends RTCommandLineProgramTest {

    private final static File TEST_TEMP_DIR =
            createTestTempDir(StandardizeReadsIntegrationTest.class.getSimpleName());

    private final File expectedPaired = getTestFile("expected_paired_standard.sam");
    private final File expectedSingle = getTestFile("expected_single_standard.sam");

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

    // expected output should be SAM
    @Test(dataProvider = "toStandardize")
    public void tesStandardizeReads(final String name, final ArgumentsBuilder args,
            final File expectedOutput)
            throws Exception {
        // output is always in
        final File output = new File(TEST_TEMP_DIR, name + ".sam");
        // add output and remove from tests the program record
        args.addOutput(output)
                .addBooleanArgument("addOutputSAMProgramRecord", false);
        runCommandLine(args);

        // using text file concordance
        // TODO: maybe we should find a different method
        IntegrationTestSpec.assertEqualTextFiles(output, expectedOutput);
    }

}