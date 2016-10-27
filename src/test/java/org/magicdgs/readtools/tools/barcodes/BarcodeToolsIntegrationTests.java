/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.utils.tests.CommandLineProgramTest;

import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;

import java.io.File;
import java.util.stream.IntStream;

/**
 * Integration tests container for all the tests using barcodes.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeToolsIntegrationTests extends CommandLineProgramTest {

    private final static String EXPECTED_SHARED_LARGE_DIRECTORY =
            getLargeTestFile("barcodes/expected_data").getAbsolutePath() + "/";

    private final static String EXPECTED_SHARED_DIRECTORY =
            getCommonTestFile("BarcodeTools").getAbsolutePath() + "/";

    /** Example barcode file for one barcode index. */
    protected static final File UNIQUE_BARCODE_FILE = getInputDataFile("unique.barcodes");
    /** Example barcode file for two barcode indexes. */
    protected static final File DUAL_BARCODE_FILE = getInputDataFile("dual.barcodes");

    /** Sample name for test files. */
    protected final static String[] SAMPLE_NAMES = IntStream.range(1, 10)
            .mapToObj(i -> "sample" + i).toArray(String[]::new);

    /** Temp directory for the class. */
    protected final File classTempDirectory = createTestTempDir(this.getClass().getSimpleName());

    /**Gets the expected file shared between several tools for barcodes. */
    protected static final File getBarcodeToolsExpectedData(final String fileName) {
       return new File(EXPECTED_SHARED_DIRECTORY, fileName);
    }

    /**
     * Checks concordance of the file in the outPath and the testName file in the expected shared
     * directory, both ending with suffix.
     */
    protected void checkExpectedSharedFiles(final String testName, final String outPath,
            final String suffix) throws Exception {
        logger.debug("Checking output: {}{}", testName, suffix);
        IntegrationTestSpec.assertEqualTextFiles(
                new File(outPath + suffix),
                getBarcodeToolsExpectedData(testName + suffix));
    }

    /**
     * Test the barcode tool which produces single/pair FASTQ outputs and discarded. The expected
     * files are in {@link #EXPECTED_SHARED_DIRECTORY}.
     */
    public void testBarcodeDetectorOutputFastq(final String testName,
            final ArgumentsBuilder builder,
            final boolean pairEnd, final boolean split) throws Exception {
        log("Running " + testName + " for " + getTestedToolName());
        final File outputPrefix = new File(classTempDirectory, testName);
        final ArgumentsBuilder args =
                builder.addArgument("output", outputPrefix.getAbsolutePath());
        // running command line
        runCommandLine(args);
        // check the metrics file
        checkExpectedSharedFiles(testName, outputPrefix.getAbsolutePath(), ".metrics");
        // suffixes for pair-end or single end data
        final String[] suffixes = (pairEnd)
                ? new String[] {"_1.fq.gz", "_discarded_1.fq.gz", "_2.fq.gz", "_discarded_2.fq.gz"}
                : new String[] {".fq.gz", "_discarded.fq.gz"};
        for (final String suffix : suffixes) {
            if (split && !suffix.contains("discarded")) {
                for (final String sample : SAMPLE_NAMES) {
                    checkExpectedSharedFiles(testName, outputPrefix.getAbsolutePath(),
                            "_" + sample + suffix);
                }
            } else {
                checkExpectedSharedFiles(testName, outputPrefix.getAbsolutePath(), suffix);
            }
        }
    }
}
