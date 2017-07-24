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
import org.magicdgs.readtools.TestResourcesUtils;

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

    // test files already in standard formatting
    private final static File STANDARD_SINGLE_INDEX_SE = TestResourcesUtils.getWalkthroughDataFile("standard.single_index.SE.sam");
    private final static File STANDARD_DUAL_INDEX_SE = TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.SE.sam");
    private final static File STANDARD_SINGLE_INDEX_PAIRED = TestResourcesUtils.getWalkthroughDataFile("standard.single_index.paired.sam");
    private final static File STANDARD_DUAL_INDEX_PAIRED = TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.paired.sam");

    // for CRAM
    // TODO: uncomment when CRAM tests are enabled
    // private final static File REFERENCE_FOR_CRAM_TESTS = TestResourcesUtils.getWalkthroughDataFile("2L.fragment.fa");

    // TODO: it will be nice to split the data provider in different kind of inputs to
    // TODO: have a good way of check what is failing (CRAM? BAM? SAM? FASTQ? DUAL_INDEX? SINGLE_INDEX? PAIRED? UNPAIRED? SPLIT_FILES?)
    // TODO: this will be useful also for enabling reference tests
    @DataProvider(name = "toStandardize")
    public Object[][] tesStandardizeReadsData() {
        return new Object[][] {
                // standardize already standard files provides the same result
                // SAM files
                {"standard.single_index.SE",
                        new ArgumentsBuilder()
                                .addInput(STANDARD_SINGLE_INDEX_SE),
                        STANDARD_SINGLE_INDEX_SE},
                {"standard.dual_index.SE",
                        new ArgumentsBuilder()
                                .addInput(STANDARD_DUAL_INDEX_SE),
                        STANDARD_DUAL_INDEX_SE},
                {"standard.single_index.paired",
                        new ArgumentsBuilder()
                                .addBooleanArgument("interleaved", true)
                                .addInput(STANDARD_SINGLE_INDEX_PAIRED),
                        STANDARD_SINGLE_INDEX_PAIRED},
                {"standard.dual_index.paired",
                        new ArgumentsBuilder()
                                .addBooleanArgument("interleaved", true)
                                .addInput(STANDARD_DUAL_INDEX_PAIRED),
                        STANDARD_DUAL_INDEX_PAIRED},
                // BAM files
                {"standard.single_index.SE.bam",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.single_index.SE.bam")),
                        STANDARD_SINGLE_INDEX_SE},
                {"standard.dual_index.SE.bam",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.SE.bam")),
                        STANDARD_DUAL_INDEX_SE},
                {"standard.single_index.paired.bam",
                        new ArgumentsBuilder()
                                .addBooleanArgument("interleaved", true)
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.single_index.paired.bam")),
                        STANDARD_SINGLE_INDEX_PAIRED},
                {"standard.dual_index.paired.bam",
                        new ArgumentsBuilder()
                                .addBooleanArgument("interleaved", true)
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.paired.bam")),
                        STANDARD_DUAL_INDEX_PAIRED},
                // CRAM files - requires reference
                // TODO: to enable CRAM file testing, we should ignore the @SQ header lines
                // TODO: because they aren't in the expected output (https://github.com/magicDGS/ReadTools/issues/305)
//                {"standard.single_index.SE.cram",
//                        new ArgumentsBuilder()
//                                .addReference(REFERENCE_FOR_CRAM_TESTS)
//                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.single_index.SE.cram")),
//                        STANDARD_SINGLE_INDEX_SE},
//                {"standard.dual_index.SE.cram",
//                        new ArgumentsBuilder()
//                                .addReference(REFERENCE_FOR_CRAM_TESTS)
//                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.SE.cram")),
//                        STANDARD_DUAL_INDEX_SE},
//                {"standard.single_index.paired.cram",
//                        new ArgumentsBuilder()
//                                .addReference(REFERENCE_FOR_CRAM_TESTS)
//                                .addBooleanArgument("interleaved", true)
//                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.single_index.paired.cram")),
//                        STANDARD_SINGLE_INDEX_PAIRED},
//                {"standard.dual_index.paired.cram",
//                        new ArgumentsBuilder()
//                                .addReference(REFERENCE_FOR_CRAM_TESTS)
//                                .addBooleanArgument("interleaved", true)
//                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.paired.cram")),
//                        STANDARD_DUAL_INDEX_PAIRED},

                // standardize a SAM file with misencoded qualities (Illumina)
                {"misencoded.single_index.SE",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("misencoded.single_index.SE.sam")),
                        STANDARD_SINGLE_INDEX_SE},


                // standardize a SAM with barcode in the read name
                {"bc_in_read_name.single_index.SE",
                        new ArgumentsBuilder()
                                .addBooleanArgument("barcodeInReadName", true)
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_read_name.single_index.SE.sam")),
                        STANDARD_SINGLE_INDEX_SE},
                {"bc_in_read_name.dual_index.SE",
                        new ArgumentsBuilder()
                                .addBooleanArgument("barcodeInReadName", true)
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_read_name.dual_index.SE.sam")),
                        STANDARD_DUAL_INDEX_SE},
                {"bc_in_read_name.single_index.paired",
                        new ArgumentsBuilder()
                                .addBooleanArgument("barcodeInReadName", true)
                                .addBooleanArgument("interleaved", true)
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_read_name.single_index.paired.sam")),
                        STANDARD_SINGLE_INDEX_PAIRED},
                {"bc_in_read_name.dual_index.paired",
                        new ArgumentsBuilder()
                                .addBooleanArgument("barcodeInReadName", true)
                                .addBooleanArgument("interleaved", true)
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_read_name.dual_index.paired.sam")),
                        STANDARD_DUAL_INDEX_PAIRED},

                // standardize a SAM file with dual index stored in BC/B2 tags (e.g., illumina2bam)
                {"bc_in_two_tags.dual_index.SE",
                        new ArgumentsBuilder()
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2")
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.SE.sam")),
                        STANDARD_DUAL_INDEX_SE},
                {"bc_in_two_tags.dual_index.paired",
                        new ArgumentsBuilder()
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2")
                                .addBooleanArgument("interleaved", true)
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.paired.sam")),
                        STANDARD_DUAL_INDEX_PAIRED},

                // standardize FASTQ files
                // with Casava read names
                {"casava.single_index.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("casava.single_index.paired_1.fq"))
                                .addFileArgument("input2", TestResourcesUtils.getWalkthroughDataFile("casava.single_index.paired_2.fq")),
                        STANDARD_SINGLE_INDEX_PAIRED},
                // TODO: with Casava format, single-end data is marked as first of pair
                // TODO: and we should force somehow single-end processing to set the unpaired flag
                // TODO: thus, this isn't in the expected output (should be marked as single-end)
                // TODO: see https://github.com/magicDGS/ReadTools/issues/306 for possible solutions
                // TODO: we should enable this test once it is solved
//                {"casava.single_index.SE",
//                        new ArgumentsBuilder()
//                                .addInput(TestResourcesUtils.getWalkthroughDataFile("casava.single_index.SE.fq")),
//                        STANDARD_SINGLE_INDEX_SE},
                // with Illumina-legacy read names
                // misencoded qualities (Illumina)
                {"legacy.single_index.illumina_quality.SE",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.illumina_quality.SE.fq")),
                        STANDARD_SINGLE_INDEX_SE},
                // misencoded qualities (Illumina)
                {"legacy.single_index.illumina_quality.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.illumina_quality_1.fq"))
                                .addFileArgument("input2", TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.illumina_quality_2.fq")),
                        STANDARD_SINGLE_INDEX_PAIRED},
                 // interleaved FASTQ
                {"legacy.dual_index.interleaved",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.interleaved.fq"))
                                .addBooleanArgument("interleaved", true),
                        STANDARD_DUAL_INDEX_PAIRED
                },
                // several standard FASTQ files
                {"legacy.dual_index.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired_1.fq"))
                                .addFileArgument("input2", TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired_2.fq")),
                        STANDARD_DUAL_INDEX_PAIRED},
                {"legacy.dual_index.SE",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.SE.fq")),
                        STANDARD_DUAL_INDEX_SE},
                {"legacy.single_index.interleaved.fq",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.interleaved.fq"))
                                .addBooleanArgument("interleaved", true),
                        STANDARD_SINGLE_INDEX_PAIRED},
                {"legacy.single_index.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.paired_1.fq"))
                                .addFileArgument("input2", TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.paired_2.fq")),
                        STANDARD_SINGLE_INDEX_PAIRED
                },
                {"legacy.single_index.SE",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.SE.fq")),
                        STANDARD_SINGLE_INDEX_SE},

                // test for barcode quality tags option (not in the Walkthrough data)
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