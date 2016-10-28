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

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.io.writers.fastq.SplitFastqWriter;
import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.cmd.argumentcollections.BarcodeArgumentCollection;
import org.magicdgs.readtools.tools.AbstractTool;
import org.magicdgs.readtools.tools.ToolNames;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.tools.cmd.OptionUtils;
import org.magicdgs.readtools.tools.cmd.ToolWritersFactory;
import org.magicdgs.readtools.tools.cmd.ToolsReadersFactory;
import org.magicdgs.readtools.utils.fastq.BarcodeMethods;
import org.magicdgs.readtools.utils.logging.ProgressLoggerExtension;
import org.magicdgs.readtools.utils.misc.Formats;
import org.magicdgs.readtools.utils.misc.IOUtils;
import org.magicdgs.readtools.utils.record.SAMRecordUtils;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;

/**
 * Class for converting from a Barcoded BAM to a FASTQ.
 *
 * Description: Because some sequencing companies/services provide a barcoded BAM file instead of a
 * FASTQ this tool
 * converts the BAM file into the latter. It works with one or two barcodes, pair-end (interleaved
 * BAM file) and
 * single-end sequencing. In addition, it matches the sequenced barcodes with the used ones and
 * discards some reads that
 * could not be matched, and adds the exact detected barcode to the read name. The method to assign
 * barcodes is the
 * following: if there is an exact match for a unique barcode, it is directly assigned; if there is
 * more than 1 barcode,
 * it assigns the read to the sample with which most barcodes match; otherwise, the read is
 * discarded. If the barcode in
 * the input file is larger than the sequenced barcode the last base from the input barcode is
 * ignored.
 *
 * @author Daniel Gómez-Sánchez
 */
public class TaggedBamToFastq extends AbstractTool {

    /**
     * Default barcode tag for the first barcode
     */
    private static final String DEFAULT_BARCODE_TAG1 = "BC";

    /**
     * Default barcode tag for the second barcode
     */
    private static final String DEFAULT_BARCODE_TAG2 = "B2";

    @Override
    protected void runThrowingExceptions(CommandLine cmd) throws Exception {
        // parsing command line
        String inputString =
                OptionUtils.getUniqueValue(cmd, ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME);
        String outputPrefix = OptionUtils
                .getUniqueValue(cmd, ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME);
        String[] tags = cmd.getOptionValues("t");
        int nThreads = ReadToolsLegacyArgumentDefinitions.numberOfThreads(logger, cmd);
        boolean multi = nThreads != 1;
        // FINISH PARSING: log the command line (not longer in the param file)
        logCmdLine(cmd);
        // open the decoder
        BarcodeDecoder decoder = BarcodeArgumentCollection
                .getBarcodeDecoderFromOption(logger, cmd, -1);
        if (tags == null) {
            tags = (decoder.getDictionary().getNumberOfBarcodes() == 1) ?
                    new String[] {DEFAULT_BARCODE_TAG1} :
                    new String[] {DEFAULT_BARCODE_TAG1, DEFAULT_BARCODE_TAG2};
        }
        if (tags.length != decoder.getDictionary().getNumberOfBarcodes()) {
            throw new ToolNames.ToolException(
                    "Number of barcodes in the file is different for the number of barcodes provided/detected");
        }
        // open the bam file
        SamReader input = ToolsReadersFactory
                .getSamReaderFromInput(new File(inputString),
                        ReadToolsLegacyArgumentDefinitions.isMaintained(logger, cmd),
                        ReadToolsLegacyArgumentDefinitions.allowHigherQualities(logger, cmd));
        // Create the writer factory
        SplitFastqWriter writer = ToolWritersFactory.getFastqSplitWritersFromInput(outputPrefix,
                BarcodeArgumentCollection.isSplit(logger, cmd) ? decoder.getDictionary() : null,
                cmd.hasOption(ReadToolsLegacyArgumentDefinitions.disableZippedOutput.getOpt()),
                multi,
                cmd.hasOption("s"));
        // create the metrics file
        File metrics = IOUtils.makeMetricsFile(outputPrefix);
        // run it!
        run(input, writer, metrics, decoder, tags, cmd.hasOption("s"));
        // close the readers and writers
        input.close();
        writer.close();
    }

    /**
     * Run with single or paired end
     *
     * @param reader  the input reader
     * @param writer  the output
     * @param decoder the decoder to use to split
     * @param tags    the tags where the barcodes are
     * @param single  it is single end?
     */
    private void run(SamReader reader, SplitFastqWriter writer, File metrics,
            BarcodeDecoder decoder, String[] tags,
            boolean single) throws IOException {
        ProgressLoggerExtension progress;
        // single end processing
        int pf;
        if (single) {
            progress = new ProgressLoggerExtension(logger, 1000000, "Processed", "records");
            pf = runSingle(reader, writer, decoder, tags, progress);
        } else {
            progress = new ProgressLoggerExtension(logger, 1000000, "Processed", "pairs");
            pf = runPaired(reader, writer, decoder, tags, progress);
        }
        progress.logNumberOfVariantsProcessed();
        if (pf != 0) {
            logger.warn(Formats.commaFmt.format(pf), (single) ? " records " : " pairs ",
                    "fails vendor quality (PF flag) and are discarded");
        }
        decoder.logMatcherResult(logger);
        decoder.outputStats(metrics);
    }

