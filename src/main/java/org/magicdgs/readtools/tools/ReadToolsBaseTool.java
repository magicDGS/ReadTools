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

package org.magicdgs.readtools.tools;

import org.magicdgs.io.readers.bam.SamReaderImpl;
import org.magicdgs.io.readers.bam.SamReaderSanger;
import org.magicdgs.io.readers.fastq.FastqReaderInterface;
import org.magicdgs.io.readers.fastq.paired.FastqReaderPairedImpl;
import org.magicdgs.io.readers.fastq.paired.FastqReaderPairedSanger;
import org.magicdgs.io.readers.fastq.single.FastqReaderSingleImpl;
import org.magicdgs.io.readers.fastq.single.FastqReaderSingleSanger;
import org.magicdgs.io.writers.bam.ReadToolsSAMFileWriterFactory;
import org.magicdgs.io.writers.bam.SplitSAMFileWriter;
import org.magicdgs.io.writers.fastq.ReadToolsFastqWriter;
import org.magicdgs.io.writers.fastq.ReadToolsFastqWriterFactory;
import org.magicdgs.io.writers.fastq.SplitFastqWriter;
import org.magicdgs.readtools.ProjectProperties;
import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.StringUtil;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.PicardCommandLineProgram;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.File;
import java.io.IOException;

