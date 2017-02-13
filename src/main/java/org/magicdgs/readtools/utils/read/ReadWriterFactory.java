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

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.exceptions.RTUserExceptions;
import org.magicdgs.readtools.utils.read.writer.FastqGATKWriter;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;

import htsjdk.samtools.Defaults;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.cram.build.CramIO;
import htsjdk.samtools.fastq.AsyncFastqWriter;
import htsjdk.samtools.fastq.BasicFastqWriter;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.util.AbstractAsyncWriter;
import htsjdk.samtools.util.CustomGzipOutputStream;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Md5CalculatingOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.SAMFileGATKReadWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Factory for generate writers for all sources of reads with the same parameters. Before opening a
 * writer, the file will be check if it exists (unless {@link #forceOverwrite} is {@code true}) and
 * create intermediate directories.
 *
 * Note: the defaults in {@link SAMFileWriterFactory} will be applied, except the useAsyncIo and
 * createMd5.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class ReadWriterFactory {

    private static final Logger logger = LogManager.getLogger(ReadWriterFactory.class);
    private static final CompressorStreamFactory compressorFactory = new CompressorStreamFactory();

    private final SAMFileWriterFactory samFactory;

    // the reference file to use with CRAM
    private File referenceFile = null;
    // false if we do not check for existence
    private boolean forceOverwrite = RTDefaults.FORCE_OVERWRITE;

    private boolean createMd5file;
    private boolean setUseAsyncIo;
    private int asyncOutputBufferSize = AbstractAsyncWriter.DEFAULT_QUEUE_SIZE;
    private int bufferSize = Defaults.BUFFER_SIZE;

    /** Creates a default factory. */
    public ReadWriterFactory() {
        this.samFactory = new SAMFileWriterFactory();
        // setting the default create Md5 to the same as the samFactory default
        this.createMd5file = SAMFileWriterFactory.getDefaultCreateMd5File();
        this.setUseAsyncIo = Defaults.USE_ASYNC_IO_WRITE_FOR_SAMTOOLS;
    }

    ////////////////////////////////////////////
    // PUBLIC METHODS FOR SET OPTIONS

    /** Sets asynchronous writing for any writer. */
    public ReadWriterFactory setUseAsyncIo(final boolean useAsyncIo) {
        this.samFactory.setUseAsyncIo(useAsyncIo);
        this.setUseAsyncIo = useAsyncIo;
        return this;
    }

    /** Sets if the factory should create a MD5 file for any writer. */
    public ReadWriterFactory setCreateMd5File(final boolean createMd5File) {
        this.samFactory.setCreateMd5File(createMd5File);
        this.createMd5file = createMd5File;
        return this;
    }

    /** Sets index creation for BAM/CRAM writers. */
    public ReadWriterFactory setCreateIndex(final boolean createIndex) {
        logger.debug("Create index for FASTQ writers is ignored");
        this.samFactory.setCreateIndex(createIndex);
        return this;
    }

    /** Sets maximum records in RAM for sorting SAM/BAM/CRAM writers. */
    public ReadWriterFactory setMaxRecordsInRam(final int maxRecordsInRam) {
        logger.debug("Maximum records in RAM for FASTQ writers is ignored");
        this.samFactory.setMaxRecordsInRam(maxRecordsInRam);
        return this;
    }

    /** Sets the temp directory for sorting SAM/BAM/CRAM writers. */
    public ReadWriterFactory setTempDirectory(final File tmpDir) {
        logger.debug("Temp directory for FASTQ writers is ignored");
        this.samFactory.setTempDirectory(tmpDir);
        return this;
    }

    /** Sets asynchronous buffer size for any writers. */
    public ReadWriterFactory setAsyncOutputBufferSize(final int asyncOutputBufferSize) {
        this.samFactory.setAsyncOutputBufferSize(asyncOutputBufferSize);
        this.asyncOutputBufferSize = asyncOutputBufferSize;
        return this;
    }

    /** Sets buffer size for SAM/BAM/CRAM writers. */
    public ReadWriterFactory setBufferSize(final int bufferSize) {
        this.samFactory.setBufferSize(bufferSize);
        this.bufferSize = bufferSize;
        return this;
    }

    /** Sets the reference file. This is required for CRAM writers. */
    public ReadWriterFactory setReferenceFile(final File referenceFile) {
        logger.debug("Reference file for FASTQ writers is ignored");
        this.referenceFile = referenceFile;
        return this;
    }

    /** Sets if the output will be overwriten even if it exists. */
    public ReadWriterFactory setForceOverwrite(final boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
        return this;
    }

    ////////////////////////////////////////////
    // PUBLIC METHODS FOR GET WRITERS


    /** Open a new FASTQ writer from a Path. */
    public FastqWriter openFastqWriter(final Path path) {
        checkOutputAndCreateDirs(path);
        final PrintStream writer = new PrintStream(getOutputStream(path));
        final FastqWriter fastqWriter = new BasicFastqWriter(writer);
        return (this.setUseAsyncIo)
                ? new AsyncFastqWriter(fastqWriter, asyncOutputBufferSize)
                : fastqWriter;
    }

    /** Open a new FASTQ writer based from a String path. */
    public FastqWriter openFastqWriter(final String output) {
        return openFastqWriter(newOutputFile(output));
    }

    /** Open a new SAM/BAM/CRAM writer from a String path. */
    public SAMFileWriter openSAMWriter(final SAMFileHeader header, final boolean presorted,
            final String output) {
        return openSAMWriter(header, presorted, newOutputFile(output));
    }

    /** Open a new SAM/BAM/CRAM writer from a Path. */
    public SAMFileWriter openSAMWriter(final SAMFileHeader header, final boolean presorted,
            final Path output) {
        checkOutputAndCreateDirs(output);
        try {
            return samFactory.makeWriter(header, presorted, output.toFile(), referenceFile);
        } catch (final SAMException e) {
            // catch SAM exceptions as IO errors -> this are the ones that may fail
            throw new UserException.CouldNotCreateOutputFile(output.toFile(), e.getMessage(), e);
        }
    }

    /** Creates a SAM/BAM/CRAM writer from a String path. */
    public GATKReadWriter createSAMWriter(final String output, final SAMFileHeader header,
            final boolean presorted) {
        if (null == referenceFile && output.endsWith(CramIO.CRAM_FILE_EXTENSION)) {
            throw new UserException.MissingReference(
                    "A reference file is required for writing CRAM files");
        }
        return new SAMFileGATKReadWriter(openSAMWriter(header, presorted, output));
    }

    /** Creates a FASTQ writer from a String path. */
    public GATKReadWriter createFASTQWriter(final String output) {
        return new FastqGATKWriter(openFastqWriter(output));
    }

    /** Creates a GATKReadWriter based on the path extension. */
    public GATKReadWriter createWriter(final String output, final SAMFileHeader header,
            final boolean presorted) {
        if (ReadToolsIOFormat.isSamBamOrCram(output)) {
            return createSAMWriter(output, header, presorted);
        } else if (ReadToolsIOFormat.isFastq(output)) {
            return createFASTQWriter(output);
        }
        throw new RTUserExceptions.InvalidOutputFormat(output,
                "not supported output format based on the extension.");
    }

    ////////////////////////////////////
    // PRIVATE HELPERS

    // get the output stream wrapped as necessary based on the params and path extension
    private OutputStream getOutputStream(final Path outputPath) {
        try {
            // the same as in the SAMFileWriterFactory
            // 1. get the output stream for the file (maybe buffered)
            OutputStream os = IOUtil.maybeBufferOutputStream(
                    Files.newOutputStream(outputPath),
                    bufferSize);

            // 2. Wraps the stream to compute MD5 digest if createMd5file is provided
            os = (createMd5file)
                    ? new Md5CalculatingOutputStream(os, new File(outputPath.toString() + ".md5"))
                    : os;

            // 3. apply a compressor if the extension is correct
            return maybeCompressedWrap(os, outputPath);

        } catch (IOException e) {
            throw new UserException.CouldNotCreateOutputFile(outputPath.toString(), e.getMessage(),
                    e);
        }
    }

    // wraps the output stream if it ends with a compression extension
    // gzip is handled with HTSJDK; other formats are handled with the compressor factory
    private OutputStream maybeCompressedWrap(final OutputStream outputStream,
            final Path outputPath) {
        try {
            // extension to determine the compression
            final String ext = FilenameUtils.getExtension(outputPath.toString());

            // handle the gzip format with the CustomGzipOutputStream from HTSJDK for backwards-compatibility
            if (CompressorStreamFactory.GZIP.equals(ext)) {
                return new CustomGzipOutputStream(outputStream, IOUtil.getCompressionLevel());
            }

            // fallback in other compression algorithms
            return compressorFactory.createCompressorOutputStream(ext, outputStream);
        } catch (final CompressorException | IOException e) {
            // log for pinpoint errors
            logger.debug("Not using compression for output stream {}: {}",
                    () -> outputPath, () -> e.getMessage());
        }

        // return the same stream if some error occurs
        return outputStream;
    }

    /**
     * Creates a new output file, generating all the sub-directories and checking for the existence
     * of the file if requested.
     *
     * @param output the output file name.
     *
     * @return the path object.
     *
     * @throws UserException if the file already exists or an I/O error occurs.
     */
    private Path newOutputFile(final String output) {
        final Path path = org.broadinstitute.hellbender.utils.io.IOUtils.getPath(output);
        // check if the file already exists and create the directories
        checkOutputAndCreateDirs(path);
        // return the file
        return path;
    }

    /**
     * Checks the existence of the file if the factory should do it and generate all the
     * intermediate directories.
     */
    private void checkOutputAndCreateDirs(final Path outputPath) {
        if (!forceOverwrite && Files.exists(outputPath)) {
            throw new RTUserExceptions.OutputFileExists(outputPath);
        }
        try {
            Files.createDirectories(outputPath.toAbsolutePath().getParent());
        } catch (final IOException e) {
            throw new UserException.CouldNotCreateOutputFile(outputPath.toFile(), e.getMessage(),
                    e);
        }
    }
}
