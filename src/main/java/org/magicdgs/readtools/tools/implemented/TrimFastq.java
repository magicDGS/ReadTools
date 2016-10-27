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
 */
package org.magicdgs.readtools.tools.implemented;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.io.readers.fastq.FastqReaderInterface;
import org.magicdgs.io.readers.fastq.paired.FastqReaderPairedInterface;
import org.magicdgs.io.readers.fastq.single.FastqReaderSingleInterface;
import org.magicdgs.io.writers.fastq.ReadToolsFastqWriter;
import org.magicdgs.io.writers.fastq.SplitFastqWriter;
import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.cmd.argumentcollections.TrimmingArgumentCollection;
import org.magicdgs.readtools.tools.AbstractTool;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.tools.cmd.OptionUtils;
import org.magicdgs.readtools.tools.cmd.ToolWritersFactory;
import org.magicdgs.readtools.tools.cmd.ToolsReadersFactory;
import org.magicdgs.readtools.tools.trimming.trimmers.Trimmer;
import org.magicdgs.readtools.utils.logging.FastqLogger;
import org.magicdgs.readtools.utils.misc.IOUtils;

import htsjdk.samtools.fastq.FastqRecord;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;

/**
 * Class that implements the trimming algorithm from Kofler et al. 2011
 *
 * @author Daniel Gómez-Sánchez
 */
public class TrimFastq extends AbstractTool {

    /**
     * The default quality score
     */
    private static final int DEFAULT_QUALTITY_SCORE = 20;

    /**
     * The default minimum length
     */
    private static final int DEFAULT_MINIMUM_LENGTH = 40;

    @Override
    protected void runThrowingExceptions(CommandLine cmd) throws Exception {
        // The input file
        File input1 = new File(
                OptionUtils.getUniqueValue(cmd,
                        ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME + "1"));
        // input file 2
        String input2string = OptionUtils
                .getUniqueValue(cmd, ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME + "2");
        File input2 = (input2string == null) ? null : new File(input2string);
        // The output prefix
        String output_prefix = OptionUtils.getUniqueValue(cmd, "output");
        // multi-thread?
        int nThreads = ReadToolsLegacyArgumentDefinitions.numberOfThreads(logger, cmd);
        boolean multi = (nThreads != 1);
        // FINISH PARSING: log the command line (not longer in the param file)
        logCmdLine(cmd);
        // save the gzip option
        boolean dgzip = ReadToolsLegacyArgumentDefinitions.isZipDisable(cmd);
        // save the keep_discard option
        boolean keepDiscard = cmd.hasOption("k");
        // save the maintained format option
        boolean isMaintained = ReadToolsLegacyArgumentDefinitions.isMaintained(logger, cmd);
        // open the reader
        FastqReaderInterface reader = ToolsReadersFactory
                .getFastqReaderFromInputs(input1, input2, isMaintained,
                        ReadToolsLegacyArgumentDefinitions.allowHigherQualities(logger, cmd));
        boolean single = !(reader instanceof FastqReaderPairedInterface);
        // open the writer
        ReadToolsFastqWriter writer = (keepDiscard) ?
                ToolWritersFactory
                        .getFastqSplitWritersFromInput(output_prefix, null, dgzip, multi, single) :
                ToolWritersFactory.getSingleOrPairWriter(output_prefix, dgzip, multi, single);
        // create the trimmer
        // create the MottAlgorithm
        Trimmer trimmer = TrimmingArgumentCollection.getTrimmer(cmd, single);
        // run it!
        process(trimmer, reader, writer, IOUtils.makeMetricsFile(output_prefix));
    }

    /**
     * Process the data depending on the reader status (if it is for single or pair-end
     *
     * @param trimmer     the algorithm with the provided settings
     * @param reader      the reader, either for pairs or single end
     * @param writer      the writer for the pairs
     * @param metricsFile the file to output the metrics for the trimming
     *
     * @throws IOException if there are problems with the files
     */
    private void process(Trimmer trimmer, FastqReaderInterface reader, ReadToolsFastqWriter writer,
            File metricsFile)
            throws IOException {
        FastqLogger progress;
        if (reader instanceof FastqReaderSingleInterface) {
            logger.debug("Running single end");
            progress = new FastqLogger(logger, 1000000, "Processed", "read-pairs");
            processSE(trimmer, (FastqReaderSingleInterface) reader, writer, progress);
        } else if (reader instanceof FastqReaderPairedInterface) {
            logger.debug("Running paired end");
            progress = new FastqLogger(logger);
            processPE(trimmer, (FastqReaderPairedInterface) reader, writer, progress);
        } else {
            logger.debug(
                    "ERROR: FastqReaderInterface is not an instance of Single or Paired interfaces");
            throw new IllegalArgumentException("Unreachable code");
        }
        // final line of progress
        progress.logNumberOfVariantsProcessed();
        // print the metrics file
        trimmer.printTrimmerMetrics(metricsFile);
        // close the readers
        reader.close();
        writer.close();
    }

