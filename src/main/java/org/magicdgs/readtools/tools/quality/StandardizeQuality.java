/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.tools.quality;

import org.magicdgs.io.readers.bam.SamReaderSanger;
import org.magicdgs.io.readers.fastq.single.FastqReaderSingleInterface;
import org.magicdgs.io.readers.fastq.single.FastqReaderSingleSanger;
import org.magicdgs.io.writers.bam.ReadToolsSAMFileWriterFactory;
import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.tools.ReadToolsBaseTool;
import org.magicdgs.readtools.utils.fastq.QualityUtils;
import org.magicdgs.readtools.utils.logging.FastqLogger;
import org.magicdgs.readtools.utils.logging.ProgressLoggerExtension;
import org.magicdgs.readtools.utils.misc.IOUtils;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.programgroups.QCProgramGroup;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Class for converting from Illumina to Sanger encoding both FASTQ and BAM files
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Convert an Illumina BAM/FASTQ file into a Sanger.",
        summary =
                "The standard encoding for a BAM file is Sanger and this tool is provided to standardize both BAM/FASTQ files "
                        + "for latter analysis. It does not support mixed qualities.",
        programGroup = QCProgramGroup.class)
public final class StandardizeQuality extends ReadToolsBaseTool {

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME, optional = false,
            doc = "Input BAM/FASTQ to determine the quality.")
    public File input = null;

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.OUTPUT_SHORT_NAME, optional = false,
            doc = "Output for the converted file. The extension determine the format SAM/BAM or FASTQ/GZIP.")
    public File output = null;

    private Closeable reader;
    private Closeable writer;

    @Override
    protected String[] customCommandLineValidation() {
        if (maintainFormat) {
            throw new UserException.BadArgumentValue(
                    ReadToolsLegacyArgumentDefinitions.MAINTAIN_FORMAT_SHORT_NAME);
        }
        if (QualityUtils.getFastqQualityFormat(input) == FastqQualityFormat.Standard) {
            throw new UserException.BadInput(
                    String.format("%s already in Sanger formatting", input));
        }
        return super.customCommandLineValidation();
    }

    @Override
    protected Object doWork() {
        if (IOUtils.isSamBamOrCram(input.toPath())) {
            runBam();
        } else {
            if (CREATE_INDEX) {
                logger.warn("Index won't be performed for FASTQ output file.");
            }
            runFastq();
        }
        return null;
    }


    /**
     * Change the format in a Fastq file
     */
    private void runFastq() {
        // open reader (directly converting)
        final FastqReaderSingleInterface reader =
                new FastqReaderSingleSanger(input, allowHigherSangerQualities);
        // open factory for writer
        final FastqWriterFactory factory = new FastqWriterFactory();
        factory.setUseAsyncIo(nThreads != 1);
        // open writer
        final FastqWriter writer = factory.newWriter(output);
        // start iterations
        final FastqLogger progress = new FastqLogger(logger);
        for (final FastqRecord record : reader) {
            writer.write(record);
            progress.add();
        }
        progress.logNumberOfVariantsProcessed();
        this.reader = reader;
        this.writer = writer;
    }

    /**
     * Change the format in a BAM file
     */
    private void runBam() {
        final SamReader reader =
                new SamReaderSanger(input, SamReaderFactory.make(), allowHigherSangerQualities);
        final SAMFileHeader header = reader.getFileHeader();
        header.addProgramRecord(getToolProgramRecord());
        final SAMFileWriter writer;
        try {
            writer = new ReadToolsSAMFileWriterFactory().setCreateIndex(CREATE_INDEX)
                    .setUseAsyncIo(nThreads != 1)
                    .makeSAMOrBAMWriter(header, true, output);
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
        // start iterations
        final ProgressLoggerExtension progress = new ProgressLoggerExtension(logger);
        for (final SAMRecord record : reader) {
            writer.addAlignment(record);
            progress.record(record);
        }
        progress.logNumberOfVariantsProcessed();
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(writer);
        CloserUtil.close(reader);
    }
}