/**
 * Base tool with basic parameters for ReadTools
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class ReadToolsBaseTool extends PicardCommandLineProgram {

    // TODO: remove this option
    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.MAINTAIN_FORMAT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.MAINTAIN_FORMAT_SHORT_NAME, optional = true,
            doc = ReadToolsLegacyArgumentDefinitions.MAINTAIN_FORMAT_DOC)
    public Boolean maintainFormat = false;

    // TODO: remove this option
    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.PARALLEL_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.PARALLEL_SHORT_NAME, optional = true,
            doc = ReadToolsLegacyArgumentDefinitions.PARALLEL_DOC)
    public Integer nThreads = 1;

    // TODO: remove this option
    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.ALLOW_HIGHER_SANGER_QUALITIES_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.ALLOW_HIGHER_SANGER_QUALITIES_SHORT_NAME, optional = true,
            doc = ReadToolsLegacyArgumentDefinitions.ALLOW_HIGHER_SANGER_QUALITIES_DOC)
    public Boolean allowHigherSangerQualities = false;

    // TODO: change this option
    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.DISABLE_ZIPPED_OUTPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.DISABLE_ZIPPED_OUTPUT_SHORT_NAME, optional = true,
            doc = ReadToolsLegacyArgumentDefinitions.DISABLE_ZIPPED_OUTPUT_DOC)
    public Boolean disableZippedOutput = false;

    /** Default implementation returns an error message if the number of threads is negative. */
    @Override
    protected String[] customCommandLineValidation() {
        if (nThreads < 0) {
            throw new UserException.BadArgumentValue(
                    ReadToolsLegacyArgumentDefinitions.PARALLEL_LONG_NAME, nThreads.toString(),
                    "should be a positive number.");
        }
        return super.customCommandLineValidation();
    }

    /** Prints the ReadTools header if not {@link #QUIET} and then run the super method. */
    public final Object instanceMainPostParseArgs() {
        if (!QUIET) {
            printProgramHeader();
        }
        return super.instanceMainPostParseArgs();
    }

    /** Prints the program header into the stderr. */
    private static void printProgramHeader() {
        String header = String.format("%s (compiled on %s)",
                ProjectProperties.getFormattedNameWithVersion(),
                ProjectProperties.getTimestamp());
        System.err.println(header);
        System.err.println(StringUtil.repeatCharNTimes('=', header.length()));
    }

    /**
     * Logging important information about our tools.
     */
    @Override
    protected void onStartup() {
        if (maintainFormat) {
            logger.warn(
                    "Output will not be standardize. Does not provide the option --{} to avoid this behaviour.",
                    ReadToolsLegacyArgumentDefinitions.MAINTAIN_FORMAT_LONG_NAME);
        } else {
            logger.info("Output will be in Sanger format independently of the input format.");
        }
        if (allowHigherSangerQualities) {
            logger.warn(
                    "Standard qualities higher than specifications will be allowed. Does not provide the option --{} to avoid this behaviour.",
                    ReadToolsLegacyArgumentDefinitions.ALLOW_HIGHER_SANGER_QUALITIES_LONG_NAME);
        }
        if (nThreads != 1) {
            logger.warn(
                    "Currently multi-threads does not control the number of threads in use, depends on the number of outputs.");
        }
    }

    /**
     * Get the tool record for a SAM header
     *
     * @return the program record with the tool
     */

    public SAMProgramRecord getToolProgramRecord() {
        SAMProgramRecord toReturn = new SAMProgramRecord(
                String.format("%s %s", ProjectProperties.getName(),
                        this.getClass().getSimpleName()));
        toReturn.setProgramName(ProjectProperties.getName());
        toReturn.setProgramVersion(getVersion());
        toReturn.setCommandLine(getCommandLine());
        return toReturn;
    }

    private ReadToolsFastqWriterFactory fastqWriterFactory = null;
    private ReadToolsSAMFileWriterFactory samFileWriterFactory = null;
    private SamReaderFactory samReaderFactory = null;

    private ReadToolsFastqWriterFactory getFastqFactoryFromCommandLine() {
        if (fastqWriterFactory == null) {
            // TODO: create and add an option to force output
            fastqWriterFactory = new ReadToolsFastqWriterFactory()
                    .setGzipOutput(!disableZippedOutput)
                    .setUseAsyncIo(nThreads != 1)
                    .setCreateMd5(CREATE_MD5_FILE);
        }
        return fastqWriterFactory;
    }

    private ReadToolsSAMFileWriterFactory getSamFactoryFromCommandLine() {
        if (samFileWriterFactory == null) {
            // Picard is taking care of setting the index creation, md5 digest, max records in ram
            // TODO: create and add an option to force output
            samFileWriterFactory = new ReadToolsSAMFileWriterFactory()
                    .setUseAsyncIo(nThreads != 1);
        }
        return samFileWriterFactory;
    }

    private SamReaderFactory getSamReaderFactoryFromCommandLine() {
        if (samReaderFactory == null) {
            // Picard is taking care of the validation stringency
            // we added the reference sequence for the CRAM files
            samReaderFactory = SamReaderFactory.makeDefault()
                    .referenceSequence(REFERENCE_SEQUENCE);
        }
        return samReaderFactory;
    }

    /**
     * Get FASTQ split writers for the input, either spliting by barcodes or not
     *
     * @param prefix     the output prefix
     * @param dictionary the barcode dictionary; if <code>null</code>, it won't split by barcode
     * @param single     single end?
     *
     * @return the writer for splitting
     */
    public SplitFastqWriter getFastqSplitWritersFromInput(String prefix,
            BarcodeDictionary dictionary, boolean single) throws IOException {
        if (dictionary != null) {
            return getFastqFactoryFromCommandLine()
                    .newSplitByBarcodeWriter(prefix, dictionary, !single);
        } else {
            return getFastqFactoryFromCommandLine()
                    .newSplitAssignUnknownBarcodeWriter(prefix, !single);
        }
    }

    /**
     * Get a FASTQ writer either single or pair for the input
     *
     * @param single single end?
     *
     * @return FastqWriter for single; PairFastqWriter for paired end
     */
    public ReadToolsFastqWriter getSingleOrPairWriter(String prefix,
            boolean single) throws IOException {
        if (single) {
            return getFastqFactoryFromCommandLine().newWriter(prefix);
        } else {
            return getFastqFactoryFromCommandLine().newPairWriter(prefix);
        }
    }

    /**
     * Get a SAMFileWriter for adding records with RG from the barcode dictionary (split or not)
     *
     * @param prefix     the output prefix
     * @param header     header with read group information (not the original)
     * @param dictionary the barcode dictionary; if <code>null</code>, it won't split by barcode
     * @param bam        should be the output a bam file?
     *
     * @return the writer for splitting or not
     */
    public SplitSAMFileWriter getBamWriterOrSplitWriterFromInput(final String prefix,
            final SAMFileHeader header, final BarcodeDictionary dictionary, final boolean bam)
            throws IOException {
        if (dictionary != null) {
            return getSamFactoryFromCommandLine()
                    .makeSplitByBarcodeWriter(header, prefix, bam, dictionary);
        } else {
            return getSamFactoryFromCommandLine()
                    .makeSplitAssignUnknownBarcodeWriter(header, prefix, bam);
        }
    }

    /**
     * Get a FastqReader for single end (if input2 is <code>null</code>) or pair-end (if input2 is
     * not
     * <code>null</code>, both in standardize format (isMaintained <code>false</code>) or in the
     * same format
     * (isMaintained <code>true</code>)
     *
     * @param input1 the input for the first pair
     * @param input2 the input for the second pair; <code>null</code> if it is single
     *               end processing
     *
     * @return the reader for the file(s)
     */
    public FastqReaderInterface getFastqReaderFromInputs(final File input1, final File input2) {
        FastqReaderInterface toReturn;
        if (input2 == null) {
            toReturn =
                    (maintainFormat) ? new FastqReaderSingleImpl(input1, allowHigherSangerQualities)
                            : new FastqReaderSingleSanger(input1, allowHigherSangerQualities);
        } else {
            toReturn = (maintainFormat) ?
                    new FastqReaderPairedImpl(input1, input2, allowHigherSangerQualities) :
                    new FastqReaderPairedSanger(input1, input2, allowHigherSangerQualities);
        }
        return toReturn;
    }

    /**
     * Get the SamReader for the input, maintaining or not the format
     *
     * @param input the input BAM/SAM file
     *
     * @return the reader for the file
     */
    public SamReader getSamReaderFromInput(File input) {
        // Picard is taking care of the validation stringency
        if (maintainFormat) {
            // if the format is maintained, create a default sam reader
            return new SamReaderImpl(input, getSamReaderFactoryFromCommandLine(),
                    allowHigherSangerQualities);
        } else {
            // if not, standardize
            return new SamReaderSanger(input, getSamReaderFactoryFromCommandLine(),
                    allowHigherSangerQualities);
        }
    }

}