    /**
     * Process the files in pair-end mode
     *
     * @param trimmer the algorithm with the provided settings
     * @param reader  the reader for the pairs
     * @param writer  the writer for the pairs (instance of PairFastqWriters)
     *
     * @throws IOException if there are problems with the files
     */
    private static void processPE(Trimmer trimmer, FastqReaderPairedInterface reader,
            ReadToolsFastqWriter writer,
            FastqLogger progress) throws IOException {
        boolean keep = (writer instanceof SplitFastqWriter);
        while (reader.hasNext()) {
            FastqPairedRecord record = reader.next();
            FastqPairedRecord newRecord =
                    trimmer.trimFastqPairedRecord(record, reader.getFastqQuality());
            if (newRecord.isComplete()) {
                writer.write(newRecord);
            } else if (newRecord.containRecords()) {
                if (newRecord.getRecord1() == null) {
                    writer.write(newRecord.getRecord2());
                    if (keep) {
                        ((SplitFastqWriter) writer)
                                .write(BarcodeMatch.UNKNOWN_STRING, record.getRecord1());
                    }
                } else {
                    writer.write(newRecord.getRecord1());
                    if (keep) {
                        ((SplitFastqWriter) writer)
                                .write(BarcodeMatch.UNKNOWN_STRING, record.getRecord2());
                    }
                }
            } else {
                if (keep) {
                    ((SplitFastqWriter) writer).write(BarcodeMatch.UNKNOWN_STRING, record);
                }
            }
            progress.add();
        }
    }

    /**
     * Process the files in single-end mode
     *
     * @param trimmer the algorithm with the provided settings
     * @param reader  the reader for the single end file
     * @param writer  the writer for the single end file * @param metricsFile
     *
     * @throws IOException if there are problems with the files
     */
    private static void processSE(Trimmer trimmer, FastqReaderSingleInterface reader,
            ReadToolsFastqWriter writer,
            FastqLogger progress) throws IOException {
        boolean keep = (writer instanceof SplitFastqWriter);
        while (reader.hasNext()) {
            FastqRecord record = reader.next();
            FastqRecord newRecord = trimmer.trimFastqRecord(record, reader.getFastqQuality());
            if (newRecord != null) {
                writer.write(newRecord);
            } else {
                if (keep) {
                    ((SplitFastqWriter) writer).write(BarcodeMatch.UNKNOWN_STRING, record);
                }
            }
            progress.add();
        }
    }

    @Override
    protected Options programOptions() {
        // Creating each options
        Option input1 = Option.builder(ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME + "1")
                .longOpt(ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME + "1")
                .desc("The FASTQ input file, or the input file of the first read").hasArg()
                .numberOfArgs(1).argName("input_1.fq").required(true).build();
        Option input2 = Option.builder(ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME + "2")
                .longOpt(ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME + "2").desc(
                        "The FASTQ input file of the second read. In case this file is provided the software will switch to paired read mode instead of single read mode")
                .hasArg().numberOfArgs(1).argName("input_2.fq").optionalArg(true).build();
        Option output = Option.builder(ReadToolsLegacyArgumentDefinitions.OUTPUT_SHORT_NAME)
                .longOpt(ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME)
                .desc("The output file prefix. Will be in fastq. Mandatory parameter").hasArg()
                .numberOfArgs(1).argName("output_prefix").required(true).build();
        Options options = new Options();
        TrimmingArgumentCollection.addTrimmingArguments(options);
        options.addOption(output);
        options.addOption(input2);
        options.addOption(input1);
        // adding common options
        options.addOption(ReadToolsLegacyArgumentDefinitions.maintainFormat); // maintain the format
        options.addOption(
                ReadToolsLegacyArgumentDefinitions.allowHigherSangerQualities); // allow higher qualities
        options.addOption(
                ReadToolsLegacyArgumentDefinitions.disableZippedOutput); // disable the zipped output
        options.addOption(ReadToolsLegacyArgumentDefinitions.parallel); // parallelization allowed
        return options;
    }
}
