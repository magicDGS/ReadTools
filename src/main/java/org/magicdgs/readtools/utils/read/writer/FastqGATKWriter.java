/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.utils.read.writer;

import htsjdk.samtools.Defaults;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.fastq.FastqConstants;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Md5CalculatingOutputStream;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/**
 * Basic writer for GATKRead to output a FASTQ file.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqGATKWriter implements GATKReadWriter {

    // TODO: this will be in HTSJDK FastqConstants after https://github.com/samtools/htsjdk/pull/572
    public static final String FIRST_OF_PAIR = "/1";
    public static final String SECOND_OF_PAIR = "/2";

    // the writer
    private final PrintStream writer;

    /**
     * Constructor from a path.
     * @param path the path to write in.
     * @param createMd5 if {@code true}, generates an MD5 digest file.
     */
    public FastqGATKWriter(final Path path, final boolean createMd5) {
        // TODO: probably we can store the path to be sure that we are writing it properly
        this.writer = new PrintStream(createOutputStream(path, createMd5));
    }

    private OutputStream createOutputStream(Path path, boolean createMd5) {
        try {
            OutputStream stream = Files.newOutputStream(path);
            // TODO: different check for other compressed extensions
            if (path.endsWith(".gz")) {
                // TODO: maybe settable buffer and compression level?
                stream = new CustomGzipOutputStream(stream, Defaults.NON_ZERO_BUFFER_SIZE,
                        Defaults.COMPRESSION_LEVEL);
            }
            if (createMd5) {
                // TODO: this should be tested
                stream = new Md5CalculatingOutputStream(stream, new File(path.toFile() + ".md5"));
            }
            return IOUtil.maybeBufferOutputStream(stream);
        } catch (IOException e) {
            throw new UserException.CouldNotCreateOutputFile(path.toFile(), e.getMessage(), e);
        }
    }

    @Override
    public void addRead(final GATKRead read) {
        writer.print(FastqConstants.SEQUENCE_HEADER);
        writer.print(read.getName());
        // add the pair information if necessary
        if (read.isPaired()) {
            // TODO: check if there is some incompatibility?
            if (read.isFirstOfPair()) {
                writer.print(FIRST_OF_PAIR);
            } else {
                writer.print(SECOND_OF_PAIR);
            }
        }
        writer.println();
        writer.println(read.getBasesString());
        writer.print(FastqConstants.QUALITY_HEADER);
        // print the comment if present
        final String comment = read.getAttributeAsString(SAMTag.CO.name());
        if (comment != null) {
            writer.print(comment);
        }
        writer.println();
        writer.println(ReadUtils.getBaseQualityString(read));
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}

/**
 * Hacky little class used to allow us to set the compression level on a GZIP output stream which,
 * for some
 * bizarre reason, is not exposed in the standard API.
 *
 * @author Tim Fennell
 */
// TODO: this is copied from IOUTil, but it should be maybe open in htsjdk
// TODO: although I rather suggest a method to wrap the stream in a CustomGzipOutputStream
class CustomGzipOutputStream extends GZIPOutputStream {
    CustomGzipOutputStream(final OutputStream outputStream, final int bufferSize,
            final int compressionLevel) throws IOException {
        super(outputStream, bufferSize);
        this.def.setLevel(compressionLevel);
    }

    CustomGzipOutputStream(final OutputStream outputStream, final int compressionLevel)
            throws IOException {
        super(outputStream);
        this.def.setLevel(compressionLevel);
    }
}