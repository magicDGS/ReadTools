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

package org.magicdgs.readtools.tools.barcodes;

import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BamBarcodeDetectorIntegrationTest extends BarcodeToolsIntegrationTests {

    @Test
    public void testBamBarcodeDetector() throws Exception {
        final String testName = "testBamBarcodeDetector";
        final File outputPath = new File(classTempDirectory, testName);
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("barcodes", UNIQUE_BARCODE_FILE.getAbsolutePath())
                .addArgument("input", getInputDataFile("example.mapped.sam").getAbsolutePath())
                .addArgument("output", outputPath.getAbsolutePath());
        runCommandLine(args);
        checkExpectedSharedFiles(testName, outputPath.getAbsolutePath(), ".metrics");
        checkBamFile(testName, outputPath.getAbsolutePath(), ".bam");
        checkBamFile(testName, outputPath.getAbsolutePath(), "_discarded.bam");
    }

    /** Check two expected BAM files. */
    private void checkBamFile(final String testName, final String actualPrefix,
            final String suffix) throws Exception {
        logger.debug("Checking output: {}{}", testName, suffix);
        final String expectedPrefix = getBarcodeToolsExpectedData(testName).getAbsolutePath();
        // open readers
        final SamReader expectedReader = SamReaderFactory.makeDefault()
                .open(new File(expectedPrefix + suffix));
        final SamReader actualReader = SamReaderFactory.makeDefault()
                .open(new File(actualPrefix + suffix));
        // only read groups in the headers
        Assert.assertEquals(expectedReader.getFileHeader().getReadGroups(),
                actualReader.getFileHeader().getReadGroups());
        // checks the reads
        final SAMRecordIterator it = actualReader.iterator();
        expectedReader.iterator().stream().forEach(r -> Assert.assertEquals(it.next(), r));
        // close the readers
        expectedReader.close();
        actualReader.close();
    }

}