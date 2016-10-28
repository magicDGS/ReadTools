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
package org.magicdgs.readtools.tools.barcodes;

import org.magicdgs.io.writers.bam.SplitSAMFileWriter;
import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.cmd.argumentcollections.BarcodeArgumentCollection;
import org.magicdgs.readtools.tools.AbstractTool;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionaryFactory;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.readtools.tools.cmd.OptionUtils;
import org.magicdgs.readtools.tools.cmd.ToolWritersFactory;
import org.magicdgs.readtools.tools.cmd.ToolsReadersFactory;
import org.magicdgs.readtools.utils.logging.ProgressLoggerExtension;
import org.magicdgs.readtools.utils.misc.IOUtils;
import org.magicdgs.readtools.utils.record.SAMRecordUtils;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * Tool for split by barcode (in the read name) for BAM files
 *
 * @author Daniel Gómez-Sánchez
 */
public class BamBarcodeDetector extends AbstractTool {

    @Override
    protected void runThrowingExceptions(CommandLine cmd) throws Exception {
        // PARSING THE COMMAND LINE
        File input = new File(OptionUtils
                .getUniqueValue(cmd, ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME));
        String outputPrefix = OptionUtils
                .getUniqueValue(cmd, ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME);
        boolean bamFormat = !cmd.hasOption("s");
        boolean index = cmd.hasOption("ind");
        int nThreads = ReadToolsLegacyArgumentDefinitions.numberOfThreads(logger, cmd);
        boolean multi = nThreads != 1;
        // logging command line
        logCmdLine(cmd);
        // open the decoder with its corresponding dictionary
        BarcodeDecoder decoder = BarcodeArgumentCollection
                .getBarcodeDecoderFromOption(logger, cmd, -1);
        // open the reader
        SamReader reader = ToolsReadersFactory
                .getSamReaderFromInput(input, ReadToolsLegacyArgumentDefinitions
                                .isMaintained(logger, cmd),
                        ReadToolsLegacyArgumentDefinitions.allowHigherQualities(logger, cmd));
        // create the new header adding the read groups
        SAMFileHeader header = reader.getFileHeader();
        addReadGroupToHeader(header, decoder.getDictionary());
        header.addProgramRecord(getToolProgramRecord(cmd));
        // create the BAM writer
        SplitSAMFileWriter writer =
                ToolWritersFactory.getBamWriterOrSplitWriterFromInput(outputPrefix, header,
                        BarcodeArgumentCollection.isSplit(logger, cmd) ? decoder.getDictionary()
                                : null,
                        bamFormat, index, multi);
        addReadGroupByBarcode(reader, writer, IOUtils.makeMetricsFile(outputPrefix), decoder);
    }

    /**
     * Run the program
     */
    private void addReadGroupByBarcode(SamReader reader, SplitSAMFileWriter writer, File metrics,
            BarcodeDecoder decoder) throws IOException {
        ProgressLoggerExtension progress = new ProgressLoggerExtension(logger);
        SAMRecordIterator it = reader.iterator();
        while (it.hasNext()) {
            SAMRecord record = it.next();
            // TODO: test if the new method is working properly
            // String barcode = SAMRecordUtils.getBarcodeInName(record);
            String[] barcode = SAMRecordUtils.getBarcodesInName(record);
            String best = decoder.getBestBarcode(barcode);
            SAMReadGroupRecord rg = decoder.getDictionary().getReadGroupFor(best);
            if (!rg.equals(BarcodeDictionaryFactory.UNKNOWN_READGROUP_INFO)) {
                SAMRecordUtils.changeBarcodeInName(record, best);
            }
            record.setAttribute("RG", rg.getId());
            writer.addAlignment(record);
            progress.record(record);
        }
        progress.logNumberOfVariantsProcessed();
        decoder.logMatcherResult(logger);
        decoder.outputStats(metrics);
        writer.close();
        reader.close();
    }

    /**
     * Add the read group to the provided header and return the mapping between sample and read
     * group record
     *
     * @param header     the header to update
     * @param dictionary the dictionary with the information for each sample
     *
     * @return a mapping with the sample and the SAMReadGroupRecord
     */
    private void addReadGroupToHeader(SAMFileHeader header, BarcodeDictionary dictionary) {
        HashSet<SAMReadGroupRecord> sampleSet = new HashSet<>(dictionary.getSampleReadGroups());
        for (SAMReadGroupRecord sample : sampleSet) {
            // catch the error and output a warning if the barcode already exists
            try {
                header.addReadGroup(sample);
            } catch (IllegalArgumentException e) {
                logger.warn("Read Group ", sample.getId(),
                        " found in original file: previous tags will be removed");
                final SAMReadGroupRecord rg = header.getReadGroup(sample.getId());
                for (String tag : SAMReadGroupRecord.STANDARD_TAGS) {
                    rg.setAttribute(tag, sample.getAttribute(tag));
                }
            }
        }
    }

    @Override
    protected Options programOptions() {
        Option input1 = Option.builder(ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME)
                .longOpt(ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME)
                .desc("The input BAM file with barcodes in the read name")
                .hasArg().numberOfArgs(1).argName("input.bam").required(true).build();
        Option output =
                Option.builder(ReadToolsLegacyArgumentDefinitions.OUTPUT_SHORT_NAME)
                        .longOpt(ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME)
                        .desc("The output file prefix").hasArg()
                        .numberOfArgs(1)
                        .argName("output_prefix").required(true).build();
        Option samFormat = Option.builder("s").longOpt("sam")
                .desc("Output will be in sam format instead of bam")
                .hasArg(false).required(false).build();
        Option index =
                Option.builder("ind").longOpt("index").desc("Index the output file").hasArg(false)
                        .required(false).build();
        // create the options
        Options options = new Options();
        // add the options
        options.addOption(input1);
        options.addOption(output);
        options.addOption(samFormat);
        options.addOption(index);
        // add options for read groups
        BarcodeArgumentCollection.addAllReadGroupCommonOptionsTo(options);
        // add options for barcode programs
        BarcodeArgumentCollection.addAllBarcodeCommonOptionsTo(options);
        // add common options
        options.addOption(ReadToolsLegacyArgumentDefinitions.maintainFormat); // maintain the format
        options.addOption(
                ReadToolsLegacyArgumentDefinitions.allowHigherSangerQualities); // allow higher qualities
        options.addOption(
                ReadToolsLegacyArgumentDefinitions.disableZippedOutput); // disable zipped output
        options.addOption(ReadToolsLegacyArgumentDefinitions.parallel); // allow parallel output
        return options;
    }
}
