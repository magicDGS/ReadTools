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

package org.magicdgs.readtools.tools.trimming;

import org.magicdgs.readtools.RTCommandLineProgramTest;

import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimReadsIntegrationTest extends RTCommandLineProgramTest {

    // temp directory for all the tests
    private final static File TEST_TEMP_DIR =
            createTestTempDir(TrimReadsIntegrationTest.class.getSimpleName());

    // TODO: this is from concordance with legacy TrimFastq and should be removed eventually
    private static ArgumentsBuilder getRequiredArguments() {
        // input argument name changed -> same as GATK
        // added the argument to do not output the program group for easy checks
        return new ArgumentsBuilder().addInput(SMALL_FASTQ_1)
                .addBooleanArgument("addOutputSAMProgramRecord", false);
    }

    // TODO: this is from concordance with legacy TrimFastq and should be removed eventually
    @DataProvider(name = "TrimmingDataFromTrimFastq")
    public Object[][] getTrimFastqConcordanceTrimmingData() throws Exception {
        return new Object[][] {
                // test default arguments, with both pair-end and single-end data
                {"testTrimmingSingleEndDefaultParameters", getRequiredArguments(),
                        false, false},
                {"testTrimmingPairEndDefaultParameters", getRequiredArguments()
                        .addFileArgument("input2", SMALL_FASTQ_2),
                        true, false},
                // test keep discarded
                {"testTrimmingSingleEndDefaultParameters", getRequiredArguments(),
                        false, true},
                {"testTrimmingPairEndDefaultParameters", getRequiredArguments()
                        .addFileArgument("input2", SMALL_FASTQ_2),
                        true, true},
                // test lower mapping quality
                // parameter name changed
                {"testTrimmingSingleEndLowQualityThreshold", getRequiredArguments()
                        .addArgument("mottQualityThreshold", "18"),
                        false, true},
                // test with length range
                // parameters name changed
                {"testTrimmingSingleEndLengthRange", getRequiredArguments()
                        .addArgument("minReadLength", "60")
                        .addArgument("maxReadLength", "75"),
                        false, true},
                // test discard internal ns
                // with new syntax, they should enabled a ReadFilter for ambiguous bases and fraction 0
                {"testTrimmingSingleEndDiscardInternalN", getRequiredArguments()
                        .addArgument("readFilter", "AmbiguousBaseReadFilter")
                        .addArgument("ambigFilterFrac", "0"),
                        false, true},
                // test trimming Ns, discarding internal Ns and no 5 primer
                {"testTrimmingSingleEndNo5p", getRequiredArguments()
                        .addBooleanArgument("disable5pTrim", true),
                        false, true},
                // test no trimming quality
                // new method for not trimming quality is disable the quality trimming
                {"testTrimmingSingleEndNoQuality", getRequiredArguments()
                        .addArgument("disableTrimmer", "MottQualityTrimmer"),
                        false, false}
        };
    }

    // TODO: this is from concordance with legacy TrimFastq and should be removed eventually
    // expected files were generated with StandardizeReads with the following modifications:
    // - Expected discarded single-en was merged with discarded
    // - Add FT tag to discarded
    // - New metrics files were generated with the current implementation
    @Test(dataProvider = "TrimmingDataFromTrimFastq")
    public void testTrimFastqConcordance(final String testName, final ArgumentsBuilder builder,
            final boolean pairEnd, final boolean keepDiscarded) throws Exception {
        final String testOutputName = testName + ((keepDiscarded) ? "KeepDiscarded" : "");
        log("Running " + testOutputName);
        // gets the output prefix and output name in SAM format for easy checking
        final File outputPrefix = new File(TEST_TEMP_DIR, testOutputName);
        final File outputName = new File(outputPrefix.getAbsolutePath() + ".sam");
        final File discardedOutput = new File(outputPrefix.getAbsolutePath() + "_discarded.sam");
        final File metricsOutput = new File(outputPrefix.getAbsolutePath() + ".metrics");

        // now there is not output prefix but bam output
        final ArgumentsBuilder args = builder
                .addOutput(outputName)
                .addBooleanArgument("keepDiscarded", keepDiscarded);
        // running the command line
        runCommandLine(args);

        // check the metrics file
        metricsFileConcordance(metricsOutput,getTestFile(testName + ".metrics"));

        // TODO: we are checking files as text files, but maybe we shouldn't
        // check the output file
        IntegrationTestSpec.assertEqualTextFiles(outputName, getTestFile(testName + ".sam"));
        // check the discarded output
        if (keepDiscarded) {
            IntegrationTestSpec.assertEqualTextFiles(discardedOutput,
                    getTestFile(testName + "_discarded.sam"));
        } else {
            Assert.assertFalse(discardedOutput.exists());
        }
    }


    @Test
    public void testTrimOnlyNdata() throws Exception {
        // create the output file and test that it does not exists
        final File outputFile = new File(TEST_TEMP_DIR, "testTrimOnlyNdata.sam");
        Assert.assertFalse(outputFile.exists());

        // tun with a very simple fastq file with a read that only contains Ns
        runCommandLine(new ArgumentsBuilder()
                .addInput(getTestFile("onlyN.fq"))
                .addOutput(outputFile));

        // assert that the output file exists and it is empty
        Assert.assertTrue(outputFile.exists());
        assertEmptySamFile(outputFile);
    }
}