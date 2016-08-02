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
package org.magicdgs.tools.implemented;

import static org.magicdgs.tools.cmd.OptionUtils.getUniqueValue;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.io.readers.fastq.FastqReaderInterface;
import org.magicdgs.io.readers.fastq.paired.FastqReaderPairedInterface;
import org.magicdgs.io.readers.fastq.single.FastqReaderSingleInterface;
import org.magicdgs.io.writers.fastq.SplitFastqWriter;
import org.magicdgs.methods.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.methods.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.tools.AbstractTool;
import org.magicdgs.tools.cmd.BarcodeOptions;
import org.magicdgs.tools.cmd.CommonOptions;
import org.magicdgs.tools.cmd.ToolWritersFactory;
import org.magicdgs.tools.cmd.ToolsReadersFactory;
import org.magicdgs.utils.loggers.FastqLogger;
import org.magicdgs.utils.misc.IOUtils;
import org.magicdgs.utils.record.FastqRecordUtils;

import htsjdk.samtools.fastq.FastqRecord;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Tool for split by barcode (in the read name) for FASTQ files
 *
 * @author Daniel Gómez-Sánchez
 */
public class FastqBarcodeDetector extends AbstractTool {

    @Override
    protected void runThrowingExceptions(CommandLine cmd) throws Exception {
        // PARSING THE COMMAND LINE
        File input1 = new File(getUniqueValue(cmd, "input1"));
        // input file 2
        String input2string = getUniqueValue(cmd, "input2");
        File input2 = (input2string == null) ? null : new File(input2string);
        String outputPrefix = getUniqueValue(cmd, "output");
        int nThreads = CommonOptions.numberOfThreads(logger, cmd);
        boolean multi = nThreads != 1;
        // logging command line
        logCmdLine(cmd);
        // open the decoder
        BarcodeDecoder decoder = BarcodeOptions.getBarcodeDecoderFromOption(logger, cmd, -1);
        // create the reader and the writer
        FastqReaderInterface reader = ToolsReadersFactory
                .getFastqReaderFromInputs(input1, input2, CommonOptions.isMaintained(logger, cmd),
                        CommonOptions.allowHigherQualities(logger, cmd));
        SplitFastqWriter writer = ToolWritersFactory.getFastqSplitWritersFromInput(outputPrefix,
                BarcodeOptions.isSplit(logger, cmd) ? decoder.getDictionary() : null,
                cmd.hasOption(CommonOptions.disableZippedOutput.getOpt()), multi, input2 == null);
        // run the method
        run(reader, writer, IOUtils.makeMetricsFile(outputPrefix), decoder);
    }

    /**
     * Run based on the reader the single or pair-end mode
     *
     * @param reader  the reader
     * @param writer  the writer
     * @param decoder the barcode decoder instance
     *
     * @throws IOException if there are some problems with the files
     */
    private void run(FastqReaderInterface reader, SplitFastqWriter writer, File metrics,
            BarcodeDecoder decoder)
            throws IOException {
        FastqLogger progress = new FastqLogger(logger);
        if (reader instanceof FastqReaderSingleInterface) {
            logger.debug("Running single end");
            runSingle((FastqReaderSingleInterface) reader, writer, decoder, progress);
        } else if (reader instanceof FastqReaderPairedInterface) {
            logger.debug("Running paired end");
            runPaired((FastqReaderPairedInterface) reader, writer, decoder, progress);
        } else {
            logger.debug(
                    "ERROR: FastqReaderInterface is not an instance of Single or Paired interfaces");
            throw new IllegalArgumentException("Unreachable code");
        }
        progress.logNumberOfVariantsProcessed();
        decoder.logMatcherResult(logger);
        decoder.outputStats(metrics);
        writer.close();
        reader.close();
    }

