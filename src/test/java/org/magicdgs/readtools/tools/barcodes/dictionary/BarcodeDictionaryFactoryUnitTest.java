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

package org.magicdgs.readtools.tools.barcodes.dictionary;

import org.magicdgs.readtools.cmd.argumentcollections.ReadGroupArgumentCollection;
import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.hellbender.exceptions.UserException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeDictionaryFactoryUnitTest extends RTBaseTest {

    private final static ReadGroupArgumentCollection RG_INFO = new ReadGroupArgumentCollection();

    @DataProvider(name = "barcodeFiles")
    public Object[][] getBarcodeFiles() {
        final List<String> uniqueSamples = Arrays.asList("sample1", "sample2", "sample3");
        final List<String> dualSamples = Arrays.asList("sample1", "sample2", "sample3", "sample1", "sample5");
        return new Object[][] {
                {getTestFile("unique_main_names.barcodes"), uniqueSamples, 1},
                {getTestFile("unique_required_names.barcodes"), uniqueSamples, 1},
                {getTestFile("dual_no_main_names.barcodes"), dualSamples, 2},
                {getTestFile("dual_required_names.barcodes"), dualSamples, 2},
                {getTestFile("dual_as_single_by_repeated_names.barcodes"), dualSamples, 1},
        };
    }


    @Test(dataProvider = "barcodeFiles")
    public void testBarcodeDictionaryFromFile(final File file, final List<String> sampleNames,
            final int numberOfBarcodes) throws Exception {
        final BarcodeDictionary dictionary = BarcodeDictionaryFactory.fromFile(file.toPath(), "runId", RG_INFO);
        Assert.assertEquals(dictionary.getNumberOfBarcodes(), numberOfBarcodes);
        Assert.assertEquals(dictionary.getSampleNames(), sampleNames);
    }

    @DataProvider(name = "badBarcodeFiles")
    public Object[][] getBadBarcodeFiles() {
        return new Object[][] {
                {getTestFile("wrong_columns.barcodes")},
                {getTestFile("empty_column_header.barcodes")}
        };
    }

    @Test(dataProvider = "badBarcodeFiles", expectedExceptions = UserException.MalformedFile.class)
    public void testInvalidFiles(final File file) throws Exception {
        BarcodeDictionaryFactory.fromFile(file.toPath(), "runId", RG_INFO);
    }

    @Test(expectedExceptions = UserException.CouldNotReadInputFile.class)
    public void testNotExistingFile() throws Exception {
        BarcodeDictionaryFactory.fromFile(new File("doesNotExists").toPath(), "runId", RG_INFO);
    }

}