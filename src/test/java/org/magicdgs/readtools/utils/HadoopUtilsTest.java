/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.utils;

import org.magicdgs.readtools.RTBaseTest;

import hdfs.jsr203.HadoopPath;
import htsjdk.samtools.Defaults;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.test.MiniClusterUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HadoopUtilsTest extends RTBaseTest {

    private static final CompressorStreamFactory APACHE_COMPRESSOR_FACTORY = new CompressorStreamFactory();
    // this is a random text file generated with bash to use for the tests
    private final File randomFile = getTestFile("random_file.txt");
    private MiniDFSCluster cluster;


    // init the cluster and copy the files there
    @BeforeClass(alwaysRun = true)
    public void setupMiniCluster() throws Exception {
        // gets the cluster and create the directory
        cluster = MiniClusterUtils.getMiniCluster(new Configuration());
    }

    // stop the mini-cluster
    @AfterClass(alwaysRun = true)
    public void shutdownMiniCluster() {
        MiniClusterUtils.stopCluster(cluster);
    }

    @DataProvider
    public Object[][] extensionAndCompressorCheck() {
        return new Object[][] {
                {".txt", null},
                {".bz2", BZip2CompressorInputStream.class},
                {".gz", GzipCompressorInputStream.class}
        };
    }

    @Test(dataProvider = "extensionAndCompressorCheck")
    public void testHadoopCompression(final String ext, final Class<? extends CompressorInputStream> compressorClass) throws Exception {
        // get the HadoopPath
        final HadoopPath path = (HadoopPath) IOUtils.getPath(
                MiniClusterUtils.getTempPath(cluster, randomFile.getName(), ext)
                        .toUri().toString());

        // create a new file with the extension
        try (final OutputStream os = HadoopUtils.maybeCompressedOutputStream(path,
                HadoopUtils.getOutputStream(path, false, Defaults.NON_ZERO_BUFFER_SIZE, null, null))) {
            // copy the test file
            Files.copy(randomFile.toPath(), os);
        }

        // now check if the compressor is the same
        try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
            // first open as a compressed stream
            final CompressorInputStream cis = APACHE_COMPRESSOR_FACTORY.createCompressorInputStream(is);
            // then check that the classes are the same
            Assert.assertEquals(cis.getClass(), compressorClass);

            // check that all the bytes are the same after de-compress with other compressor
            Assert.assertEquals(IOUtils.readStreamIntoByteArray(cis, Defaults.BUFFER_SIZE),
                    Files.readAllBytes(randomFile.toPath())
            );

        } catch (CompressorException e) {
            Assert.assertNull(compressorClass, "Compressor not found");
        }
    }
}
