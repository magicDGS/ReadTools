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
package org.magicdgs.utils.fastq;

import htsjdk.samtools.util.FastqQualityFormat;
import org.junit.*;

import java.util.Arrays;

public class QualityUtilsTest {

	/**
	 * All the Sanger qualities (ASCII)
	 */
	public static final String sangerQuality = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHI";

	/**
	 * All the Illumina qualities (ASCII)
	 */
	public static final String illuminaQuality = "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefgh";

	/**
	 * All the qualities in Phred equivalent to the ASCII code (both Illumina and Sanger)
	 */
	public static final byte[] quality = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
		19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40};

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
		for (int i = 0; i < quality.length; i++) {
			Assert.assertEquals(quality[i],
				QualityUtils.getQuality(sangerQuality.charAt(i), FastqQualityFormat.Standard));
			Assert.assertEquals(quality[i],
				QualityUtils.getQuality(illuminaQuality.charAt(i), FastqQualityFormat.Illumina));
		}
		// check the 41 quality for the sanger Illumina 1.8+
		Assert.assertEquals(41, QualityUtils.getQuality('J', FastqQualityFormat.Standard));
	}

	@Test
	public void testCheckEncoding() throws Exception {
		// first check correct qualities
		try {
			for (byte b : sangerQuality.getBytes()) {
				QualityUtils.checkEncoding(b, FastqQualityFormat.Standard, false);
			}
			for (byte b : illuminaQuality.getBytes()) {
				QualityUtils.checkEncoding(b, FastqQualityFormat.Illumina, false);
			}
		} catch (QualityUtils.QualityException e) {
			Assert.fail(e.getMessage());
		}
		// now check if it does not raise an error in the not shared qualities
		byte[] sangerBytesToTest = Arrays.copyOfRange(sangerQuality.getBytes(), 0, sangerQuality.length() - 10);
		for (byte b : sangerBytesToTest) {
			try {
				QualityUtils.checkEncoding(b, FastqQualityFormat.Illumina, false);
				Assert.fail(
					"QualityException is not thrown for quality " + b + " (" + (char) b + ") in Illumina encoding");
			} catch (QualityUtils.QualityException e) {
			}
		}
		byte[] illuminaBytesToTest = Arrays.copyOfRange(illuminaQuality.getBytes(), 11, illuminaQuality.length());
		for (byte b : illuminaBytesToTest) {
			try {
				QualityUtils.checkEncoding(b, FastqQualityFormat.Standard, false);
				Assert
					.fail("QualityException is not thrown for quality " + b + " (" + (char) b + ") in Sanger encoding");
			} catch (QualityUtils.QualityException e) {
			}
		}
		// check if allow higher qualities is working
		for (byte b : illuminaBytesToTest) {
			try {
				QualityUtils.checkEncoding(b, FastqQualityFormat.Standard, true);
			} catch (QualityUtils.QualityException e) {
				Assert.fail("QualityException is thrown for quality " + b + " (" + (char) b + ") in Sanger encoding when allowing higher qualities");
			}
		}
	}

	@Ignore("We will need BAM/FASTQ files in resources for this test")
	@Test
	public void testGetEncoding() throws Exception {
		// TODO: implement
	}
}