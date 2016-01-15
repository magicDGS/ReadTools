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

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.junit.*;

public class StandardizerAndCheckerTest {

	FastqRecord illumina1;

	FastqRecord sanger1;

	FastqRecord illumina2;

	FastqRecord sanger2;

	StandardizerAndChecker sangerChecker;

	StandardizerAndChecker illuminaChecker;

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
		sanger1 = new FastqRecord("Record1#ACGT/1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
			QualityUtilsTest.sangerQuality);
		sanger2 = new FastqRecord("Record1#ACGT/2", "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT", "",
			QualityUtilsTest.sangerQuality);
		// init the checkers
		sangerChecker = new StandardizerAndChecker(FastqQualityFormat.Standard);
		illuminaChecker = new StandardizerAndChecker(FastqQualityFormat.Illumina);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	@Ignore("Not implemented")
	public void testCheckMisencoded() throws Exception {
		// TODO: more tests
		// check if checkMisencoded throws an error if the counter is high
		try {
			sangerChecker.count = StandardizerAndChecker.frequency + 1;
			sangerChecker.checkMisencoded(illumina1);
			Assert.fail(
				"Sanger checkMisencoded does not throw a QualityException if the quality is Illumina and it is checking");
		} catch (QualityUtils.QualityException e) {
		}
		// this should not thrown an exception because of the counter
		sangerChecker.checkMisencoded(illumina1);
		// check if checkMisencoded throws an error if the counter is high
		try {
			illuminaChecker.count = StandardizerAndChecker.frequency + 1;
			illuminaChecker.checkMisencoded(sanger1);
			Assert.fail(
				"Sanger checkMisencoded does not throw a QualityException if the quality is Illumina and it is checking");
		} catch (QualityUtils.QualityException e) {
		}
		// this should not thrown an exception because of the counter
		illuminaChecker.checkMisencoded(sanger1);
	}

	@Test
	public void testStandardize() throws Exception {
		// checking standardizer for illumina
		FastqRecord standard1 = illuminaChecker.standardize(illumina1);
		FastqRecord standard2 = illuminaChecker.standardize(illumina2);
		Assert.assertEquals(sanger1, standard1);
		Assert.assertEquals(sanger2, standard2);
		// checking that the standard is not changed
		FastqRecord copySanger1 = sangerChecker.standardize(sanger1);
		FastqRecord copySanger2 = sangerChecker.standardize(sanger2);
		Assert.assertEquals(sanger1, copySanger1);
		Assert.assertEquals(sanger2, copySanger2);
		// checking that an error is thrown if the quality is misencoded
		try {
			illuminaChecker.standardize(sanger1);
			Assert.fail("Illumina Standardizer does not throw a QualityException if the quality is Sanger");
		} catch (QualityUtils.QualityException e) {
		}
		// check if checkMisencoded throws an error if the counter is high
		try {
			sangerChecker.count = StandardizerAndChecker.frequency + 1;
			sangerChecker.standardize(illumina1);
			Assert.fail(
				"Sanger Standardize does not throw a QualityException if the quality is Illumina and it is checking");
		} catch (QualityUtils.QualityException e) {
		}
		// this should not thrown an exception because of the counter
		sangerChecker.standardize(illumina1);
	}
}