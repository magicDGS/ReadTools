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

import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqBarcodeDetectorIntegrationTest extends BarcodeToolsIntegrationTests {

    private final static File DUAL_FASTQ_1 = getInputDataFile("SRR1931701.dual.barcoded_1.fq");
    private final static File DUAL_FASTQ_2 = getInputDataFile("SRR1931701.dual.barcoded_2.fq");

    /**
     * Returns an argument builder with the required arguments for all the tests for dual
     * barcoding.
     */
    private static ArgumentsBuilder getDualRequiredArgs() {
        return new ArgumentsBuilder()
                .addArgument("barcodes", DUAL_BARCODE_FILE.getAbsolutePath())
                .addArgument("input1",
                        DUAL_FASTQ_1.getAbsolutePath());
    }

    /**
     * Returns an argument builder with the required arguments for all the tests for unique
     * barcoding.
     */
    private static ArgumentsBuilder getUniqueRequiredArgs() {
        return new ArgumentsBuilder()
                .addArgument("barcodes", UNIQUE_BARCODE_FILE.getAbsolutePath())
                .addArgument("input1",
                        SMALL_FASTQ_1.getAbsolutePath());
    }

    @DataProvider(name = "FastqBarcodeData")
    public Object[][] getFastqBarcodeData() throws Exception {
        return new Object[][] {
                // single end
                {"testSingleEndDefaultParameters", getDualRequiredArgs(),
                        false, false},
                // paired-end
                {"testPairEndDefaultParameters", getDualRequiredArgs()
                        .addArgument("input2", DUAL_FASTQ_2.getAbsolutePath()),
                        true, false},
                // pair-end with max. mismatches
                {"testPairEndMaxMismatch", getDualRequiredArgs()
                        .addArgument("input2", DUAL_FASTQ_2.getAbsolutePath())
                        .addArgument("maximum-mismatches", "3"),
                        true, false},
                // test with splitting
                {"testSingleEndSplitting", getDualRequiredArgs()
                        .addBooleanArgument("split", true),
                        false, true},
                // test unique barcode single-end
                {"testSingleEndDefaultParameterUniqueBarcode", getUniqueRequiredArgs(),
                        false, false},
                // test unique barcode pair-end
                {"testPairEndDefaultParametersUniqueBarcode", getUniqueRequiredArgs()
                        .addArgument("input2", SMALL_FASTQ_2.getAbsolutePath()), true, false},
        };
    }

    @Test(dataProvider = "FastqBarcodeData")
    public void testFastqBarcodeDetector(final String testName, final ArgumentsBuilder args,
            final boolean pairEnd, final boolean split) throws Exception {
        testBarcodeDetectorOutputFastq(testName, args, pairEnd, split);
    }

    @Test
    public void testSingleEndDefaultParameterUniqueBarcodeWithSpecifiedSeparator() throws Exception {
        final String testName = "testSingleEndDefaultParameterUniqueBarcodeWithSpecifiedSeparator";
        log("Running " + testName);
        final File outputPrefix = new File(classTempDirectory, testName);
        // running command line
        runCommandLine(Arrays.asList("--readNameBarcodeSeparator", " 1:N:0:",
                "--barcodes", UNIQUE_BARCODE_FILE.getAbsolutePath(),
                "--input1", getInputDataFile("SRR1931701.separator.single.fq").getAbsolutePath(),
                "--output", outputPrefix.getAbsolutePath()));
        // check the metrics file
        checkExpectedSharedFiles("testSingleEndDefaultParameterUniqueBarcode",
                outputPrefix.getAbsolutePath(), ".metrics");
        // check discarded
        // TODO: change for the normal output once this is solved
        checkExpectedSharedFiles(testName,
                outputPrefix.getAbsolutePath(), "_discarded.fq.gz");
        // check the non-discarded file
        checkExpectedSharedFiles("testSingleEndDefaultParameterUniqueBarcode",
                outputPrefix.getAbsolutePath(), ".fq.gz");
    }
}