    /**
     * Run the pair-end mode
     *
     * @param reader  the input reader
     * @param writer  the output
     * @param matcher the matcher to use to split
     * @param tags    the tags where the barcodes are
     *
     * @return the number of reads that fails the vendor quality
     */
    private int runPaired(SamReader reader, SplitFastqWriter writer, BarcodeDecoder matcher,
            String[] tags,
            ProgressLoggerExtension progress) {
        int pf = 0;
        SAMRecordIterator it = reader.iterator();
        while (it.hasNext()) {
            SAMRecord record1 = it.next();
            if (!it.hasNext()) {
                throw new SAMException("Truncated interleaved BAM file");
            }
            SAMRecord record2 = it.next();
            String best;
            String[] barcodes = getBarcodeFromTags(record1, tags);
            if (!record1.getReadFailsVendorQualityCheckFlag()) {
                best = matcher.getBestBarcode(barcodes);
            } else {
                best = BarcodeMatch.UNKNOWN_STRING;
            }
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                SAMRecordUtils.addBarcodeToName(record1, BarcodeMethods.joinBarcodes(barcodes));
                SAMRecordUtils.addBarcodeToName(record2, BarcodeMethods.joinBarcodes(barcodes));
            } else {
                SAMRecordUtils.addBarcodeToName(record1, best);
                SAMRecordUtils.addBarcodeToName(record2, best);
            }
            FastqPairedRecord outputRecord =
                    new FastqPairedRecord(SAMRecordUtils.toFastqRecord(record1, 1),
                            SAMRecordUtils.toFastqRecord(record2, 2));
            writer.write(best, outputRecord);
            progress.record(record1);
        }
        return pf;
    }

    /**
     * Run the single-end mode
     *
     * @param reader  the input reader
     * @param writer  the output
     * @param matcher the matcher to use to split
     * @param tags    the tags where the barcodes are
     *
     * @return the number of reads that fails the vendor quality
     */
    private int runSingle(SamReader reader, SplitFastqWriter writer, BarcodeDecoder matcher,
            String[] tags,
            ProgressLoggerExtension progress) {
        int pf = 0;
        for (SAMRecord record : reader) {
            String[] barcodes = getBarcodeFromTags(record, tags);
            String best;
            if (!record.getReadFailsVendorQualityCheckFlag()) {
                best = matcher.getBestBarcode(barcodes);
            } else {
                best = BarcodeMatch.UNKNOWN_STRING;
            }
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                SAMRecordUtils.addBarcodeToName(record, BarcodeMethods.joinBarcodes(barcodes));
            } else {
                SAMRecordUtils.addBarcodeToName(record, best);
            }
            writer.write(best, SAMRecordUtils.toFastqRecord(record, null));
            progress.record(record);
        }
        return pf;
    }

    /**
     * Get the all the barcodes from the provided tags
     *
     * @param record the record to extract the barcodes from
     * @param tags   the tags where the barcodes are
     *
     * @return the barcodes in the order of the tags
     */
    private static String[] getBarcodeFromTags(SAMRecord record, String... tags) {
        String[] toReturn = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            toReturn[i] = getBarcodeFromTag(record, tags[i]);
        }
        return toReturn;
    }

    /**
     * Get a barcode from an unique tag
     *
     * @param record the record to extract the barcodes from
     * @param tag    the tag where the requested barcode is
     *
     * @return the barcode for this tag
     *
     * @throws htsjdk.samtools.SAMException if the barcode is not found
     */
    private static String getBarcodeFromTag(SAMRecord record, String tag) {
        String barcode = record.getStringAttribute(tag);
        if (barcode == null) {
            throw new SAMException(tag + " not found in record " + record);
        }
        return barcode;
    }

    @Override
    protected Options programOptions() {
        Option input = Option.builder(ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME)
                .longOpt(ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME)
                .desc("Input BAM/SAM file. If pair-end, it should be interleaved").hasArg()
                .argName("INPUT.bam").numberOfArgs(1).required().build();
        Option output = Option.builder(ReadToolsLegacyArgumentDefinitions.OUTPUT_SHORT_NAME)
                .longOpt(ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME)
                .desc("FASTQ output prefix").hasArg()
                .argName("OUTPUT_PREFIX").numberOfArgs(1).required().build();
        Option tag = Option.builder("t").longOpt("tag").desc(
                "Tag in the BAM file for the stored barcodes. It should be provided the same number of times as barcodes provided in the file. Default: "
                        + DEFAULT_BARCODE_TAG1 + " for the first barcode, " + DEFAULT_BARCODE_TAG2
                        + " for the second.")
                .hasArg().numberOfArgs(1).argName("TAG").required(false).build();
        Option single = Option.builder("s").longOpt("single").desc("Switch to single-end parsing")
                .hasArg(false)
                .required(false).build();
        Options options = new Options();
        options.addOption(single);
        options.addOption(tag);
        options.addOption(output);
        options.addOption(input);
        // add options for barcode programs
        BarcodeArgumentCollection.addAllBarcodeCommonOptionsTo(options);
        // add common options
        options.addOption(ReadToolsLegacyArgumentDefinitions.maintainFormat); // maintain the format
        options.addOption(
                ReadToolsLegacyArgumentDefinitions.allowHigherSangerQualities); // allow higher qualities
        options.addOption(
                ReadToolsLegacyArgumentDefinitions.disableZippedOutput); // disable zipped output
        options.addOption(ReadToolsLegacyArgumentDefinitions.parallel);
        return options;
    }
}