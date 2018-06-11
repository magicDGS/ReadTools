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

import org.magicdgs.readtools.RTCommandLineProgramTest;
import org.magicdgs.readtools.TestResourcesUtils;

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
public class QualityEncodingDetectorIntegrationTest extends RTCommandLineProgramTest {

    @Test(expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testBadArgument() throws Exception {
        runCommandLine(
                Arrays.asList("-I", TestResourcesUtils.getWalkthroughDataFile("illumina_legacy.single_index.SE.fq").getAbsolutePath(), "--maximumReads", "-1"));
    }

    @DataProvider(name = "notExistingFiles")
    public Object[][] getNotExisting() {
        return new Object[][] {{getSafeNonExistentFile("doesNotExists.bam")}, {getSafeNonExistentFile("doesNotExists.fq")}};
    }

    @Test(dataProvider = "notExistingFiles", expectedExceptions = UserException.CouldNotReadInputFile.class)
    public void testFileDoesNotExists(final File fileName) throws Exception {
        runCommandLine(Arrays.asList("-I", fileName.getAbsolutePath().toString()));
    }

    @DataProvider(name = "filesWithQualities")
    public static Object[][] getTestFiles() {
        return new Object[][] {
                // test FASTQ file
                {TestResourcesUtils.getWalkthroughDataFile("casava.single_index.SE.fq"), FastqQualityFormat.Standard},
                {TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.interleaved.fq"), FastqQualityFormat.Standard},
                {TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.illumina_quality.SE.fq"), FastqQualityFormat.Illumina},
                // test SAM files
                {TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.SE.sam"), FastqQualityFormat.Standard},
                {TestResourcesUtils.getWalkthroughDataFile("misencoded.single_index.SE.sam"), FastqQualityFormat.Illumina},
                // mapped BAM file
                {TestResourcesUtils.getWalkthroughDataFile("legacy.dual_index.paired.mapped.bam"), FastqQualityFormat.Standard}
        };
    }

    @Test(dataProvider = "filesWithQualities")
    public void testQualityChecker(final File file, final FastqQualityFormat expectedFormat) {
        final Object format = runCommandLine(Arrays.asList("-I", file.getAbsolutePath()));
        Assert.assertEquals(format, expectedFormat);
    }

}