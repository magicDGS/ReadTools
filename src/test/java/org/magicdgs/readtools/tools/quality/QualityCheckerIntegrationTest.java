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
 * SOFTWARE.
 */

package org.magicdgs.readtools.tools.quality;

import org.magicdgs.readtools.utils.tests.CommandLineProgramTest;

import org.broadinstitute.hellbender.exceptions.UserException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class QualityCheckerIntegrationTest extends CommandLineProgramTest {

    @DataProvider(name = "smallFiles")
    public Object[][] getTestFiles() {
        return new Object[][] {
                // test FASTQ file
                {SMALL_FASTQ_1, "Sanger"},
                {getInputDataFile("small.illumina.fq"), "Illumina"},
                // test SAM files
                {PAIRED_BAM_FILE, "Sanger"},
                {getInputDataFile("small.illumina.sam"), "Illumina"}
        };
    }

    @Test(expectedExceptions = UserException.BadArgumentValue.class)
    public void testBadArgument() throws Exception {
        runCommandLine(
                Arrays.asList("-i", SMALL_FASTQ_1.getAbsolutePath(), "--maximum-reads", "-1"));
    }

    @Test(dataProvider = "smallFiles")
    public void testQualityChecker(final File file, final String expectedFormat) {
        final Object format = runCommandLine(Arrays.asList("-i", file.getAbsolutePath()));
        Assert.assertEquals(format, expectedFormat);
    }


}