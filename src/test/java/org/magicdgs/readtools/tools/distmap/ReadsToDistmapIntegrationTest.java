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

package org.magicdgs.readtools.tools.distmap;

import org.magicdgs.readtools.RTCommandLineProgramTest;
import org.magicdgs.readtools.TestResourcesUtils;

import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.broadinstitute.hellbender.utils.test.MiniClusterUtils;
import org.broadinstitute.hellbender.utils.text.XReadLines;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadsToDistmapIntegrationTest extends RTCommandLineProgramTest {

    private static final File TEST_TEMP_DIR =
            createTestTempDir(ReadsToDistmapIntegrationTest.class.getSimpleName());

    private MiniDFSCluster cluster;
    private String clusterOutputFolder;

    // init the cluster and copy the files there
    @BeforeClass(alwaysRun = true)
    public void setupMiniCluster() throws Exception {
        // gets the cluster and create the directory
        cluster = MiniClusterUtils.getMiniCluster();
        final Path distmapClusterFolder = IOUtils.getPath(
                cluster.getFileSystem().getUri().toString()
                        + "/distmap_input/fastq_paired_end/");
        clusterOutputFolder = distmapClusterFolder.toUri().toString();
    }

    // stop the mini-cluster
    @AfterClass(alwaysRun = true)
    public void shutdownMiniCluster() {
        MiniClusterUtils.stopCluster(cluster);
    }

    @DataProvider(name = "toDistmap")
    public Object[][] tesReadsToDistmapData() {
        // expected files for walkthrough data
        final File expectedSingleIndexSE = getTestFile("expected.single_index.SE.distmap");
        final File expectedDualIndexSE = getTestFile("expected.dual_index.SE.distmap");
        final File expectedDualIndexPaired = getTestFile("expected.dual_index.paired.distmap");
        final File expectedSingleIndexPaired = getTestFile("expected.single_index.paired.distmap");

        return new Object[][] {
                // standard data (singled and pair-end) - using only BAM files
                {"standard.single_index.SE",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.single_index.SE.bam")),
                        expectedSingleIndexSE},
                {"standard.dual_index.SE",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.SE.bam")),
                        expectedDualIndexSE},
                {"standard.single_index.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.single_index.paired.bam"))
                                .addBooleanArgument("interleaved", true),
                        expectedSingleIndexPaired},
                {"standard.dual_index.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.paired.bam"))
                                .addBooleanArgument("interleaved", true),
                        expectedDualIndexPaired},
                // using paired data with two barcode tags (SAM files)
                {"bc_in_two_tags.dual_index.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.paired.sam"))
                                .addBooleanArgument("interleaved", true)
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2"),
                        expectedDualIndexPaired},
                {"bc_in_two_tags.dual_index.SE",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.SE.sam"))
                                .addArgument("rawBarcodeSequenceTags", "BC")
                                .addArgument("rawBarcodeSequenceTags", "B2"),
                        expectedDualIndexSE},
                // testing barcode in the read name for SAM/BAM
                {"bc_in_read_name.single_index.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("bc_in_read_name.single_index.paired.sam"))
                                .addBooleanArgument("interleaved", true)
                                .addBooleanArgument("barcodeInReadName", true),
                        expectedSingleIndexPaired},
