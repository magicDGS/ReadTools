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

import org.magicdgs.readtools.TestResourcesUtils;
import org.magicdgs.readtools.exceptions.RTUserExceptions;
import org.magicdgs.readtools.utils.fastq.FastqGATKWriter;
import org.magicdgs.readtools.utils.read.writer.NullGATKWriter;
import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.SAMFileGATKReadWriter;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.broadinstitute.hellbender.utils.test.MiniClusterUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadWriterFactoryUnitTest extends RTBaseTest {

    // this is the test read with 5 bases (default one)
    private final static GATKRead DEFAULT_READ_TO_TEST = ArtificialReadUtils
            .createArtificialRead("5M");

    // temp directory for this tests
    private final File testDir = createTempDir(this.getClass().getSimpleName());

    @DataProvider(name = "namesAndClass")
    public Object[][] outpueNamesProvider() {
        return new Object[][] {
                {new File(testDir, "example.bam"), SAMFileGATKReadWriter.class},
                {new File(testDir, "example.sam"), SAMFileGATKReadWriter.class},
                {new File(testDir, "example.cram"), SAMFileGATKReadWriter.class},
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
        outputFile.deleteOnExit();
        Assert.assertEquals(new ReadWriterFactory()
                        .setReferencePath(TestResourcesUtils.getWalkthroughDataFile("2L.fragment.fa").toPath())
                        .createWriter(outputFile.getAbsolutePath(), new SAMFileHeader(), true).getClass(),
                writerClass);
        Assert.assertTrue(outputFile.exists());
    }

    @Test(expectedExceptions = UserException.MissingReference.class)
    public void testCramFailingWithoutReference() {
        new ReadWriterFactory().createWriter("example.cram", new SAMFileHeader(), true);
    }

    @Test(expectedExceptions = UserException.CouldNotCreateOutputFile.class)
    public void testCramFailingWithNonExistingReference() {
        new ReadWriterFactory()
                .setReferencePath(new File("notExisting.fasta").toPath())
                .createWriter(new File(testDir, "notExisting.example.cram").getAbsolutePath(),
                        new SAMFileHeader(), true);
    }

    @Test
    public void testCramWithHdfsReference() throws Exception {
        // create mini cluster
        final MiniDFSCluster cluster =  MiniClusterUtils.getMiniCluster();
        try {
            // first copy the reference to the mini cluster
            final Path localRef = TestResourcesUtils.getWalkthroughDataFile("2L.fragment.fa").toPath();
            final Path hdfsRef = IOUtils.getPath(cluster.getFileSystem().getUri().toString()
                            + "/2L.fragment.fa");
            Files.copy(localRef, hdfsRef);

            // create a writer should not fail
            final File output = new File(testDir, "hdfs.example.cram");
            final GATKReadWriter writer = new ReadWriterFactory().setReferencePath(hdfsRef)
                            .createWriter(output.getAbsolutePath(),
                                    new SAMFileHeader(), true);
            // neither closing it
            writer.close();

            // and the file should exists (with no reads)
            Assert.assertTrue(output.exists());
        } finally {
            // always stop the mini-cluster
            MiniClusterUtils.stopCluster(cluster);
        }
    }

    @Test(expectedExceptions = RTUserExceptions.OutputFileExists.class)
    public void testExistantFileBlowsUp() throws Exception {
        final File existantFile = new File(testDir, "exists.sam");
        Assert.assertTrue(existantFile.createNewFile(), "unable to create test file");
        new ReadWriterFactory()
                .createWriter(existantFile.getAbsolutePath(), new SAMFileHeader(), true);
    }

    @Test
    public void testNonAbsolute() throws Exception {
        final String nonAbsolute = "nonAbsolute.fq";
        final File file = new File(nonAbsolute);
        file.deleteOnExit();
        Assert.assertFalse(file.exists(), "test implementation error");
        new ReadWriterFactory()
                .createWriter(nonAbsolute, null, true);
        Assert.assertTrue(file.exists(), "file was not generated");
    }

    @Test(expectedExceptions = UserException.CouldNotCreateOutputFile.class)
    public void testIOException() throws Exception {
        final File fileAsDirectory = new File(testDir, "fileAsDirectory");
        fileAsDirectory.deleteOnExit();
        fileAsDirectory.createNewFile();
        new ReadWriterFactory()
                .createWriter(new File(fileAsDirectory, "example.fq").toString(), null, true);
    }

    @DataProvider(name = "defaultReadFiles")
    public Object[][] getFilesForMd5() {
        return new Object[][] {
                {getTestFile("singleRead.fq")},
                // the level of compression 5 is the default in IOUtil
                {getTestFile("singleRead.fq.gz")}
        };
    }

    // the md5 should change with the compression
    @Test(dataProvider = "defaultReadFiles")
    public void testMd5AndCompressionLevel(final File expectedFile)
            throws Exception {
        // creates a file to test
        final File writedFile = new File(testDir, expectedFile.getName());

        // open the writer
        final GATKReadWriter writer = new ReadWriterFactory()
                .setCreateMd5File(true)
                .createFASTQWriter(writedFile.getAbsolutePath());

        // write and close the writer
        writer.addRead(DEFAULT_READ_TO_TEST);
        writer.close();

        // now check the output files
        IntegrationTestSpec.assertEqualTextFiles(writedFile, expectedFile);
        // and the MD5
        IntegrationTestSpec.assertEqualTextFiles(
                new File(writedFile.getAbsolutePath() + ".md5"),
                new File(expectedFile.getAbsolutePath() + ".md5"));
    }

    @DataProvider(name = "allSetterValues")
    public Iterator<Object[]> allSetterValues() {
        final File tempDir = createTempDir("temp_directory");
        final List<Object[]> data = new ArrayList<>();
        final boolean[] trueOrFalse = new boolean[] {true, false};
        for (final boolean useAsyncIo : trueOrFalse) {
            for (final boolean createMd5File : trueOrFalse) {
                for (final boolean createIndex : trueOrFalse) {
                    final int maxRecordsInRam = 2;
                    final int asyncOutputBufferSize = 10;
                    final int bufferSize = 10;
                    // bam files
                    data.add(new Object[] {useAsyncIo, createMd5File, createIndex,
                            maxRecordsInRam, tempDir, true,
                            asyncOutputBufferSize, bufferSize});
                    // only test FASTQ output for no asynchronious writing
                    // TODO: this limitation comes from a non-deterministic exception of AsyncFastqWriter
                    // TODO: it is very difficult to debug due to the uninformative error message
                    if (!useAsyncIo) {
                        data.add(new Object[] {useAsyncIo, createMd5File, createIndex,
                                maxRecordsInRam, tempDir, false,
                                asyncOutputBufferSize, bufferSize});
                    }
                }
            }

        }
        return data.iterator();
    }

    @Test(dataProvider = "allSetterValues")
    public void testWriteDefaultReadWithAllSetters(
            final boolean useAsyncIo,
            final boolean createMd5File,
            final boolean createIndex,
            final int maxRecordsInRam,
            final File tmpDir,
            final boolean bam,
            final int asyncOutputBufferSize,
            final int bufferSize)
            throws Exception {
        // get the factory
        final ReadWriterFactory factory = new ReadWriterFactory()
                .setUseAsyncIo(useAsyncIo)
                .setCreateMd5File(createMd5File)
                .setCreateIndex(createIndex)
                .setMaxRecordsInRam(maxRecordsInRam)
                .setTempDirectory(tmpDir)
                .setAsyncOutputBufferSize(asyncOutputBufferSize)
                .setBufferSize(bufferSize);

        final File outputFile = (bam) ? new File(tmpDir, factory.toString() + ".bam")
                : new File(tmpDir, factory.toString() + ".fq");
        final File samIndex = new File(outputFile.toString().replace(".bam", ".bai"));

        // get the writers, write the default record and close them
        final GATKReadWriter writer = factory.createWriter(outputFile.toString(), ArtificialReadUtils.createArtificialSamHeader(), true);
        writer.addRead(DEFAULT_READ_TO_TEST);
        writer.close();

        // check existence of the file and the MD5 if requested
        Assert.assertTrue(outputFile.exists(), "output");
        Assert.assertEquals(new File(outputFile.getAbsolutePath() + ".md5").exists(),
                createMd5File, "md5");
        // check if the SAM index was created if createIndex is specified only if it is a bam
        if (bam) {
            Assert.assertEquals(samIndex.exists(), createIndex);
        }
    }

    @DataProvider(name = "writersToClose")
    public Object[][] getWritersToClose() {
        return new Object[][] {{null}, {new NullGATKWriter()}};
    }

    @Test(dataProvider = "writersToClose")
    public void testCloseCorrectlyWriter(final GATKReadWriter writer) throws Exception {
        ReadWriterFactory.closeWriter(writer);
    }

    @DataProvider(name = "closingException")
    public Object[][] getClosingExceptions() throws Exception {
        return new Object[][] {
                {IOException.class, true},
                {FileNotFoundException.class, true},
                {NullPointerException.class, false},
                {IllegalStateException.class, false}
        };
    }

    @Test(dataProvider = "closingException")
    public void failWhileClosingWriter(final Class<Exception> exceptionClass,
            final boolean isHandled) throws Exception {
        // test using a mocked writer, which the close method throws the provided exception
        final GATKReadWriter mocked = Mockito.mock(GATKReadWriter.class);
        Mockito.doThrow(exceptionClass).when(mocked).close();
        if (isHandled) {
            // handled exceptions throws an user exception
            Assert.assertThrows(UserException.CouldNotCreateOutputFile.class,
                    () -> ReadWriterFactory.closeWriter(mocked));
        } else {
            // otherwise, they do not catch and throw the one by the method call
            Assert.assertThrows(exceptionClass,
                    () -> ReadWriterFactory.closeWriter(mocked));
        }
    }

}