/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
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

import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class QualityEncodingDetectorIntegrationTest extends CommandLineProgramTest {

    @Test(expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testBadArgument() throws Exception {
        // TODO: change when SMALL_FASTQ_1 doees not exists anymore
        runCommandLine(
                Arrays.asList("-I", SMALL_FASTQ_1.getAbsolutePath(), "--maximumReads", "-1"));
    }

    @DataProvider(name = "notExistingFiles")
    public Object[][] getNotExisting() {
        return new Object[][] {{"doesNotExists.bam"}, {"doesNotExists.fq"}};
    }

    @Test(dataProvider = "notExistingFiles", expectedExceptions = UserException.CouldNotReadInputFile.class)
    public void testFileDoesNotExists(final String fileName) throws Exception {
        // TODO: change when SMALL_FASTQ_1 doees not exists anymore
        runCommandLine(Arrays.asList("-I", fileName));
    }

    // TODO: generate a new data provider for this tool
    // TODO: this is a very simple test for check that we are not changing anything
    // TODO: but it is not required anymore
    @DataProvider(name = "smallFiles")
    public static Object[][] getTestFiles() {
        return new Object[][] {
                // test FASTQ file
                {SMALL_FASTQ_1, FastqQualityFormat.Standard},
                {getInputDataFile("small.illumina.fq"), FastqQualityFormat.Illumina},
                // test SAM files
                {PAIRED_BAM_FILE, FastqQualityFormat.Standard},
                {getInputDataFile("small.illumina.sam"), FastqQualityFormat.Illumina}
        };
    }

    @Test(dataProvider = "smallFiles")
    public void testQualityChecker(final File file, final FastqQualityFormat expectedFormat) {
        final Object format = runCommandLine(Arrays.asList("-I", file.getAbsolutePath()));
        Assert.assertEquals(format, expectedFormat);
    }

}