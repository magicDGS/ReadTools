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

package org.magicdgs.readtools.tools.barcodes;

import org.magicdgs.readtools.utils.read.ReadReaderFactory;
import org.magicdgs.readtools.RTCommandLineProgramTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class AssignReadGroupByBarcodeIntegrationTest extends RTCommandLineProgramTest {

    // old test files in FASTQ format -> they are modified to have the correct separator
    private final File DUAL_FASTQ_1 = getTestFile("SRR1931701.dual.barcoded_1.fq");
    private final File DUAL_FASTQ_2 = getTestFile("SRR1931701.dual.barcoded_2.fq");

    private final File SINGLE_BAM_FILE = getTestFile("SRR1931701.single.tagged.sam");
    private final File PAIRED_BAM_FILE = getTestFile("SRR1931701.tagged.sam");

    protected final File UNIQUE_BARCODE_FILE = getTestFile("unique.barcodes");
    protected final File DUAL_BARCODE_FILE = getTestFile("dual.barcodes");

    // this is in sync with the input files
    private final static List<String> EXPECTED_BY_SAMPLE_EXT = IntStream.range(1, 10)
            .mapToObj(i -> "_sample" + i + ".sam").collect(Collectors.toList());

    // TODO: clean up this tests
    @DataProvider
    public Object[][] addByReadGroupData() {
        // the metrics file are the same
        return new Object[][] {
                // CONCORDANCE with legacy TaggedBamToFastq (removed)
                // the input file is the same, but without header

                // single end
                {"testTaggedSingleEndDefaultParameters", "TaggedBamToFastq",
                        new ArgumentsBuilder().addInput(SINGLE_BAM_FILE)
                                .addFileArgument("barcodeFile", DUAL_BARCODE_FILE)
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2"),
                        false},
                // paired-end
                {"testPairEndDefaultParameters", "TaggedBamToFastq",
                        new ArgumentsBuilder().addInput(PAIRED_BAM_FILE)
                                .addFileArgument("barcodeFile", DUAL_BARCODE_FILE)
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2")
                                .addBooleanArgument("interleaved", true),
                        false},
                // pair-end with max. mismatches
                {"testPairEndMaxMismatch", "TaggedBamToFastq",
                        new ArgumentsBuilder().addInput(PAIRED_BAM_FILE)
                                .addFileArgument("barcodeFile", DUAL_BARCODE_FILE)
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2")
                                .addBooleanArgument("interleaved", true)
                                .addArgument("maximumMismatches", "3"),
                        false},
                // test with splitting
                {"testTaggedSingleEndSplitting", "TaggedBamToFastq",
                        new ArgumentsBuilder().addInput(SINGLE_BAM_FILE)
                                .addFileArgument("barcodeFile", DUAL_BARCODE_FILE)
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2"),
                        true},
                // test unique barcode single-end
                {"testTaggedSingleEndDefaultParameterUniqueBarcode", "TaggedBamToFastq",
                        new ArgumentsBuilder().addInput(SINGLE_BAM_FILE)
                                .addFileArgument("barcodeFile", UNIQUE_BARCODE_FILE),
                        false},
                // test unique barcode pair-end
                {"testPairEndDefaultParametersUniqueBarcode", "TaggedBamToFastq",
                        new ArgumentsBuilder().addInput(PAIRED_BAM_FILE)
                                .addFileArgument("barcodeFile", UNIQUE_BARCODE_FILE)
                                .addBooleanArgument("interleaved", true),
                        false},

                // CONCORDANCE with legacy FastqBarcodeDetector (removed)
                // the input files are the same except dual-barcoded ones, which have the new separator


                // single end
                {"testSingleEndDefaultParameters", "FastqBarcodeDetector",
                        new ArgumentsBuilder().addInput(DUAL_FASTQ_1)
                                .addFileArgument("barcodeFile", DUAL_BARCODE_FILE),
                        false},
                // paired-end
                {"testPairEndDefaultParameters", "FastqBarcodeDetector",
                        new ArgumentsBuilder().addInput(DUAL_FASTQ_1)
                                .addFileArgument("input2", DUAL_FASTQ_2)
                                .addFileArgument("barcodeFile", DUAL_BARCODE_FILE),
                        false},
                // pair-end with max. mismatches
                {"testPairEndMaxMismatch", "FastqBarcodeDetector",
                        new ArgumentsBuilder().addInput(DUAL_FASTQ_1)
                                .addFileArgument("input2", DUAL_FASTQ_2)
                                .addFileArgument("barcodeFile", DUAL_BARCODE_FILE)
                                .addArgument("maximumMismatches", "3"),
                        false},
                // test with splitting
                {"testSingleEndSplitting", "FastqBarcodeDetector",
                        new ArgumentsBuilder().addInput(DUAL_FASTQ_1)
                                .addFileArgument("barcodeFile", DUAL_BARCODE_FILE),
                        true},
                // test unique barcode single-end
                {"testSingleEndDefaultParameterUniqueBarcode", "FastqBarcodeDetector",
                        new ArgumentsBuilder().addInput(SMALL_FASTQ_1)
                                .addFileArgument("barcodeFile", UNIQUE_BARCODE_FILE),
                        false},
                // test unique barcode pair-end
                {"testPairEndDefaultParametersUniqueBarcode", "FastqBarcodeDetector",
                        new ArgumentsBuilder().addInput(SMALL_FASTQ_1)
                                .addFileArgument("input2", SMALL_FASTQ_2)
                                .addFileArgument("barcodeFile", UNIQUE_BARCODE_FILE),
                        false},

                // CONCORDANCE with legacy BamBarcodeDetector (removed)
                {"testBamBarcodeDetector", "BamBarcodeDetector",
                        new ArgumentsBuilder()
                                .addFileArgument("barcodeFile", UNIQUE_BARCODE_FILE)
                                .addFileArgument("input", getInputDataFile("example.mapped.sam"))
                                .addBooleanArgument("barcodeInReadName", true),
                        false},
                {"testBamBarcodeDetectorSplit", "BamBarcodeDetector",
                        new ArgumentsBuilder()
                                .addFileArgument("barcodeFile", UNIQUE_BARCODE_FILE)
                                .addFileArgument("input", getInputDataFile("example.mapped.sam"))
                                .addBooleanArgument("barcodeInReadName", true),
                        true}
        };
    }

    // expected outputs are SAM files
    // testSplit is only for splitSample
    @Test(dataProvider = "addByReadGroupData")
    public void testAddReadGroupByBarcodeOldToolConcordance(final String testName,
            final String deprecatedTool,
            final ArgumentsBuilder builder,
            final boolean testSplit) throws Exception {
        log("Testing " + testName + " for deprecated " + deprecatedTool);

        // add the outputs
        final File actualOutputPrefix = new File(createTestTempDir(deprecatedTool), testName);
        builder.addOutput(actualOutputPrefix)
                .addBooleanArgument("addOutputSAMProgramRecord",
                        false) // does not output the program group record
                .addArgument("outputFormat", "SAM") // output always SAM as text file
                .addBooleanArgument("keepDiscarded",
                        true) // always keep discarded for the concordance tests, because it previously was done
                .addBooleanArgument("splitSample", testSplit);

        // run the command line
        Assert.assertNull(runCommandLine(builder));

        // first check the metrics file -> the metrics file is the same
        metricsFileConcordance(new File(actualOutputPrefix + ".metrics"),
                getTestFile(testName + ".metrics"));

        testSamFileEquivalentForBarcodeDetection(new File(actualOutputPrefix + "_discarded.sam"),
                getTestFile(testName + "_discarded.sam"));

        // get the extensions to check
        final List<String> outputToCheck = (testSplit)
                ? EXPECTED_BY_SAMPLE_EXT : Collections.singletonList(".sam");
        // check them
        for (final String ext : outputToCheck) {
            // we expect that they are
            testSamFileEquivalentForBarcodeDetection(new File(actualOutputPrefix + ext),
                    getTestFile(testName + ext));
        }
    }

    // TODO: this should be removed to check if the files are exactly the same
    private void testSamFileEquivalentForBarcodeDetection(final File actualSam, File expectedSam)
            throws Exception {
        final ReadReaderFactory factory = new ReadReaderFactory();
        try (final SamReader actualReader = factory.openSamReader(actualSam);
                final SamReader expectedReader = factory.openSamReader(expectedSam)) {
            final SAMFileHeader actualHeader = actualReader.getFileHeader();
            final SAMFileHeader expectedHeader = expectedReader.getFileHeader();
            // check headers are equal
            Assert.assertEquals(actualHeader, expectedHeader);
            // check all the reads
            final Iterator<SAMRecord> it = actualReader.iterator();
            expectedReader.iterator().forEachRemaining(expected -> {
                Assert.assertTrue(it.hasNext(), "no more reads");
                final SAMRecord actual = it.next();
                // check read name to be sure is the same
                Assert.assertEquals(actual.getReadName(), expected.getReadName(), "not same read");
                // check the sequence and quality to check that they are the same
                Assert.assertEquals(actual.getReadBases(), expected.getReadBases(),
                        "not same bases");
                // check the sequence and quality to check that they are the same
                Assert.assertEquals(actual.getBaseQualities(), expected.getBaseQualities(),
                        "not same quals");
                // check the BC tag
                Assert.assertEquals(actual.getAttribute("BC"), expected.getAttribute("BC"),
                        "not same BC");
                // check the RG
                Assert.assertEquals(actual.getReadGroup(), expected.getReadGroup(), "not same RG");
            });
        }
    }

}