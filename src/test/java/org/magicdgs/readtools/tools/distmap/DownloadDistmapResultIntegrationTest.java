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

import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DownloadDistmapResultIntegrationTest extends RTCommandLineProgramTest {

    private static final File TEST_TEMP_DIR =
            createTestTempDir(DownloadDistmapResultIntegrationTest.class.getSimpleName());

    private final File distmapFolder = getClassTestDirectory();

    @DataProvider
    public Object[][] getLocalArguments() {
        return new Object[][] {
                // test all in one batch
                {new ArgumentsBuilder().addInput(distmapFolder),
                        getTestFile("parts-00000-to-00003.sam")},
                // test more batches
                {new ArgumentsBuilder().addInput(distmapFolder).addArgument("numberOfParts", "2"),
                        getTestFile("parts-00000-to-00003.sam")},
                // test only some parts
                {new ArgumentsBuilder().addInput(distmapFolder)
                        .addBooleanArgument("noRemoveTaskProgramGroup", true)
                        .addArgument("partName", "part-00001.gz")
                        .addArgument("partName", "part-00002.gz"),
                        getTestFile("parts-00001-to-00002.sam")},
                // test unsorted with one part
                {new ArgumentsBuilder().addInput(distmapFolder)
                        .addArgument("partName", "part-00001.gz")
                        .addArgument("SORT_ORDER", "unsorted"),
                        getTestFile("only-00001.unsorted.sam")},
        };
    }

    @Test(dataProvider = "getLocalArguments")
    public void testDownloadDistmapResult(final ArgumentsBuilder args, final File expectedOutput)
            throws Exception {
        // output in SAM format for text comparison
        final File output = new File(TEST_TEMP_DIR,
                args.toString() + "." + expectedOutput.getName() + ".sam");

        args.addOutput(output).addBooleanArgument("addOutputSAMProgramRecord", false);
        runCommandLine(args);

        // using text file concordance
        IntegrationTestSpec.assertEqualTextFiles(output, expectedOutput);
    }

    @DataProvider
    public Object[][] getInvalidArguments() {
        return new Object[][]{
                {new ArgumentsBuilder().addInput(distmapFolder)
                        .addArgument("partName", "part-52.gz")},
                {new ArgumentsBuilder().addInput(new File(TEST_TEMP_DIR, "noFile"))},
                {new ArgumentsBuilder().addInput(TEST_TEMP_DIR)}
        };
    }

    @Test(dataProvider = "getInvalidArguments", expectedExceptions = UserException.class)
    public void invalidArguments(final ArgumentsBuilder args) throws Exception {
        args.addOutput(new File(TEST_TEMP_DIR, args.toString() + ".bam"));
        runCommandLine(args);
    }

}