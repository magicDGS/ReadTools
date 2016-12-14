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
import org.magicdgs.readtools.cmd.programgroups.RawDataProgramGroup;
import org.magicdgs.readtools.tools.ReadToolsBaseTool;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.utils.fastq.BarcodeMethods;
import org.magicdgs.readtools.utils.logging.ProgressLoggerExtension;
import org.magicdgs.readtools.utils.misc.Formats;
import org.magicdgs.readtools.utils.misc.IOUtils;
import org.magicdgs.readtools.utils.record.SAMRecordUtils;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for converting from a Barcoded BAM to a FASTQ.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(summary ="Because some sequencing companies/services provide a barcoded BAM file instead of a FASTQ this tool"
                + "converts the BAM file into the latter. It works with one or two barcodes, pair-end (interleaved BAM file) "
                + "and single-end sequencing. In addition, it matches the sequenced barcodes with the used ones and discards some"
                + " reads that could not be matched, and adds the exact detected barcode to the read name. The method to assign"
                + " barcodes is the following: if there is an exact match for a unique barcode, it is directly assigned; if there"
                + " is more than 1 barcode, it assigns the read to the sample with which most barcodes match; otherwise, the read"
                + " is discarded. If the barcode in the input file is larger than the sequenced barcode the last base from the "
                + "input barcode is ignored.",
        oneLineSummary = "Convert an BAM file with BC tags into a FASTQ file.",
        programGroup = RawDataProgramGroup.class)
public final class TaggedBamToFastq extends ReadToolsBaseTool {

    private final static String DEFAULT_BARCODE_TAG1 = "BC";
    private final static String DEFAULT_BARCODE_TAG2 = "B2";

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME, optional = false,
            doc = "Input BAM/SAM file. By default, it is assumed to be interleaved pair-end.")
    public File input = null;

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.OUTPUT_SHORT_NAME, optional = false,
            doc = "The output file prefix")
    public String outputPrefix = null;

    @Argument(fullName = "tag", shortName = "t", optional = true,
            doc = "Tag in the BAM file for the stored barcodes. It should be provided the same number of times as barcodes provided in the file. If not, default values are "
            + DEFAULT_BARCODE_TAG1 + " and " + DEFAULT_BARCODE_TAG2)
    public List<String> inputTags = new ArrayList<>();

    @Argument(fullName = "single", shortName = "s", optional = false, doc = "Switch to single-end parsing.")
    public Boolean single = false;

    @ArgumentCollection
    public BarcodeArgumentCollection barcodeArguments = new BarcodeArgumentCollection();

    private BarcodeDecoder decoder;
    private SamReader reader;
    private SplitFastqWriter writer;
    private String[] tags;

    @Override
    protected void onStartup() {
        super.onStartup();
        try {
            // open the decoder with its corresponding dictionary
            decoder = barcodeArguments.getBarcodeDecoderFromArguments(logger);
            // if they are the defaults just handle them
            if (inputTags.isEmpty()) {
                // set the tags
                tags = (decoder.getDictionary().getNumberOfBarcodes() == 1)
                        ? new String[] {DEFAULT_BARCODE_TAG1}
                        : new String[] {DEFAULT_BARCODE_TAG1, DEFAULT_BARCODE_TAG2};
            } else {
                tags = inputTags.stream().toArray(String[]::new);
            }
            if (tags.length != decoder.getDictionary().getNumberOfBarcodes()) {
                throw new CommandLineException.BadArgumentValue("tag", inputTags.toString(),
                        "number of barcodes in file differ for non-default barcode tags provided");
            }
            logger.debug("Tags: {}", Arrays.toString(tags));
            // create the reader and the writer
            reader = getSamReaderFromInput(input);
            writer = getFastqSplitWritersFromInput(outputPrefix, (barcodeArguments.split) ?
                    decoder.getDictionary() : null, single);
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
    }

    @Override
    protected Object doWork() {
        final ProgressLoggerExtension progress;
        // single end processing
        int pf;
        if (single) {
            progress = new ProgressLoggerExtension(logger, 1000000, "Processed", "records");
            pf = runSingle(progress);
        } else {
            progress = new ProgressLoggerExtension(logger, 1000000, "Processed", "pairs");
            pf = runPaired(progress);
        }
        progress.logNumberOfVariantsProcessed();
        if (pf != 0) {
            logger.warn("{} {} fails vendor quality (PF flag) and were discarded",
                    Formats.commaFmt.format(pf), (single) ? "records" : "pairs");
        }
        decoder.logMatcherResult(logger);
        try {
            decoder.outputStats(IOUtils.makeMetricsFile(outputPrefix).toFile());
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Run the pair-end mode
     *
     * @return the number of reads that fails the vendor quality
     */
    private int runPaired(
            final ProgressLoggerExtension progress) {
        int pf = 0;
        final SAMRecordIterator it = reader.iterator();
        while (it.hasNext()) {
            final SAMRecord record1 = it.next();
            if (!it.hasNext()) {
                throw new SAMException("Truncated interleaved BAM file");
            }
            final SAMRecord record2 = it.next();
            final String[] barcodes = getBarcodeFromTags(record1, tags);
            final String best = getBestBarcodeIfPassVQ(record1, barcodes);
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                final String toReadName = BarcodeMethods.joinBarcodes(barcodes);
                SAMRecordUtils.addBarcodeToName(record1, toReadName);
                SAMRecordUtils.addBarcodeToName(record2, toReadName);
            } else {
                SAMRecordUtils.addBarcodeToName(record1, best);
                SAMRecordUtils.addBarcodeToName(record2, best);
            }
            final FastqPairedRecord outputRecord = new FastqPairedRecord(
                    SAMRecordUtils.toFastqRecord(record1, 1),
                    SAMRecordUtils.toFastqRecord(record2, 2));
            writer.write(best, outputRecord);
            progress.record(record1);
        }
        return pf;
    }

    /**
     * Run the single-end mode
     *
     * @return the number of reads that fails the vendor quality
     */
    private int runSingle(final ProgressLoggerExtension progress) {
        int pf = 0;
        for (SAMRecord record : reader) {
            final String[] barcodes = getBarcodeFromTags(record, tags);
            final String best = getBestBarcodeIfPassVQ(record, barcodes);
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
     * Check the VQ flag and get the best barcode.
     *
     * @param record   the record
     * @param barcodes the raw barcodes extracted
     *
     * @return the best barcode if present, {@link BarcodeMatch#UNKNOWN_STRING} if the read fails
     * vendor quality
     */
    private final String getBestBarcodeIfPassVQ(final SAMRecord record, final String... barcodes) {
        if (record.getReadFailsVendorQualityCheckFlag()) {
            return BarcodeMatch.UNKNOWN_STRING;
        }
        return decoder.getBestBarcode(barcodes);
    }

    /**
     * Get the all the barcodes from the provided tags
     *
     * @param record the record to extract the barcodes from
     * @param tags   the tags where the barcodes are
     *
     * @return the barcodes in the order of the tags
     */
    private static String[] getBarcodeFromTags(final SAMRecord record, final String... tags) {
        final String[] toReturn = new String[tags.length];
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
    private static String getBarcodeFromTag(final SAMRecord record, final String tag) {
        final String barcode = record.getStringAttribute(tag);
        if (barcode == null) {
            throw new SAMException(tag + " not found in record " + record);
        }
        return barcode;
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(writer);
        CloserUtil.close(reader);
    }
}
