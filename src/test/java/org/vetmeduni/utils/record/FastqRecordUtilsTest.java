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
package org.vetmeduni.utils.record;

import htsjdk.samtools.fastq.FastqRecord;
import org.junit.*;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.utils.fastq.QualityUtilsTest;

import java.util.Arrays;

/**
 * @author Daniel Gomez-Sanchez
 */
public class FastqRecordUtilsTest {

	FastqRecord illumina1;

	FastqRecord sanger1;

	FastqRecord illumina2;

	FastqRecord sanger2;

	FastqPairedRecord illuminaPaired;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() throws Exception {
		illumina1 = new FastqRecord("Record1#ACGT/1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
			QualityUtilsTest.illuminaQuality);
		illumina2 = new FastqRecord("Record1#ACGT/2", "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT", "",
			QualityUtilsTest.illuminaQuality);
		illuminaPaired = new FastqPairedRecord(illumina1, illumina2);
		sanger1 = new FastqRecord("Record1#ACGT/1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
			QualityUtilsTest.sangerQuality);
		sanger2 = new FastqRecord("Record1#ACGT/2", "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT", "",
			QualityUtilsTest.sangerQuality);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCutRecord() throws Exception {
		int length = QualityUtilsTest.illuminaQuality.length();
		int offset = 5;
		// cutting the end
		char[] a = new char[length-offset];
		Arrays.fill(a, 'A');
		FastqRecord cut1 = FastqRecordUtils.cutRecord(illumina1, 0, length-offset);
		Assert.assertEquals(length-offset, cut1.length());
		Assert.assertEquals(new String(a), cut1.getReadString());
		Assert.assertEquals(QualityUtilsTest.illuminaQuality.charAt(length-offset-1), cut1.getBaseQualityString().charAt(cut1.length()-1));
		Assert.assertEquals(QualityUtilsTest.illuminaQuality.charAt(0), cut1.getBaseQualityString().charAt(0));
		// cutting the start
		FastqRecord cut2 = FastqRecordUtils.cutRecord(illumina1, offset, length);
		Assert.assertEquals(length-offset, cut2.length());
		Assert.assertEquals(new String(a), cut2.getReadString());
		Assert.assertEquals(QualityUtilsTest.illuminaQuality.charAt(length-1), cut2.getBaseQualityString().charAt(cut2.length()-1));
		Assert.assertEquals(QualityUtilsTest.illuminaQuality.charAt(offset), cut2.getBaseQualityString().charAt(0));
		// cutting both start and end
		FastqRecord cut3 = FastqRecordUtils.cutRecord(illumina1, offset, length-offset);
		a = Arrays.copyOf(a, length-offset-offset);
		Assert.assertEquals(length-offset-offset, cut3.length());
		Assert.assertEquals(new String(a), cut3.getReadString());
		Assert.assertEquals(QualityUtilsTest.illuminaQuality.charAt(length-offset-1), cut3.getBaseQualityString().charAt(cut3.length()-1));
		Assert.assertEquals(QualityUtilsTest.illuminaQuality.charAt(offset), cut3.getBaseQualityString().charAt(0));
		// null cuts
		Assert.assertNull(FastqRecordUtils.cutRecord(illumina1, length, 0));
		Assert.assertNull(FastqRecordUtils.cutRecord(illumina1, 5, 5));
	}

	@Test
	@Ignore("Deprecated method")
	public void testCopyToSanger() throws Exception {
		FastqRecord sangerCopy1 = FastqRecordUtils.copyToSanger(illumina1);
		FastqRecord sangerCopy2 = FastqRecordUtils.copyToSanger(illumina2);
		FastqPairedRecord sangerPaired = FastqRecordUtils.copyToSanger(illuminaPaired);
		Assert.assertEquals(sanger1, sangerCopy1);
		Assert.assertEquals(sanger2, sangerCopy2);
		Assert.assertEquals(new FastqPairedRecord(sanger1, sanger2), sangerPaired);
	}

	@Test
	public void testGetBarcodeInName() throws Exception {
		String barcodeSingle = FastqRecordUtils.getBarcodeInName(sanger1);
		Assert.assertEquals("ACGT", barcodeSingle);
		String barcodePaired = FastqRecordUtils.getBarcodeInName(illuminaPaired);
		Assert.assertEquals("ACGT", barcodePaired);
	}

	@Test
	public void testGetReadNameWithoutBarcode() throws Exception {
		String readNameSingle = FastqRecordUtils.getReadNameWithoutBarcode(sanger1);
		Assert.assertEquals("Record1", readNameSingle);
		String readNamePaired = FastqRecordUtils.getReadNameWithoutBarcode(illuminaPaired);
		Assert.assertEquals("Record1", readNamePaired);
	}

	@Test
	public void testChangeBarcode() throws Exception {
		FastqRecord singleChanged = FastqRecordUtils.changeBarcodeInSingle(sanger1, "TTTT");
		Assert.assertEquals(singleChanged.getReadHeader(), "Record1#TTTT/0");
		singleChanged = FastqRecordUtils.changeBarcode(sanger1, "AAA", 1);
		Assert.assertEquals(singleChanged.getReadHeader(), "Record1#AAA/1");
		FastqPairedRecord pairedChanged = FastqRecordUtils.changeBarcodeInPaired(illuminaPaired, "TTT");
		Assert.assertEquals("Record1#TTT/1", pairedChanged.getRecord1().getReadHeader());
		Assert.assertEquals("Record1#TTT/2", pairedChanged.getRecord2().getReadHeader());
	}
}