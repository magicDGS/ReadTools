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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.RTCommandLineProgramTest;
import org.magicdgs.readtools.TestResourcesUtils;

import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;
import org.broadinstitute.hellbender.cmdline.TestProgramGroup;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeDetectorArgumentCollectionUnitTest extends RTCommandLineProgramTest {

    private static final File UNIQUE_BARCODE_FILE = TestResourcesUtils.getWalkthroughDataFile("single.barcodes");
    private static final File DUAL_BARCODE_FILE = TestResourcesUtils.getWalkthroughDataFile("dual.barcodes");

    @CommandLineProgramProperties(oneLineSummary = "BarcodeDetectorArgumentCollection", summary = "BarcodeDetectorArgumentCollection", programGroup = TestProgramGroup.class)
    private final static class BarcodeDetectorArgumentCollectionTool extends CommandLineProgram {
        @ArgumentCollection
        public final BarcodeDetectorArgumentCollection args =
                new BarcodeDetectorArgumentCollection();

        // TODO: this is not necessary when range is specified
        @Override
        public String[] customCommandLineValidation() {
            args.validateArguments();
            return null;
        }

        @Override
        protected Object doWork() {
            // this will blows up if the decoder is wrong
            args.getBarcodeDecoder();
            return 1;
        }
    }

    @DataProvider(name = "badArgumentsForValidation")
    public Object[][] getBadArsForValidation() {
        return new Object[][] {
                {-1, Collections.emptyList(), Collections.emptyList()},
                {2, Collections.emptyList(), Collections.emptyList()},
                {2, Collections.singletonList(-1), Collections.emptyList()},
                {2, Arrays.asList(1, -1), Collections.emptyList()},
                {2, Collections.singletonList(0), Collections.emptyList()},
                {2, Collections.singletonList(0), Collections.singletonList(-1)},
                {2, Collections.singletonList(0), Arrays.asList(1, -1)},
                {2, Collections.singletonList(0), Collections.singletonList(0)},
                {2, Collections.singletonList(0), Arrays.asList(1, 0)},
        };
    }

    @Test(dataProvider = "badArgumentsForValidation", expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testBadArgumentsInValidateArguments(final Integer maxN,
            final List<Integer> maxMismatches, final List<Integer> minDistance) throws Exception {
        runBarcodeDetectorArgumentCollectionToolWithArgs(getTestFile("unique.barcodes"), maxN,
                maxMismatches, minDistance);
    }

    // test the validation while running a tool with customCommandLineValidation
    // should thrown
    private Object runBarcodeDetectorArgumentCollectionToolWithArgs(final File barcodeFile,
            final Integer maxN, final List<Integer> maxMismatches,
            final List<Integer> minDistance) {
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .addFileArgument("barcodeFile", barcodeFile)
                .addArgument("maximumN", String.valueOf(maxN));
        if (maxMismatches.isEmpty()) {
            args.addArgument("maximumMismatches", "null");
        } else {
            maxMismatches.forEach(m -> args.addArgument("maximumMismatches", String.valueOf(m)));
        }
        if (minDistance.isEmpty()) {
            args.addArgument("minimumDistance", "null");
        } else {
            minDistance.forEach(m -> args.addArgument("minimumDistance", String.valueOf(m)));
        }
        final BarcodeDetectorArgumentCollectionTool tool =
                new BarcodeDetectorArgumentCollectionTool();
        return tool.instanceMain(injectDefaultVerbosity(args.getArgsList()).toArray(new String[0]));
    }

    @DataProvider(name = "badArgumentsForGetBarcodeDecoder")
    public Object[][] getBadArgumentsForBarcodeDecoder() {
        final List<Integer> oneElement = Collections.singletonList(1);
        final List<Integer> twoElements = Arrays.asList(1, 2);
        final List<Integer> threeElements = Arrays.asList(1, 2, 3);
        return new Object[][] {
                {getTestFile("notExistingBarcode"), oneElement, oneElement},
                {UNIQUE_BARCODE_FILE, twoElements, oneElement},
                {UNIQUE_BARCODE_FILE, oneElement, twoElements},
                {DUAL_BARCODE_FILE, threeElements, twoElements},
                {DUAL_BARCODE_FILE, twoElements, threeElements}
        };
    }

    @Test(dataProvider = "badArgumentsForGetBarcodeDecoder", expectedExceptions = UserException.class)
    public void testBadArgumentsForGetBarcodeDecoder(final File barcodeFile,
            final List<Integer> maxMismatches, final List<Integer> minDistance) throws Exception {
        runBarcodeDetectorArgumentCollectionToolWithArgs(barcodeFile, null, maxMismatches,
                minDistance);
    }

    @DataProvider(name = "goodArguments")
    public Object[][] getGoodArguments() {
        final List<Integer> oneElement = Collections.singletonList(1);
        final List<Integer> twoElements = Arrays.asList(1, 2);
        return new Object[][] {
                {UNIQUE_BARCODE_FILE, null, oneElement, oneElement},
                {DUAL_BARCODE_FILE, null, oneElement, oneElement},
                {DUAL_BARCODE_FILE, null, twoElements, twoElements},
                {UNIQUE_BARCODE_FILE, 1, oneElement, oneElement},
                {DUAL_BARCODE_FILE, 1, oneElement, oneElement},
                {DUAL_BARCODE_FILE, 1, twoElements, twoElements}
        };
    }

    // maxN is only for exercise the code path
    @Test(dataProvider = "goodArguments")
    public void testGoodArguments(final File barcodeFile, final Integer maxN,
            final List<Integer> maxMismatches, final List<Integer> minDistance) throws Exception {
        runBarcodeDetectorArgumentCollectionToolWithArgs(barcodeFile, maxN, maxMismatches,
                minDistance);
    }

}