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

package org.magicdgs.readtools.utils.read;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.misc.IOUtils;
import org.magicdgs.readtools.utils.read.writer.FastqGATKWriter;

import htsjdk.samtools.Defaults;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.cram.build.CramIO;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.SAMFileGATKReadWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

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

    private final FastqWriterFactory fastqFactory;
    private final SAMFileWriterFactory samFactory;

    // the reference file to use with CRAM
    private File referenceFile = null;
    // false if we do not check for existence
    private boolean forceOverwrite = RTDefaults.FORCE_OVERWRITE;

    /** Creates a default factory. */
    public ReadWriterFactory() {
        this.samFactory = new SAMFileWriterFactory();
        this.fastqFactory = new FastqWriterFactory();
        // setting the default create Md5 to the same as the samFactory default
        // because this could be change statically and we would like to propagate to the FASTQ writer
        this.fastqFactory.setCreateMd5(SAMFileWriterFactory.getDefaultCreateMd5File());
    }

    /** Sets asynchronous writing for any writer. */
    public ReadWriterFactory setUseAsyncIo(final boolean useAsyncIo) {
        this.samFactory.setUseAsyncIo(useAsyncIo);
        this.fastqFactory.setUseAsyncIo(useAsyncIo);
        return this;
    }

    /** Sets if the factory should create a MD5 file for any writer. */
    public ReadWriterFactory setCreateMd5File(final boolean createMd5File) {
        this.samFactory.setCreateMd5File(createMd5File);
        this.fastqFactory.setCreateMd5(createMd5File);
        return this;
    }

    /** Sets index creation for BAM/CRAM writers. */
    public ReadWriterFactory setCreateIndex(final boolean createIndex) {
        this.samFactory.setCreateIndex(createIndex);
        return this;
    }

    /** Sets maximum records in RAM for sorting SAM/BAM/CRAM writers. */
    public ReadWriterFactory setMaxRecordsInRam(final int maxRecordsInRam) {
        // TODO: change when supporting sorting of FASTQ files
        this.samFactory.setMaxRecordsInRam(maxRecordsInRam);
        return this;
    }

    /** Sets the temp directory for sorting SAM/BAM/CRAM writers. */
    public ReadWriterFactory setTempDirectory(final File tmpDir) {
        // TODO: change when supporting sorting og FASTQ files
        this.samFactory.setTempDirectory(tmpDir);
        return this;
    }

    /** Sets asynchronous buffer size for SAM/BAM/CRAM writers. */
    public ReadWriterFactory setAsyncOutputBufferSize(final int asyncOutputBufferSize) {
        // TODO: this should be change for FastqWriters
        this.samFactory.setAsyncOutputBufferSize(asyncOutputBufferSize);
        return this;
    }

    /** Sets buffer size for SAM/BAM/CRAM writers. */
    public ReadWriterFactory setBufferSize(final int bufferSize) {
        // TODO: this should be change for FastqWriters as well
        this.samFactory.setBufferSize(bufferSize);
        return this;
    }

    /** Sets the reference file. This is required for CRAM writers. */
    public ReadWriterFactory setReferenceFile(final File referenceFile) {
        this.referenceFile = referenceFile;
        return this;
    }

    /** Sets if the output will be overwriten even if it exists. */
    public ReadWriterFactory setForceOverwrite(final boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
        return this;
    }

    /** Open a new FASTQ writer from a Path. */
    public FastqWriter openFastqWriter(final Path path) {
        checkOutputAndCreateDirs(path);
        return createWrappingException(() -> fastqFactory.newWriter(path.toFile()), path::toString);
    }

    /** Open a new FASTQ writer based from a String path. */
    public FastqWriter openFastqWriter(final String output) {
        return openFastqWriter(IOUtils.newOutputFile(output, !forceOverwrite));
    }

    /** Open a new SAM/BAM/CRAM writer from a String path. */
    public SAMFileWriter openSAMWriter(final SAMFileHeader header, final boolean presorted,
            final String output) {
        return openSAMWriter(header, presorted, IOUtils.newOutputFile(output, !forceOverwrite));
    }

    /** Open a new SAM/BAM/CRAM writer from a Path. */
    public SAMFileWriter openSAMWriter(final SAMFileHeader header, final boolean presorted,
            final Path output) {
        checkOutputAndCreateDirs(output);
        return createWrappingException(
                () -> samFactory.makeWriter(header, presorted, output.toFile(), referenceFile),
                output::toString);
    }

    /** Creates a SAM/BAM/CRAM writer from a String path. */
    public GATKReadWriter createSAMWriter(final String output, final SAMFileHeader header,
            final boolean presorted) {
        if (null == referenceFile && output.endsWith(CramIO.CRAM_FILE_EXTENSION)) {
            throw new UserException.MissingReference("A reference file is required for writing CRAM files");
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
        if (IOUtils.isSamBamOrCram(output)) {
            return createSAMWriter(output, header, presorted);
        } else if (IOUtils.isFastq(output)) {
            return createFASTQWriter(output);
        }
        throw new UserException.CouldNotCreateOutputFile(new File(output),
                "not supported output format based on the extension");
    }

    /**
     * Checks the existence of the file if the factory should do it and generate all the
     * intermediate directories.
     */
    private void checkOutputAndCreateDirs(final Path outputFile) {
        try {
            if (!forceOverwrite) {
                IOUtils.exceptionIfExists(outputFile);
            }
            Files.createDirectories(outputFile.getParent());
        } catch (IOException e) {
            throw new UserException.CouldNotCreateOutputFile(outputFile.toFile(), e.getMessage(),
                    e);
        }
    }

    // any exception caused by open a file for will thrown a could not read input file exception
    private static <T> T createWrappingException(final Callable<T> opener,
            final Supplier<String> source) {
        try {
            return opener.call();
        } catch (Exception e) {
            throw new UserException.CouldNotCreateOutputFile(source.get(), e.getMessage(), e);
        }
    }

}
