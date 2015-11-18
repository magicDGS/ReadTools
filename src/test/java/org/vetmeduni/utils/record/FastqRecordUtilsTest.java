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
import junit.framework.TestCase;
import org.junit.*;

/**
 * @author Daniel Gomez-Sanchez
 */
public class FastqRecordUtilsTest extends TestCase {

	static String sangerQuality = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHI";

	static String illuminaQuality = "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefgh";

	FastqRecord illumina1;

	FastqRecord illumina2;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() throws Exception {
		illumina1 = new FastqRecord("Record1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "", illuminaQuality);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Ignore("not implemented")
	public void testCutRecord() throws Exception {
		// TODO
	}

	@Ignore("not implemented")
	public void testCopyToSanger() throws Exception {
		FastqRecord sanger = new FastqRecord("Record1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "", sangerQuality);
		Assert.assertEquals(sanger, FastqRecordUtils.copyToSanger(illumina1));
	}

	@Ignore("not implemented")
	public void testGetBarcodeInName() throws Exception {
		// TODO
	}

	@Ignore("not implemented")
	public void testGetReadNameWithoutBarcode() throws Exception {
		// TODO
	}

	@Ignore("not implemented")
	public void testGetReadNameWithoutBarcode1() throws Exception {
		// TODO
	}

	@Ignore("not implemented")
	public void testChangeBarcode() throws Exception {
		// TODO
	}

	@Ignore("not implemented")
	public void testChangeBarcodeInSingle() throws Exception {
		// TODO
	}

	@Ignore("not implemented")
	public void testChangeBarcodeInPaired() throws Exception {
		// TODO
	}
}