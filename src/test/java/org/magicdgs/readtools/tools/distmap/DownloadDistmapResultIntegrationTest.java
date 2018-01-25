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

import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.broadinstitute.hellbender.utils.test.MiniClusterUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DownloadDistmapResultIntegrationTest extends RTCommandLineProgramTest {

    private static final File TEST_TEMP_DIR =
            createTempDir(DownloadDistmapResultIntegrationTest.class.getSimpleName());

    private MiniDFSCluster cluster;
    private String clusterInputFolder;
    private final File distmapFolder = getClassTestDirectory();

    // init the cluster and copy the files there
    @BeforeClass(alwaysRun = true)
    public void setupMiniCluster() throws Exception {
        // gets the cluster and create the directory
        cluster = MiniClusterUtils.getMiniCluster();
        final Path distmapClusterFolder = IOUtils.getPath(
                cluster.getFileSystem().getUri().toString()
                + "/distmap_output/fastq_paired_end_mapping_bwa/");
        Files.createDirectory(distmapClusterFolder);
        clusterInputFolder = distmapClusterFolder.toUri().toString();

        // copy input part files into the directory
        for (final File file: distmapFolder.listFiles((d, f) -> f.startsWith("part-"))) {
            Files.copy(file.toPath(), IOUtils.getPath(clusterInputFolder + "/" + file.getName()));
        }
    }

    // stop the mini-cluster
    @AfterClass(alwaysRun = true)
    public void shutdownMiniCluster() {
        MiniClusterUtils.stopCluster(cluster);
    }

    @DataProvider
    public Object[][] getArguments() {
        return new Object[][] {
                // test all in one batch
                {new ArgumentsBuilder(),
                        getTestFile("parts-00000-to-00003.sam")},
                // test more batches
                {new ArgumentsBuilder().addArgument("numberOfParts", "2"),
                        getTestFile("parts-00000-to-00003.sam")},
                // test only some parts
                {new ArgumentsBuilder()
                        .addBooleanArgument("noRemoveTaskProgramGroup", true)
                        .addArgument("partName", "part-00001.gz")
                        .addArgument("partName", "part-00002.gz"),
                        getTestFile("parts-00001-to-00002.sam")},
                // test unsorted with one part
                {new ArgumentsBuilder()
                        .addArgument("partName", "part-00001.gz")
                        .addArgument("SORT_ORDER", "unsorted"),
                        getTestFile("only-00001.unsorted.sam")},
        };
    }

    @Test
    public void testLocalPreSorted() throws Exception {
        final ArgumentsBuilder args = new ArgumentsBuilder();
        final File expectedOutput = getTestFile("parts-00000-to-00003.sam");
        final File output = new File(TEST_TEMP_DIR,
                args.hashCode() + ".local.presorted." + expectedOutput.getName() + ".sam");
        testDonwloadDistmapResult(args, distmapFolder.getAbsolutePath() + "/presorted", output, expectedOutput);
    }

    @Test(dataProvider = "getArguments")
    public void testDownloadDistmapResultLocal(final ArgumentsBuilder args, final File expectedOutput)
            throws Exception {
        // output in SAM format for text comparison
        final File output = new File(TEST_TEMP_DIR,
                args.hashCode() + ".local." + expectedOutput.getName() + ".sam");
        testDonwloadDistmapResult(args, distmapFolder.getAbsolutePath(), output, expectedOutput);
    }

    @Test(dataProvider = "getArguments")
    public void testDownloadDistmapResultCluster(final ArgumentsBuilder args, final File expectedOutput)
            throws Exception {
        // output in SAM format for text comparison
        final File output = new File(TEST_TEMP_DIR,
                args.hashCode() + ".cluster." + expectedOutput.getName() + ".sam");
        testDonwloadDistmapResult(args, clusterInputFolder, output, expectedOutput);
    }

    // helper method for run every test
    private void testDonwloadDistmapResult(final ArgumentsBuilder args, final String inputPath, final File output, final File expectedOutput)
            throws Exception {

        final ArgumentsBuilder completeArgs = new ArgumentsBuilder(args.getArgsArray())
                .addArgument("input", inputPath)
                .addFileArgument("output", output)
                .addBooleanArgument("addOutputSAMProgramRecord", false);
        runCommandLine(completeArgs);

        // using text file concordance
        IntegrationTestSpec.assertEqualTextFiles(output, expectedOutput);
    }

    @DataProvider
    public Object[][] getInvalidArguments() {
        return new Object[][]{
                {new ArgumentsBuilder().addFileArgument("input", distmapFolder)
                        .addArgument("partName", "part-52.gz")},
                {new ArgumentsBuilder().addFileArgument("input", new File(TEST_TEMP_DIR, "noFile"))},
                {new ArgumentsBuilder().addFileArgument("input", TEST_TEMP_DIR)}
        };
    }

    @Test(dataProvider = "getInvalidArguments", expectedExceptions = UserException.class)
    public void invalidArguments(final ArgumentsBuilder args) throws Exception {
        args.addFileArgument("output", new File(TEST_TEMP_DIR, args.hashCode() + ".bam"));
        runCommandLine(args);
    }

}