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

package org.magicdgs.readtools.utils.barcodes.legacy.dictionary;

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.utils.barcodes.legacy.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.SAMReadGroupRecord;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BarcodeDictionaryTest extends RTBaseTest {

    private static BarcodeDictionary dictionarySingle, dictionaryDouble;

    private static final List<SAMReadGroupRecord> samples = new ArrayList<>();

    private static final List<List<String>> barcodesSingle = new ArrayList<>(1);

    private static final List<List<String>> barcodesDouble = new ArrayList<>(2);

    private static final String[] barcodes = new String[] {"AAAA", "CCCC", "TTTT", "GGGG"};

    // the unknown read group to test
    private static final SAMReadGroupRecord UNKNOWN_READGROUP_INFO;

    // initialize the unknown read group information
    static {
        UNKNOWN_READGROUP_INFO = new SAMReadGroupRecord(BarcodeMatch.UNKNOWN_STRING);
        UNKNOWN_READGROUP_INFO.setProgramGroup(RTHelpConstants.PROGRAM_NAME);
        UNKNOWN_READGROUP_INFO.setSample(BarcodeMatch.UNKNOWN_STRING);
    }


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        barcodesSingle.add(new ArrayList<>());
        barcodesDouble.add(new ArrayList<>());
        barcodesDouble.add(new ArrayList<>());
        for (int i = 0; i < barcodes.length; i++) {
            final SAMReadGroupRecord rg =
                    new SAMReadGroupRecord("sample" + i + String.join("_", barcodes[i]),
                            UNKNOWN_READGROUP_INFO);
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
        dictionarySingle = new BarcodeDictionary(samples, barcodesSingle, UNKNOWN_READGROUP_INFO);
        dictionaryDouble = new BarcodeDictionary(samples, barcodesDouble, UNKNOWN_READGROUP_INFO);
    }

    @Test
    public void testGetMaxNumberOfIndexes() throws Exception {
        Assert.assertEquals(dictionarySingle.getMaxNumberOfIndexes(), 1);
        Assert.assertEquals(dictionaryDouble.getMaxNumberOfIndexes(), 2);
    }

    @Test
    public void testAsReadGroupList() throws Exception {
        Assert.assertEquals(dictionarySingle.asReadGroupList(), samples);
        Assert.assertEquals(dictionaryDouble.asReadGroupList(), samples);
    }

    @Test
    public void testSize() throws Exception {
        Assert.assertEquals(dictionarySingle.size(), barcodes.length);
        Assert.assertEquals(dictionaryDouble.size(), barcodes.length);
    }

    @Test
    public void testGetBarcodesForSample() throws Exception {
        for (int i = 0; i < barcodes.length; i++) {
            final String[] expected = new String[2];
            Arrays.fill(expected, barcodes[i]);
            Assert.assertEquals(dictionarySingle.getAllBarcodesForSample(i),
                    Arrays.asList(Arrays.copyOfRange(expected, 0, 1)));
            Assert.assertEquals(dictionaryDouble.getAllBarcodesForSample(i), Arrays.asList(expected));
        }
    }

    @Test
    public void testGetReadGroupForJoinedBarcode() throws Exception {
        for (int i = 0; i < barcodes.length; i++) {
            // one barcode
            Assert.assertEquals(dictionarySingle.getReadGroupForJoinedBarcode(barcodes[i]), samples.get(i));
            // combined barcode
            final String combinedBarcode = dictionaryDouble.getJoinedBarcodesForSample(i);
            Assert.assertEquals(dictionaryDouble.getReadGroupForJoinedBarcode(combinedBarcode), samples.get(i));
        }
    }
}