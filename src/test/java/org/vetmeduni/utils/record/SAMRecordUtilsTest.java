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

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import junit.framework.TestCase;
import org.junit.*;

/**
 * @author Daniel Gomez-Sanchez
 */
public class SAMRecordUtilsTest extends TestCase {

	static String illuminaQuality = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHI";

	static String sangerQuality = "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefgh";

	SAMRecord illuminaRecord;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() throws Exception {
		SAMFileHeader header = new SAMFileHeader();
		illuminaRecord = new SAMRecord(header);
		illuminaRecord.setBaseQualityString(illuminaQuality);
	}

	@After
	public void tearDown() throws Exception {
	}

	public void testToFastqRecord() throws Exception {
		// TODO
	}

	public void testAssertPairedMates() throws Exception {
		// TODO
	}

	public void testAddBarcodeToName() throws Exception {
		// TODO
	}

	public void testCopyToSanger() throws Exception {
		SAMFileHeader header = new SAMFileHeader();
		SAMRecord sangerRecord = new SAMRecord(header);
		sangerRecord.setBaseQualityString(sangerQuality);
		Assert.assertArrayEquals(sangerRecord.getBaseQualities(),
			SAMRecordUtils.copyToSanger(illuminaRecord).getBaseQualities());
	}

	public void testToSanger() throws Exception {
		// TODO
	}

	public void testGetBarcodeInName() throws Exception {
		// TODO
	}

	public void testGetReadNameWithoutBarcode() throws Exception {
		// TODO
	}
}