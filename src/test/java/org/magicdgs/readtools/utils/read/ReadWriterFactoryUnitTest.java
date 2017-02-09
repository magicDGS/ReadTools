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

package org.magicdgs.readtools.utils.read;

import org.magicdgs.readtools.exceptions.RTUserExceptions;
import org.magicdgs.readtools.utils.read.writer.FastqGATKWriter;
import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.SAMFileGATKReadWriter;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadWriterFactoryUnitTest extends BaseTest {

    // temp directory for this tests
    private final File testDir = createTestTempDir(this.getClass().getSimpleName());

    @DataProvider(name = "namesAndClass")
    public Object[][] outpueNamesProvider() {
        return new Object[][] {
                {new File(testDir, "example.bam"), SAMFileGATKReadWriter.class},
                {new File(testDir, "example.sam"), SAMFileGATKReadWriter.class},
                // TODO: uncomment by adding a known reference file
                // {new File(testDir, "example.cram"), SAMFileGATKReadWriter.class},
                {new File(testDir, "example.fq"), FastqGATKWriter.class},
                {new File(testDir, "example.fq.gz"), FastqGATKWriter.class},
                {new File(testDir, "example.fastq"), FastqGATKWriter.class},
                {new File(testDir, "example.fastq.gz"), FastqGATKWriter.class}
        };
    }

    @Test(dataProvider = "namesAndClass")
    public void testCorrectGATKWriter(final File outputFile,
            final Class<? extends GATKReadWriter> writerClass) {
        Assert.assertFalse(outputFile.exists());
        // TODO: add a FASTA file as reference to test CRAM writer creation
        Assert.assertEquals(new ReadWriterFactory()
                        .createWriter(outputFile.getAbsolutePath(), new SAMFileHeader(), true).getClass(),
                writerClass);
        Assert.assertTrue(outputFile.exists());
        outputFile.delete();
    }

    @Test(expectedExceptions = UserException.MissingReference.class)
    public void testCramFailingWithoutReference() {
        new ReadWriterFactory().createWriter("example.cram", new SAMFileHeader(), true);
    }

    @Test(expectedExceptions = UserException.CouldNotCreateOutputFile.class)
    public void testCramFailingWithNonExistingReference() {
        new ReadWriterFactory()
                .setReferenceFile(new File("notExisting.fasta"))
                .createWriter(new File(testDir, "example.cram").getAbsolutePath(),
                        new SAMFileHeader(), true);
    }

    @Test(expectedExceptions = RTUserExceptions.OutputFileExists.class)
    public void testExistantFileBlowsUp() throws Exception {
        final File existantFile = new File(testDir, "exists.sam");
        Assert.assertTrue(existantFile.createNewFile(), "unable to create test file");
        new ReadWriterFactory()
                .createWriter(existantFile.getAbsolutePath(), new SAMFileHeader(), true);
    }

}