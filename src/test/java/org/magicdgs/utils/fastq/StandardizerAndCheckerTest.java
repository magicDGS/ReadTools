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

import org.magicdgs.utils.record.SAMRecordUtilsTest;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StandardizerAndCheckerTest {

    FastqRecord illuminaFASTQ;

    FastqRecord sangerFASTQ;

    SAMRecord illuminaSAM;

    SAMRecord sangerSAM;

    private static final StandardizerAndChecker sangerChecker =
            new StandardizerAndChecker(FastqQualityFormat.Standard, false);

    private static final StandardizerAndChecker illuminaChecker =
            new StandardizerAndChecker(FastqQualityFormat.Illumina, false);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() {
    }

    @Before
    public void setUp() throws Exception {
        // init FASTQ
        illuminaFASTQ =
                new FastqRecord("Record1#ACGT/1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
                        QualityUtilsTest.illuminaQuality);
        sangerFASTQ =
                new FastqRecord("Record1#ACGT/1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "",
                        QualityUtilsTest.sangerQuality);
        // init SAMR
        illuminaSAM = SAMRecordUtilsTest
                .createSamRecord("Read1#ACTG/1", (byte) 'A', QualityUtilsTest.illuminaQuality);
        sangerSAM = SAMRecordUtilsTest
                .createSamRecord("Read1#ACTG/1", (byte) 'A', QualityUtilsTest.sangerQuality);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCheckMisencoded() throws Exception {
        // Check exceptions
        checkMisencodedException(illuminaFASTQ, FastqQualityFormat.Standard);
        checkMisencodedException(illuminaSAM, FastqQualityFormat.Standard);
        checkMisencodedException(sangerFASTQ, FastqQualityFormat.Illumina);
        checkMisencodedException(sangerSAM, FastqQualityFormat.Illumina);
        // set to 0 the counters and in that case an exception should not be thrown
        sangerChecker.count.set(0);
        illuminaChecker.count.set(0);
        // this should not thrown an exception because of the counter
        sangerChecker.checkMisencoded(illuminaFASTQ);
        sangerChecker.checkMisencoded(illuminaSAM);
        // this should not thrown an exception because of the counter
        illuminaChecker.checkMisencoded(sangerFASTQ);
        illuminaChecker.checkMisencoded(sangerSAM);
    }

    private void checkMisencodedException(Object read, FastqQualityFormat checkerFormat) {
        final StandardizerAndChecker checker;
        switch (checkerFormat) {
            case Standard:
                checker = sangerChecker;
                break;
            case Illumina:
                checker = illuminaChecker;
                break;
            default:
                throw new RuntimeException("Unreachable code");
        }
        try {
            checker.checkMisencoded(read);
            Assert.fail(checkerFormat
                    + ".checkMisencoded does not throw a QualityException if the quality is not correctly formatted");
        } catch (QualityUtils.QualityException e) {
        }
    }

    @Test
    public void testStandardize() throws Exception {
        // checking standardizer for illumina
        Assert.assertEquals(sangerFASTQ, illuminaChecker.standardize(illuminaFASTQ));
        Assert.assertEquals(sangerSAM, illuminaChecker.standardize(illuminaSAM));
        // checking that the standard is not changed
        Assert.assertEquals(sangerFASTQ, sangerChecker.standardize(sangerFASTQ));
        Assert.assertEquals(sangerSAM, sangerChecker.standardize(sangerSAM));
        // checking that an error is thrown if the quality is misencoded
        try {
            illuminaChecker.standardize(sangerFASTQ);
            Assert.fail(
                    "FASTQ Illumina Standardizer does not throw a SAMException if the quality is Sanger");
        } catch (SAMException e) {
        }
        try {
            illuminaChecker.standardize(sangerSAM);
            Assert.fail(
                    "SAM Illumina Standardizer does not throw a SAMException if the quality is Sanger");
        } catch (SAMException e) {
        }
        // this should not throw errors, but return the exactly same result
        Assert.assertEquals(illuminaFASTQ, sangerChecker.standardize(illuminaFASTQ));
        Assert.assertEquals(illuminaSAM, sangerChecker.standardize(illuminaSAM));
    }
}