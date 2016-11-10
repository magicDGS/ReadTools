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

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMReadGroupRecord;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BarcodeDictionaryTest extends BaseTest {

    private static BarcodeDictionary dictionarySingle, dictionaryDouble;

    private static final List<SAMReadGroupRecord> samples = new ArrayList<>();

    private static final List<List<String>> barcodesSingle = new ArrayList<>(1);

    private static final List<List<String>> barcodesDouble = new ArrayList<>(2);

    private static final String[] barcodes = new String[] {"AAAA", "CCCC", "TTTT", "GGGG"};

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
        final List<String> sampleNames = samples.stream().map(SAMReadGroupRecord::getSample)
                .collect(Collectors.toList());
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
}