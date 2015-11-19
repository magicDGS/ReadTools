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
package org.vetmeduni.utils.fastq;

import htsjdk.samtools.util.FastqQualityFormat;
import org.junit.*;

public class QualityUtilsTest {

	/**
	 * All the Sanger qualities (ASCII)
	 */
	public static String sangerQuality = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHI";

	/**
	 * All the Illumina qualities (ASCII)
	 */
	public static String illuminaQuality = "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefgh";

	/**
	 * All the qualities in Phred equivalent to the ASCII code (both Illumina and Sanger)
	 */
	public static byte[] quality = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
		21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40};

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
	public void testGetQuality() throws Exception {
		for (int i = 1; i < quality.length; i++) {
			Assert.assertEquals(quality[i],
				QualityUtils.getQuality(sangerQuality.charAt(i), FastqQualityFormat.Standard));
			Assert.assertEquals(quality[i],
				QualityUtils.getQuality(illuminaQuality.charAt(i), FastqQualityFormat.Illumina));
		}
	}

	@Ignore("We will need BAM/FASTQ files in resources for this test")
	@Test
	public void testGetEncoding() throws Exception {
	}
}