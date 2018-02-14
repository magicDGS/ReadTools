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

import hdfs.jsr203.HadoopPath;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.compress.BZip2Codec;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Hadoop utilities.
 *
 * <p>The purpose of this utilities is to avoid the usage of {@link org.apache.hadoop} imports in
 * other classes not related directly with hadoop.
 *
 * <p>For example, if the jar file does not include the Hadoop library, it should still work
 * for {@link java.nio.file.Path} that are not {@link HadoopPath}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class HadoopUtils {

    // cannot be instantiated
    private HadoopUtils() {}

    // bzip2 codec, initialized on demand
    private static BZip2Codec bzip2 = null;

    /**
     * Gets an output stream from an HDFS file.
     *
     * <p>Note: teh replication use the default set on the path.
     *
     * @param path       path in HDFS.
     * @param overwrite  if {@code true}, it will overwrite if the path already exists.
     * @param bufferSize buffer-size for the output stream.
     * @param blockSize  block-size for the output stream.
     *
     * @return an output stream (maybe buffered)
     */
    public static OutputStream getOutputStream(final HadoopPath path, final boolean overwrite,
            final int bufferSize, final long blockSize) throws IOException {
        final FileSystem hdfs = path.getFileSystem().getHDFS();
        // gets the the path (extracted here to get the default replication)
        final org.apache.hadoop.fs.Path hdfsPath = path.getRawResolvedPath();

        // construct the output stream already buffered here
        return hdfs.create(hdfsPath, overwrite,
                // buffer size
                bufferSize,
                // replication is using the default
                hdfs.getDefaultReplication(hdfsPath),
                // block-size is the one set here
                blockSize);
    }

    /**
     * Wraps the output stream with the hadoop {@link BZip2Codec}.
     *
     * <p>Note: this method can be used also for non-HDFS files. Nevertheless, if the Hadoop-library
     * is not present it might fail.
     *
     * @param outputStream output stream to wrap.
     *
     * @return a wrapped output stream with bzip2 compression.
     */
    public static OutputStream wrapBzip2Stream(final OutputStream outputStream)
            throws IOException {
        if (bzip2 == null) {
            // require initialize with a default configuration
            // initialize only if required for the output
            bzip2 = new BZip2Codec();
            bzip2.setConf(new Configuration());
        }
        return bzip2.createOutputStream(outputStream);
    }
}
