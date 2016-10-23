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
 */
package org.magicdgs.readtools.tools.barcodes.dictionary;

import htsjdk.samtools.SAMReadGroupRecord;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class BarcodeDictionaryTest {

    private static BarcodeDictionary dictionarySingle, dictionaryDouble;

    private static final ArrayList<SAMReadGroupRecord> samples = new ArrayList<>();

    private static final ArrayList<ArrayList<String>> barcodesSingle = new ArrayList<>(1);

    private static final ArrayList<ArrayList<String>> barcodesDouble = new ArrayList<>(2);

    private static final String[] barcodes = new String[] {"AAAA", "CCCC", "TTTT", "GGGG"};

    private static String getBarcode(char base) {
        char[] bases = new char[10];
        Arrays.fill(bases, base);
        return new String(bases);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        barcodesSingle.add(new ArrayList<>());
        barcodesDouble.add(new ArrayList<>());
        barcodesDouble.add(new ArrayList<>());
        for (int i = 0; i < barcodes.length; i++) {
            final SAMReadGroupRecord rg =
                    new SAMReadGroupRecord("sample" + i + String.join("_", barcodes[i]),
                            BarcodeDictionaryFactory.UNKNOWN_READGROUP_INFO);
            samples.add(rg);
            barcodesSingle.get(0).add(barcodes[i]);
            barcodesDouble.get(0).add(barcodes[i]);
            barcodesDouble.get(1).add(barcodes[i]);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        barcodesSingle.clear();
        barcodesDouble.clear();
        samples.clear();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        dictionarySingle = new BarcodeDictionary(samples, barcodesSingle);
        dictionaryDouble = new BarcodeDictionary(samples, barcodesDouble);
    }

    @Test
    public void testGetNumberOfBarcodes() throws Exception {
        Assert.assertEquals(dictionarySingle.getNumberOfBarcodes(), 1);
        Assert.assertEquals(dictionaryDouble.getNumberOfBarcodes(), 2);
    }

    @Test
    public void testGetSampleNames() throws Exception {
        final ArrayList<String> sampleNames = new ArrayList<>();
        for (final SAMReadGroupRecord sample : samples) {
            sampleNames.add(sample.getSample());
        }
        Assert.assertEquals(dictionarySingle.getSampleNames(), sampleNames);
        Assert.assertEquals(dictionaryDouble.getSampleNames(), sampleNames);
    }

    @Test
    public void testGetSampleReadGroups() throws Exception {
        Assert.assertEquals(dictionarySingle.getSampleReadGroups(), samples);
        Assert.assertEquals(dictionaryDouble.getSampleReadGroups(), samples);
    }

    @Test
    public void testNumberOfSamples() throws Exception {
        Assert.assertEquals(dictionarySingle.numberOfSamples(), barcodes.length);
        Assert.assertEquals(dictionaryDouble.numberOfSamples(), barcodes.length);
    }

    @Test(enabled = false, description = "Not implemented")
    public void testNumberOfUniqueSamples() throws Exception {
        // TODO: create a dictionary with repeated samples
    }

    @Test
    public void testGetBarcodesFor() throws Exception {
        for (int i = 0; i < barcodes.length; i++) {
            final String[] expected = new String[2];
            Arrays.fill(expected, barcodes[i]);
            Assert.assertEquals(dictionarySingle.getBarcodesFor(i),
                    Arrays.copyOfRange(expected, 0, 1));
            Assert.assertEquals(dictionaryDouble.getBarcodesFor(i), expected);
        }
    }

    @Test
    public void testGetReadGroupFor() throws Exception {
        for (int i = 0; i < barcodes.length; i++) {
            // one barcode
            Assert.assertEquals(dictionarySingle.getReadGroupFor(barcodes[i]), samples.get(i));
            // combined barcode
            final String combinedBarcode = dictionaryDouble.getCombinedBarcodesFor(i);
            Assert.assertEquals(dictionaryDouble.getReadGroupFor(combinedBarcode), samples.get(i));
        }
    }

    @Test(enabled = false, description = "Not implemented")
    public void testGetCombinedBarcodesFor() throws Exception {
        // TODO: make test with new implementation
    }

    @Test(enabled = false, description = "Not implemented")
    public void testIsBarcodeUniqueInAt() throws Exception {
    }

    @Test(enabled = false, description = "Not implemented")
    public void testGetBarcodesFromIndex() throws Exception {
    }

    @Test(enabled = false, description = "Not implemented")
    public void testGetSetBarcodesFromIndex() throws Exception {
    }
}