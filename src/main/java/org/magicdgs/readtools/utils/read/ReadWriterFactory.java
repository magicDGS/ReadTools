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
import org.magicdgs.readtools.utils.HadoopUtils;
import org.magicdgs.readtools.utils.distmap.DistmapGATKWriter;
import org.magicdgs.readtools.utils.fastq.FastqGATKWriter;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;

import hdfs.jsr203.HadoopPath;
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
import htsjdk.tribble.AbstractFeatureReader;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.SAMFileGATKReadWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.URI;
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

    private final SAMFileWriterFactory samFactory;

    // the reference file to use with CRAM
    private Path referencePath = null;
    // false if we do not check for existence
    private boolean forceOverwrite = RTDefaults.FORCE_OVERWRITE;

    private boolean createMd5file;
    private boolean useAsyncIo;
    private int asyncOutputBufferSize = AbstractAsyncWriter.DEFAULT_QUEUE_SIZE;
    private int bufferSize = Defaults.BUFFER_SIZE;

    // block-size for HDFS; if null, use the default
    private Long hdfsBlockSize = null;

    /** Creates a default factory. */
    public ReadWriterFactory() {
        this.samFactory = new SAMFileWriterFactory();
        // setting the default create Md5 to the same as the samFactory default
        this.createMd5file = SAMFileWriterFactory.getDefaultCreateMd5File();
        this.useAsyncIo = Defaults.USE_ASYNC_IO_WRITE_FOR_SAMTOOLS;
    }

    ////////////////////////////////////////////
    // PUBLIC METHODS FOR SET OPTIONS

    /** Sets asynchronous writing for any writer. */
    public ReadWriterFactory setUseAsyncIo(final boolean useAsyncIo) {
        this.samFactory.setUseAsyncIo(useAsyncIo);
        this.useAsyncIo = useAsyncIo;
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
        logger.debug("Create index for FASTQ/Distmap writers is ignored");
        this.samFactory.setCreateIndex(createIndex);
        return this;
    }

    /** Sets maximum records in RAM for sorting SAM/BAM/CRAM writers. */
    public ReadWriterFactory setMaxRecordsInRam(final int maxRecordsInRam) {
        logger.debug("Maximum records in RAM for FASTQ/Distmap writers is ignored");
        this.samFactory.setMaxRecordsInRam(maxRecordsInRam);
        return this;
    }

    /** Sets the temp directory for sorting SAM/BAM/CRAM writers. */
    public ReadWriterFactory setTempDirectory(final File tmpDir) {
        logger.debug("Temp directory for FASTQ/Distmap writers is ignored");
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
    public ReadWriterFactory setReferencePath(final Path referencePath) {
        logger.debug("Reference file for FASTQ/Distmap writers is ignored");
        // TODO - this should set the reference Path in the samFactory (https://github.com/magicDGS/ReadTools/issues/376)
        this.referencePath = referencePath;
        return this;
    }

    /** Sets if the output will be overwriten even if it exists. */
    public ReadWriterFactory setForceOverwrite(final boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
        return this;
    }

    /** Sets the block-size for HDFS output files. */
    public ReadWriterFactory setHdfsBlockSize(final Long hdfsBlockSize) {
        this.hdfsBlockSize = hdfsBlockSize;
        return this;
    }

    ////////////////////////////////////////////
    // PUBLIC METHODS FOR GET WRITERS


    /** Open a new FASTQ writer from a Path. */
    public FastqWriter openFastqWriter(final Path path) {
        checkOutputAndCreateDirs(path);
        final PrintStream writer = new PrintStream(getOutputStream(path));
        final FastqWriter fastqWriter = new BasicFastqWriter(writer);
        return (this.useAsyncIo)
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
            return samFactory.makeWriter(header, presorted, output, getReferenceAsFile());
        } catch (final SAMException e) {
            // catch SAM exceptions as IO errors -> this are the ones that may fail
            throw new UserException.CouldNotCreateOutputFile(output.toUri().toString(), e.getMessage(), e);
        }
    }

    // TODO - this will blow up if the java.nio.Path is not a file and the output is CRAM (https://github.com/magicDGS/ReadTools/issues/376)
    // TODO - it requires an HTSJDK change not yet in their codebase (https://github.com/samtools/htsjdk/pull/1005)
    private File getReferenceAsFile() {
        try {
            return (referencePath == null) ? null : referencePath.toFile();
        } catch (final UnsupportedOperationException e) {
            // log a warning saying the limitation
            logger.warn("{} is not in the deafult file system and cannot be use for writing outputs (would fail for CRAM files). This limitation might be removed in the future.",
                    referencePath::toUri);
            return null;
        }
    }

    /** Creates a SAM/BAM/CRAM writer from a String path. */
    public GATKReadWriter createSAMWriter(final String output, final SAMFileHeader header,
            final boolean presorted) {
        if (output.endsWith(CramIO.CRAM_FILE_EXTENSION) && getReferenceAsFile() == null) {
            throw new UserException.MissingReference(
                    "A reference file is required for writing CRAM files");
        }
        return new SAMFileGATKReadWriter(openSAMWriter(header, presorted, output));
    }

    /** Creates a FASTQ writer from a String path. */
    public GATKReadWriter createFASTQWriter(final String output) {
        return new FastqGATKWriter(openFastqWriter(output));
    }

    /** Creates a GATKWriter for Distmap output. */
    public GATKReadWriter createDistmapWriter(final String output, final boolean isPaired) {
        final Path outputPath = newOutputFile(output);
        logger.debug("Distmap output: {}", outputPath::toUri);
        final DistmapGATKWriter writer = new DistmapGATKWriter(
                new OutputStreamWriter(getOutputStream(outputPath)),
                outputPath.toUri().toString(), isPaired);
        return (useAsyncIo) ? new AsyncGATKWriter(writer, asyncOutputBufferSize) : writer;
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
            // Note: hadoop paths are handled differently
            OutputStream os = getMaybeBufferedOutputStream(outputPath);

            // 2. Wraps the stream to compute MD5 digest if createMd5file is provided
            os = (createMd5file)
                    ? new Md5CalculatingOutputStream(
                    os, outputPath.getFileSystem().getPath(outputPath.toString() + ".md5"))
                    : os;

            // 3. apply a compressor if the extension is correct
            return maybeCompressedWrap(os, outputPath);

        } catch (IOException e) {
            throwCouldNotCreateOutputPath(outputPath, e);
        }
        throw new GATKException.ShouldNeverReachHereException("getOutputStream");
    }

    /**
     * Creates a maybe buffered output stream from a Path.
     *
     * <p>For HDFS files, it uses the Hadoop API, which sets its own buffer, so no extra wrapping
     * is needed. In addition, it sets the block-size to the one in the factory.
     */
    private OutputStream getMaybeBufferedOutputStream(final Path path) throws IOException {
        // if it is a Hadoop path, get the stream with its API always
        if (path instanceof HadoopPath) {
            return HadoopUtils.getOutputStream((HadoopPath) path, forceOverwrite, bufferSize,
                    // TODO: include an argument for replication (https://github.com/magicDGS/ReadTools/issues/410)
                    null, hdfsBlockSize);
        }
        if (hdfsBlockSize != null) {
            logger.debug("Block-size={} is ignored for {}", () -> hdfsBlockSize, path::toUri);
        }
        return IOUtil.maybeBufferOutputStream(Files.newOutputStream(path), bufferSize);
    }

    /**
     * Wraps the output stream into a compressed stream if needed.
     *
     * <p>Detection of compression is done as following:
     *
     * <ul>
     * <li>
     * If {@link AbstractFeatureReader#hasBlockCompressedExtension(URI)} returns {@code true}, then
     * it is open as GZIP (HTSJDK compatible).
     * </li>
     * <li>
     * If {@link BZip2Utils#isCompressedFilename(String)} returns {@code true}, then it is open as a
     * Bzip compressed input using commons-compress
     * </li>
     * </ul>
     *
     * <p>Warning:for HDFS files, compression is handled by the Hadoop codecs.
     */
    private OutputStream maybeCompressedWrap(final OutputStream outputStream,
            final Path outputPath) throws IOException {
        // for HadoopPath is compressing-decompressing the files
        if (outputPath instanceof HadoopPath) {
            logger.debug("Delegating to Hadoop to choose compressor for {}", outputPath::toUri);
            return HadoopUtils.maybeCompressedOutputStream((HadoopPath) outputPath, outputStream);
        }
        // for local files use commons compress except for gzip compression:
        // use CustomGzipOutputStream from HTSJDK for backwards-compatibility
        // TODO: we should be more consistent with the supported compression formats (https://github.com/magicDGS/ReadTools/issues/411)
        if (AbstractFeatureReader.hasBlockCompressedExtension(outputPath.toUri())) {
            logger.debug("Using gzip compression for {}", outputPath::toUri);
            return new CustomGzipOutputStream(outputStream, IOUtil.getCompressionLevel());
        } else if (BZip2Utils.isCompressedFilename(outputPath.toString())) {
            // kept for backwards compatibility
            logger.debug("Using bzip2 compressor for {}", outputPath::toUri);
            return new BZip2CompressorOutputStream(outputStream);
        } else {
            logger.debug("Not using compression for {}", outputPath::toUri);
            return outputStream;
        }
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
            throwCouldNotCreateOutputPath(outputPath, e);
        }
    }

    /**
     * Helper method to throw a {@link UserException.CouldNotCreateOutputFile} exception for a
     * path. This method should be used in the ReadWriterFactory for consistency in thrown
     * exceptions with informative messages.
     */
    private static final void throwCouldNotCreateOutputPath(final Path path,
            final Exception e) {
        throw new UserException.CouldNotCreateOutputFile(
                // using URI to be more informative
                path.toUri().toString(),
                // use the class name, because the exception message is printed anyway
                e.getClass().getSimpleName(),
                e);
    }

    /**
     * Close the writer if it is not {@code null}, throwing if it is not possible to close.
     */
    public static final void closeWriter(final GATKReadWriter writer) {
        // if the writer is null, just log
        if (writer == null) {
            logger.debug("Writer is null.");
        } else {
            try {
                writer.close();
            } catch (final IOException e) {
                throw new UserException.CouldNotCreateOutputFile(
                        String.format("Couldn't close output file: %s", e.getMessage()),
                        e);
            }
        }
    }

    /** Class for writing asynchronously with custom GATKReadWriters. */
    private static final class AsyncGATKWriter extends AbstractAsyncWriter<GATKRead>
            implements GATKReadWriter {

        private final GATKReadWriter underlyingWriter;

        /** Constructor for wrapping a writer. */
        protected AsyncGATKWriter(final GATKReadWriter underlyingWriter, int queueSize) {
            super(queueSize);
            this.underlyingWriter = underlyingWriter;
        }

        /** Adds the read asynchronously. */
        @Override
        public void addRead(GATKRead read) {
            write(read);
        }

        /** Adds the read to the underlying writer. */
        @Override
        protected void synchronouslyWrite(final GATKRead item) {
            underlyingWriter.addRead(item);
        }

        /** Close the underlying writer. */
        @Override
        protected void synchronouslyClose() {
            closeWriter(this.underlyingWriter);
        }

        @Override
        protected String getThreadNamePrefix() {
            return "GATKReadWriterThread-";
        }
    }
}
