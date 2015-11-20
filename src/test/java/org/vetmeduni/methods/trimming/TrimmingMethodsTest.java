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
package org.vetmeduni.methods.trimming;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.junit.*;

import static org.vetmeduni.methods.trimming.TrimmingMethods.*;

public class TrimmingMethodsTest {

	// the default encoding for testing
	private static FastqQualityFormat encoding;

	private static FastqRecord createFakeRecord(String sequence, String quality) {
		// the record will have always the same headers (important for comparison)
		return new FastqRecord("Record1", sequence, "", quality);
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// we will do the tests with Illumina encoding
		encoding = FastqQualityFormat.Illumina;
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTrimNs() throws Exception {
		String withoutNs = "AAAAACCCC";
		// create a record with 3 Ns in the edges
		FastqRecord recordWithNs = new FastqRecord("Record1", "NNN" + withoutNs + "NNN", "", "BBB" + withoutNs + "BBB");
		// check trimming 5', 3' all and if the boolean perform the same
		Assert.assertEquals(withoutNs + "NNN", trim5pNs(recordWithNs).getReadString());
		Assert.assertEquals("NNN" + withoutNs, trim3pNs(recordWithNs).getReadString());
		Assert.assertEquals(withoutNs, trimNs(recordWithNs).getReadString());
		Assert.assertEquals(trim3pNs(recordWithNs), trimNs(recordWithNs, true));
	}

	@Test
	public void testTrimQualityMott() throws Exception {
		// no trimmed read with quality 19, trimmed with 20 to TT (only if 5' set) and null with 21
		FastqRecord record = createFakeRecord("AAAATT", "TTTTUU");
		Assert.assertEquals(record, trimQualityMott(record, encoding, 19));
		Assert.assertEquals(createFakeRecord("TT", "UU"), trimQualityMott(record, encoding, 20));
		Assert.assertEquals(record, trimQualityMott(record, encoding, 20, true));
		Assert.assertNull(trimQualityMott(record, encoding, 21));
		Assert.assertNull(trimQualityMott(record, encoding, 21, true));
	}
}