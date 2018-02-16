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

import com.google.common.annotations.VisibleForTesting;
import hdfs.jsr203.HadoopPath;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger logger = LogManager.getLogger(HadoopUtils.class);

    // cannot be instantiated
    private HadoopUtils() {}

    private static CompressionCodecFactory compressionFactory = null;

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

    // TODO: add javadoc
    public static OutputStream maybeCompressedOutputStream(final HadoopPath path, final OutputStream outputStream) throws IOException {
        // get the codec to compress
        final CompressionCodec codec = getCompressionCodec(path);
        if (codec != null) {
            logger.debug("Using {} compressor for {}", codec::getCompressorType, path::toUri);
            return codec.createOutputStream(outputStream);
        }
        // do not use compression
        return outputStream;
    }

    @VisibleForTesting
    protected static CompressionCodec getCompressionCodec(final HadoopPath path) {
        if (compressionFactory == null) {
            compressionFactory = new CompressionCodecFactory(new Configuration());
            logger.debug("Loaded compressors: {}", compressionFactory);
        }
        return compressionFactory.getCodec(path.getRawResolvedPath());
    }
}
