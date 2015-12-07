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
package org.vetmeduni.methods.barcodes.dictionary;

import htsjdk.samtools.SAMReadGroupRecord;
import org.junit.*;

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
			// TODO: this test will fail because of the combined sample: implement with the new method
			final SAMReadGroupRecord rg = new SAMReadGroupRecord("sample" + i + String.join("", barcodes[i]),
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

	@Before
	public void setUp() throws Exception {
		dictionarySingle = new BarcodeDictionary(samples, barcodesSingle);
		dictionaryDouble = new BarcodeDictionary(samples, barcodesDouble);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetNumberOfBarcodes() throws Exception {
		Assert.assertEquals(1, dictionarySingle.getNumberOfBarcodes());
		Assert.assertEquals(2, dictionaryDouble.getNumberOfBarcodes());
	}

	@Test
	public void testGetSampleNames() throws Exception {
		ArrayList<String> sampleNames = new ArrayList<>();
		for (SAMReadGroupRecord sample : samples) {
			sampleNames.add(sample.getSample());
		}
		Assert.assertEquals(sampleNames, dictionarySingle.getSampleNames());
		Assert.assertEquals(sampleNames, dictionaryDouble.getSampleNames());
	}

	@Test
	public void testGetSampleReadGroups() throws Exception {
		Assert.assertEquals(samples, dictionarySingle.getSampleReadGroups());
		Assert.assertEquals(samples, dictionaryDouble.getSampleReadGroups());
	}

	@Test
	public void testNumberOfSamples() throws Exception {
		Assert.assertEquals(barcodes.length, dictionarySingle.numberOfSamples());
		Assert.assertEquals(barcodes.length, dictionaryDouble.numberOfSamples());
	}

	@Ignore("Not implemented")
	@Test
	public void testNumberOfUniqueSamples() throws Exception {
		// TODO: create a dictionary with repeated samples
	}

	@Test
	public void testGetBarcodesFor() throws Exception {
		for (int i = 0; i < barcodes.length; i++) {
			String[] expected = new String[2];
			Arrays.fill(expected, barcodes[i]);
			Assert.assertArrayEquals(Arrays.copyOfRange(expected, 0, 1), dictionarySingle.getBarcodesFor(i));
			Assert.assertArrayEquals(expected, dictionaryDouble.getBarcodesFor(i));
		}
	}

	@Test
	public void testGetReadGroupFor() throws Exception {
		for (int i = 0; i < barcodes.length; i++) {
			// one barcode
			Assert.assertEquals(samples.get(i), dictionarySingle.getReadGroupFor(barcodes[i]));
			// combined barcode
			String combinedBarcode = dictionaryDouble.getCombinedBarcodesFor(i);
			Assert.assertEquals(samples.get(i), dictionaryDouble.getReadGroupFor(combinedBarcode));
		}
	}

	@Ignore("Not implemented")
	@Test
	public void testGetCombinedBarcodesFor() throws Exception {
		// TODO: make test with new implementation
	}

	@Ignore("Not implemented")
	@Test
	public void testIsBarcodeUniqueInAt() throws Exception {
	}

	@Ignore("Not implemented")
	@Test
	public void testGetBarcodesFromIndex() throws Exception {
	}

	@Ignore("Not implemented")
	@Test
	public void testGetSetBarcodesFromIndex() throws Exception {
	}
}