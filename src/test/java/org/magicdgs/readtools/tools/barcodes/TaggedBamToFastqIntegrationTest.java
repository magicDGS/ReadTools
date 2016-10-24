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

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TaggedBamToFastqIntegrationTest extends BarcodeToolsIntegrationTests {

    /**
     * Returns an argument builder with the required arguments for all the pair-end tests.
     */
    private static ArgumentsBuilder getPairEndRequiredArgs(final File barcodeFile) {
        return new ArgumentsBuilder()
                .addArgument("barcodes", barcodeFile.getAbsolutePath())
                .addArgument("input", PAIRED_BAM_FILE.getAbsolutePath());
    }

    /**
     * Returns an argument builder with the required arguments for all the single-end tests.
     */
    private static ArgumentsBuilder getSingleEndRequiredArgs(final File barcodeFile) {
        return new ArgumentsBuilder()
                .addBooleanArgument("single", true)
                .addArgument("barcodes", barcodeFile.getAbsolutePath())
                .addArgument("input", SINGLE_BAM_FILE.getAbsolutePath());
    }

    @DataProvider(name = "TaggedPairEndBarcodeData")
    public Object[][] getPairEndTaggedBarcodeData() throws Exception {
        return new Object[][] {
                // single end
                {"testTaggedSingleEndDefaultParameters",
                        getSingleEndRequiredArgs(DUAL_BARCODE_FILE),
                        false, false},
                // paired-end
                {"testPairEndDefaultParameters",
                        getPairEndRequiredArgs(DUAL_BARCODE_FILE),
                        true, false},
                // pair-end with max. mismatches
                {"testPairEndMaxMismatch",
                        getPairEndRequiredArgs(DUAL_BARCODE_FILE)
                                .addArgument("maximum-mismatches", "3"),
                        true, false},
                // test with splitting
                {"testTaggedSingleEndSplitting", getSingleEndRequiredArgs(DUAL_BARCODE_FILE)
                        .addBooleanArgument("split", true),
                        false, true},
                // test unique barcode single-end
                {"testTaggedSingleEndDefaultParameterUniqueBarcode",
                        getSingleEndRequiredArgs(UNIQUE_BARCODE_FILE),
                        false, false},
                // test unique barcode pair-end
                {"testPairEndDefaultParametersUniqueBarcode",
                        getPairEndRequiredArgs(UNIQUE_BARCODE_FILE),
                        true, false}
        };
    }

    @Test(dataProvider = "TaggedPairEndBarcodeData")
    public void testPairedTaggedBamToFastq(final String testName, final ArgumentsBuilder args,
            final boolean pairEnd, final boolean split) throws Exception {
        testBarcodeDetectorOutputFastq(testName, args, pairEnd, split);
    }

}