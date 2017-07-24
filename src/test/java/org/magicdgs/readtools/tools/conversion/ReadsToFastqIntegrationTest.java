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

import org.magicdgs.readtools.RTBaseTest;
import org.magicdgs.readtools.RTCommandLineProgramTest;
import org.magicdgs.readtools.TestResourcesUtils;

import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadsToFastqIntegrationTest extends RTCommandLineProgramTest {

    private static final File TEST_TEMP_DIR =
            createTestTempDir(ReadsToFastqIntegrationTest.class.getSimpleName());

    // this is separated from the rest because it will have special treatment in the future
    @Test
    public void testMappedSortFile() throws Exception {
        final File outputPrefix = new File(TEST_TEMP_DIR, "testMappedSortFile");
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .addInput(getTestFile("mapped.sort.sam"))
                .addOutput(outputPrefix);
        runCommandLine(args);
        assertFileIsEmpty(new File(outputPrefix.getAbsolutePath() + "_SE.fq.gz"));
        testFiles(Arrays.asList(
                new File(outputPrefix.getAbsolutePath() + "_1.fq.gz"),
                new File(outputPrefix.getAbsolutePath() + "_2.fq.gz")),
                Arrays.asList(
                        getTestFile("expected_sort_mapped_1.fq"),
                        getTestFile("expected_sort_mapped_2.fq")));
    }

    @DataProvider
    public Object[][] readSources() {
        return new Object[][] {
                // SAM standardized
                {"test_SAM_single_standard",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.SE.sam")),
                        Collections.singletonList(TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.SE.fq")),
                        false},
                {"test_SAM_paired_standard",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.paired.sam"))
                                .addBooleanArgument("interleaved", true),
                        Arrays.asList(
                                TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired_1.fq"),
                                TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired_2.fq")),
                        true},
                // illumina FASTQ
                {"test_FASTQ_single_standard",
                        new ArgumentsBuilder()
                                .addArgument("rawBarcodeSequenceTags", "null")
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.illumina_quality.SE.fq")),
                        Collections.singletonList(TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.SE.fq")),
                        false},
                {"test_FASTQ_paired_standard",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.illumina_quality_1.fq"))
                                .addFileArgument("input2", TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.illumina_quality_2.fq")),
                        Arrays.asList(
                                TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.paired_1.fq"),
                                TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.paired_2.fq")),
                        true},
                // test no standard SAMs
                {"test_SAM_paired_2_tags",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.paired.sam"))
                                .addBooleanArgument("interleaved", true)
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2"),
                        Arrays.asList(
                                TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired_1.fq"),
                                TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired_2.fq")),
                        true},
                {"test_SAM_paired_read_names",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_read_name.dual_index.paired.sam"))
                                .addBooleanArgument("interleaved", true)
                                .addBooleanArgument("barcodeInReadName", true),
                        Arrays.asList(
                                TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired_1.fq"),
                                TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired_2.fq")),
                        true},
        };
    }

    @Test(dataProvider = "readSources")
    public void testReadsToFastq(final String testName, final ArgumentsBuilder args,
            final List<File> expectedFiles, final boolean paired) {
        final File outputName = new File(TEST_TEMP_DIR, testName);
        args.addOutput(outputName);
        final List<File> pairedFiles = Arrays.asList(
                new File(TEST_TEMP_DIR, testName + "_1.fq.gz"),
                new File(TEST_TEMP_DIR, testName + "_2.fq.gz"));
        final File singleEndFile = new File(TEST_TEMP_DIR, testName + "_SE.fq.gz");
        log("Running " + testName);
        runCommandLine(args);
        if (paired) {
            testFiles(pairedFiles, expectedFiles);
            assertFileIsEmpty(singleEndFile);
        } else {
            testFiles(Collections.singletonList(singleEndFile), expectedFiles);
            pairedFiles.forEach(RTBaseTest::assertFileIsEmpty);
        }
    }

    private void testFiles(final List<File> actualFiles, final List<File> expectedFiles) {
        try {
            for (int i = 0; i < expectedFiles.size(); i++) {
                IntegrationTestSpec.assertEqualTextFiles(actualFiles.get(i), expectedFiles.get(i));
            }
        } catch (Exception e) {
            Assert.fail("Not equal outputs: " + actualFiles + " vs. " + expectedFiles + ": " + e
                    .getMessage());
        }
    }

}