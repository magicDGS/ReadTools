/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.tools.mapped;

import org.magicdgs.readtools.RTCommandLineProgramTest;
import org.magicdgs.readtools.TestResourcesUtils;

import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.BaseTest;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ComputeProperStatByWindowIntegrationTest extends RTCommandLineProgramTest {

    private final static File TEMP_DIR = createTestTempDir("ComputeProperStatByWindowIntegrationTest");

    // dataset generated for testing
    // includes mates in other contigs but main contigs should have more reads
    // it is indexed
    private final File exampleBam = getTestFile("example.bam");
    private final List<String> exampleMainContigs = Arrays.asList("contig1", "contig2");

    @DataProvider
    public Object[][] testCases() {
        final ArgumentsBuilder contigArgs = new ArgumentsBuilder();
        exampleMainContigs.forEach(val -> contigArgs.addArgument("contig", val));
        return new Object[][]{
                // test the print-all with one test
                {"example/all.testCount_NM_LT_2.w100000",
                        exampleBam,
                        new ArgumentsBuilder()
                                .addArgument("window-size", "100000")
                                .addArgument("count-pair-int-tag-list", "NM")
                                .addArgument("count-pair-int-tag-operator-list", "LT")
                                .addArgument("count-pair-int-tag-threshold-list", "2")
                },
                // do not print all in this test cases to reduce output
                {"example/testCount_NM_LT_2_and_3.w10000",
                        exampleBam,
                        new ArgumentsBuilder().addBooleanArgument("do-not-print-all", true)
                                .addArgument("window-size", "10000")
                                .addArgument("count-pair-int-tag-list", "NM")
                                .addArgument("count-pair-int-tag-operator-list", "LT")
                                .addArgument("count-pair-int-tag-threshold-list", "2")
                                .addArgument("count-pair-int-tag-list", "NM")
                                .addArgument("count-pair-int-tag-operator-list", "LT")
                                .addArgument("count-pair-int-tag-threshold-list", "3")
                },
                {"example/testCount_NM_LT_2.indels.softclips.w10000",
                        exampleBam,
                        new ArgumentsBuilder().addBooleanArgument("do-not-print-all", true)
                                .addArgument("window-size", "10000")
                                .addArgument("count-pair-int-tag-list", "NM")
                                .addArgument("count-pair-int-tag-operator-list", "LT")
                                .addArgument("count-pair-int-tag-threshold-list", "2")
                                .addBooleanArgument("ContainSoftclipCounter", true)
                                .addBooleanArgument("ContainIndelCounter", true)
                },
                // smaller window-size to test (slower) but only limited to contigs
                {"example/contigs.testCount_NM_LT_2.w5000",
                        exampleBam,
                        new ArgumentsBuilder(contigArgs.getArgsArray()).addBooleanArgument("do-not-print-all", true)
                                .addArgument("window-size", "5000")
                                .addArgument("count-pair-int-tag-list", "NM")
                                .addArgument("count-pair-int-tag-operator-list", "LT")
                                .addArgument("count-pair-int-tag-threshold-list", "2")
                }
        };
    }

    @Test(dataProvider = "testCases")
    public void testComputeProperStatByWindow(final String testName, final File input, final ArgumentsBuilder args)
            throws Exception{
        // get the expected file name
        final File expected = getTestFile("expected/" + testName + ".table");

        // creates a temp output with the same name
        final File tmpOutput = new File(TEMP_DIR, expected.getName());
        // run the tool with the input/output arguments added to the arg-builder
        runCommandLine(args
                .addFileArgument("input", input)
                .addFileArgument("output", tmpOutput));

        // test that the test file is the same as the expected
        IntegrationTestSpec.assertEqualTextFiles(tmpOutput, expected);
    }

    @DataProvider
    public Object[][] badArgs() throws Exception{
        // except output and window-size
        final ArgumentsBuilder nonThrowingArgs = new ArgumentsBuilder()
                .addFileArgument("input", exampleBam)
                .addArgument("count-pair-int-tag-list", "NM")
                .addArgument("count-pair-int-tag-operator-list", "LT")
                .addArgument("count-pair-int-tag-threshold-list", "2");
        return new Object[][]{
                // problem creating output file (non-overwrite)
                {new ArgumentsBuilder(nonThrowingArgs.getArgsArray())
                        .addFileArgument("output", File.createTempFile("exists", ".table"))
                        .addArgument("window-size", "100")
                },
                // unsorted input
                {new ArgumentsBuilder(nonThrowingArgs.getArgsArray())
                        .addFileArgument("input", TestResourcesUtils.getWalkthroughDataFile("standard.single_index.SE.bam"))
                        .addArgument("window-size", "100")
                        .addFileArgument("output", BaseTest.getSafeNonExistentFile("unsorted.table"))

                },
                // intervals provided (temporary unsupported)
                {new ArgumentsBuilder(nonThrowingArgs.getArgsArray())
                        .addArgument("L", "contig1:1-100")
                        .addArgument("window-size", "100")
                        .addFileArgument("output", BaseTest.getSafeNonExistentFile("intervals.table"))
                },
                // incorrect tag-list arguments (two tags, only one operator/threshold)
                {new ArgumentsBuilder()
                        .addFileArgument("input", exampleBam)
                        .addArgument("window-size", "100")
                        .addArgument("count-pair-int-tag-list", "NM")
                        .addArgument("count-pair-int-tag-list", "NM")
                        .addArgument("count-pair-int-tag-operator-list", "LT")
                        .addArgument("count-pair-int-tag-threshold-list", "2")
                        .addFileArgument("output", BaseTest.getSafeNonExistentFile("intervals.table"))

                }
        };
    }


    // set timeOut to ensure that if it does not fail, the program does not proceed consuming time
    @Test(dataProvider = "badArgs", expectedExceptions = UserException.class, timeOut = 1500)
    public void testBadArguments(final ArgumentsBuilder args) {
        runCommandLine(args);
    }
}