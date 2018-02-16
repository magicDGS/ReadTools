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
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.test.MiniClusterUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HadoopUtilsTest extends RTBaseTest {

    private MiniDFSCluster cluster;

    // init the cluster and copy the files there
    @BeforeClass(alwaysRun = true)
    public void setupMiniCluster() throws Exception {
        // gets the cluster and create the directory
        cluster = MiniClusterUtils.getMiniCluster();
    }

    // stop the mini-cluster
    @AfterClass(alwaysRun = true)
    public void shutdownMiniCluster() {
        MiniClusterUtils.stopCluster(cluster);
    }

    private final HadoopPath getHadoopPath(final String fileName) throws IOException {
        final String uri = cluster.getFileSystem().getUri().toString() + "/" + fileName;
        System.err.println(uri);
        // this should be safe
        return (HadoopPath) IOUtils.getPath(uri);
    }

    @DataProvider
    public Object[][] maybeCompressedDataForOutput() {
        return new Object[][] {
                {"file.txt", null},
                {"file.bz2", org.apache.hadoop.io.compress.BZip2Codec.class}
                // TODO: add 4mc support (see https://github.com/magicDGS/ReadTools/issues/403)
                // {"file..4mc", com.hadoop.compression.fourmc.FourMcCodec.class}
        };
    }

    @Test(dataProvider = "maybeCompressedDataForOutput")
    public void testMaybeCompressedOutputStream(final String fileName,
            final Class<?> expectedClass) throws Exception {
        final HadoopPath path = getHadoopPath(fileName);
        final CompressionCodec codec = HadoopUtils.getCompressionCodec(path);
        if (codec == null) {
            Assert.assertNull(expectedClass, "codec not found");
        } else {
            Assert.assertEquals(codec.getClass(), expectedClass, "unexpected codec");
        }
    }
}