    /**
     * Run single-end mode
     *
     * @param reader  the reader
     * @param writer  the writer
     * @param decoder the barcode decoder instance
     *
     * @throws IOException if there are some problems with the files
     */
    private void runSingle(FastqReaderSingleInterface reader, SplitFastqWriter writer,
            BarcodeDecoder decoder,
            FastqLogger progress) throws IOException {
        Iterator<FastqRecord> it = reader.iterator();
        while (it.hasNext()) {
            FastqRecord record = it.next();
            // TODO: checks if it is working perfectly
            // String barcode = FastqRecordUtils.getBarcodeInName(record);
            String[] barcode = FastqRecordUtils.getBarcodesInName(record);
            String best = decoder.getBestBarcode(barcode);
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                writer.write(best, record);
            } else {
                writer.write(best, FastqRecordUtils.changeBarcodeInSingle(record, best));
            }
            progress.add();
        }
    }

    /**
     * Run pair-end mode
     *
     * @param reader  the reader
     * @param writer  the writer
     * @param decoder the barcode decoder instance
     *
     * @throws IOException if there are some problems with the files
     */
    private void runPaired(FastqReaderPairedInterface reader, SplitFastqWriter writer,
            BarcodeDecoder decoder,
            FastqLogger progress) throws IOException {
        Iterator<FastqPairedRecord> it = reader.iterator();
        while (it.hasNext()) {
            FastqPairedRecord record = it.next();
            // TODO: checks if it is working perfectly
            // String barcode = FastqRecordUtils.getBarcodeInName(record);
            String[] barcode = FastqRecordUtils.getBarcodesInName(record);
            String best = decoder.getBestBarcode(barcode);
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                writer.write(best, record);
            } else {
                writer.write(best, FastqRecordUtils.changeBarcodeInPaired(record, best));
            }
            progress.add();
        }
    }

    @Override
    protected Options programOptions() {
        Option input1 = Option.builder("i1").longOpt("input1")
                .desc("The input file, or the input file of the first read, in FASTQ format")
                .hasArg()
                .numberOfArgs(1).argName("input_1.fq").required(true).build();
        Option input2 = Option.builder("i2").longOpt("input2").desc(
                "The FASTQ input file of the second read. In case this file is provided the software will switch to paired read mode instead of single read mode")
                .hasArg().numberOfArgs(1).argName("input_2.fq").optionalArg(true).build();
        Option output =
                Option.builder("o").longOpt("output").desc("The output file prefix").hasArg()
                        .numberOfArgs(1)
                        .argName("output_prefix").required(true).build();
        // TODO: change for the default when updated to the combination with the separator between barcodes
        // Option max = Option.builder("m").longOpt("maximum-mismatches").desc(
        //	"Maximum number of mismatches allowed for a matched barcode.  [Default="
        //		+ BarcodeDecoder.DEFAULT_MAXIMUM_MISMATCHES + "]").hasArg().numberOfArgs(1).argName("INT")
        //				   .required(false).build();
        // TODO: change for the default when updated to the combination with the separator between barcodes
        // Option dist = Option.builder("d").longOpt("minimum-distance").desc(
        //	"Minimum distance between the best match and the second to consider a match. [Default="
        //		+ BarcodeDecoder.DEFAULT_MIN_DIFFERENCE_WITH_SECOND + "]").hasArg().numberOfArgs(1).argName("INT")
        //					.required(false).build();
        // create the options
        Options options = new Options();
        // add the options
        options.addOption(input1);
        options.addOption(input2);
        options.addOption(output);
        // TODO: change for adding all when implemented combined barcode with "_"
        // options.addOption(max);
        // options.addOption(dist);
        // add options for barcode programs
        BarcodeOptions.addAllBarcodeCommonOptionsTo(options);
        // TODO: remove following lines
        // options.addOption(BarcodeOptions.barcodes);
        // options.addOption(BarcodeOptions.nNoMismatch);
        // options.addOption(BarcodeOptions.split);
        // options.addOption(BarcodeOptions.maxN);
        // default options
        // add common options
        options.addOption(CommonOptions.maintainFormat); // maintain the format
        options.addOption(CommonOptions.allowHigherSangerQualities); // allow higher qualities
        options.addOption(CommonOptions.disableZippedOutput); // disable zipped output
        options.addOption(CommonOptions.parallel); // allow parallel output
        return options;
    }
}