//                // testing mapped file (using also CRAM file, which requires reference)
//                // TODO: this is not working because the reads are in the reverse strand
//                // TODO: maybe also soft-clipped?
//                {"illumina_legacy.dual_index.paired.mapped.cram",
//                        new ArgumentsBuilder()
//                                .addInput(TestResourcesUtils.getWalkthroughDataFile("illumina_legacy.dual_index.paired.mapped.cram"))
//                                .addReference(TestResourcesUtils.getWalkthroughDataFile("2L.fragment.fa"))
//                                .addBooleanArgument("barcodeInReadName", true)
//                                .addBooleanArgument("interleaved", true),
//                        expectedDualIndexPaired},
                // FASTQ split file for Illumina-legacy
                {"illumina_legacy.single_index.paired",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("illumina_legacy.single_index.paired_1.fq"))
                                .addFileArgument("input2", TestResourcesUtils.getWalkthroughDataFile("illumina_legacy.single_index.paired_2.fq")),
                        expectedSingleIndexPaired},
                // FASTQ interleaved file
                {"illumina_legacy.single_index.interleaved",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("illumina_legacy.single_index.interleaved.fq"))
                                .addBooleanArgument("interleaved", true),
                        expectedSingleIndexPaired},
                // FASTQ single-end Casava
                {"casava.single_index.SE",
                        new ArgumentsBuilder()
                                .addInput(TestResourcesUtils.getWalkthroughDataFile("casava.single_index.SE.fq")),
                        expectedSingleIndexSE}
        };
    }

    @Test(dataProvider = "toDistmap")
    public void testReadsToDistmapLocal(final String name, final ArgumentsBuilder args,
            final File expectedOutput)
            throws Exception {
        // output is always in
        final File output = new File(TEST_TEMP_DIR, name + ".distmap");
        // add output, and set the quiet level
        args.addOutput(output);
        runCommandLine(args);

        // using text file concordance
        IntegrationTestSpec.assertEqualTextFiles(output, expectedOutput);
    }

    @Test(dataProvider = "toDistmap")
    public void testReadsToDistmapCluster(final String name, final ArgumentsBuilder args,
            final File expectedOutput)
            throws Exception {
        // output is always in
        final Path output = IOUtils.getPath(clusterOutputFolder + "/" + name + ".distmap");
        // add output, and set the quiet level
        args.addArgument("output", output.toUri().toString());
        runCommandLine(args);

        // using text file concordance
        assertEqualTextFiles(output, expectedOutput);
    }

    @DataProvider
    public Object[][] blockSizes() {
        // this are the two block sizes implemented in Distmap for different mappers
        return new Object[][] {{16777216}, {8388608}};
    }

    @Test(dataProvider = "blockSizes")
    public void testHdfsBlockSize(final long blockSize) throws Exception {
        final org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(clusterOutputFolder, "testHdfsBlockSize-" + blockSize + ".distmap");
        Assert.assertFalse(cluster.getFileSystem().exists(path), "output already exists");

        final ArgumentsBuilder args = new ArgumentsBuilder()
                .addInput(TestResourcesUtils.getExampleDataFile("SRR1931701.illumina_1.fq"))
                .addArgument("output", path.toUri().toString())
                .addArgument("hdfsBlockSize", Long.toString(blockSize));

        runCommandLine(args);

        // only test if its exists and block sizes
        Assert.assertTrue(cluster.getFileSystem().exists(path));
        Assert.assertEquals(cluster.getFileSystem().getFileStatus(path).getBlockSize(), blockSize);
    }

    // copied from IntegrationTestSpec.assertEqualTextFiles to allow path
    private static void assertEqualTextFiles(final Path resultPath, final File expectedFile) throws
            IOException {
        try (final XReadLines resultReader = new XReadLines(Files.newBufferedReader(resultPath), true, null);
        final XReadLines expectedReader = new XReadLines(expectedFile)) {
            final List<String> actualLines = resultReader.readLines().stream()
                    .collect(Collectors.toList());
            final List<String> expectedLines = expectedReader.readLines().stream()
                    .collect(Collectors.toList());
            //For ease of debugging, we look at the lines first and only then check their counts
            final int minLen = Math.min(actualLines.size(), expectedLines.size());
            for (int i = 0; i < minLen; i++) {
                Assert.assertEquals(actualLines.get(i), expectedLines.get(i), "Line number " + i + " (not counting comments)");
            }
            Assert.assertEquals(actualLines.size(), expectedLines.size(), "line counts");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}