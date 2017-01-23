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
import org.magicdgs.io.readers.fastq.FastqReaderInterface;
import org.magicdgs.io.readers.fastq.paired.FastqReaderPairedInterface;
import org.magicdgs.io.readers.fastq.single.FastqReaderSingleInterface;
import org.magicdgs.io.writers.fastq.SplitFastqWriter;
import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.cmd.argumentcollections.BarcodeLegacyArgumentCollection;
import org.magicdgs.readtools.cmd.argumentcollections.ReadNameBarcodeArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.DeprecatedProgramGroup;
import org.magicdgs.readtools.tools.ReadToolsBaseTool;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.metrics.barcodes.MatcherStat;
import org.magicdgs.readtools.utils.fastq.BarcodeMethods;
import org.magicdgs.readtools.utils.fastq.RTFastqContstants;
import org.magicdgs.readtools.utils.logging.FastqLogger;
import org.magicdgs.readtools.utils.misc.Formats;
import org.magicdgs.readtools.utils.misc.IOUtils;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Tool for split by barcode (in the read name) for FASTQ files.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @deprecated this tool correspond to legacy elements.
 */
@CommandLineProgramProperties(oneLineSummary = "DEPRECATED: USE 'AssignReadGroupByBarcode' for identify barcodes in the read name for a FASTQ file and assign to the ones used on the library.",
        summary = "DEPRECATED: USE 'AssignReadGroupByBarcode' instead.\n" +
                "Detect barcodes in the header of the read name (based on the marker "
                + RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER
                + ") and assign to a sample based on a provided dictionary. Barcodes in the input file "
                + "that are larger than the used ones are cut in the last bases.",
        programGroup = DeprecatedProgramGroup.class)
@Deprecated
public final class FastqBarcodeDetector extends ReadToolsBaseTool {

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME + "1", shortName =
            ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME + "1", optional = false,
            doc = "The input file, or the input file of the first read, in FASTQ format.")
    public File input1 = null;

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME + "2", shortName =
            ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME + "2", optional = true,
            doc = "The FASTQ input file of the second read. In case this file is provided the software will switch to paired read mode instead of single read mode.")
    public File input2 = null;

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.OUTPUT_SHORT_NAME, optional = false,
            doc = "The output file prefix")
    public String outputPrefix = null;

    @ArgumentCollection
    public BarcodeLegacyArgumentCollection barcodeArguments = new BarcodeLegacyArgumentCollection();

    @ArgumentCollection
    public ReadNameBarcodeArgumentCollection readNameBarcodeArguments =
            new ReadNameBarcodeArgumentCollection();

    private BarcodeDecoder decoder;
    private FastqReaderInterface reader;
    private SplitFastqWriter writer;

    @Override
    protected void onStartup() {
        super.onStartup();
        try {
            // open the decoder with its corresponding dictionary
            decoder = barcodeArguments.getBarcodeDecoderFromArguments(logger);
            // create the reader and the writer
            reader = getFastqReaderFromInputs(input1, input2);
            writer = getFastqSplitWritersFromInput(outputPrefix, (barcodeArguments.split) ?
                    decoder.getDictionary() : null, input2 == null);
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
    }

    @Override
    protected Object doWork() {
        final FastqLogger progress = new FastqLogger(logger);
        if (reader instanceof FastqReaderSingleInterface) {
            logger.debug("Running single end");
            runSingle(progress);
        } else if (reader instanceof FastqReaderPairedInterface) {
            logger.debug("Running paired end");
            runPaired(progress);
        } else {
            logger.debug(
                    "ERROR: FastqReaderInterface is not an instance of Single or Paired interfaces");
            throw new GATKException.ShouldNeverReachHereException(
                    "Unknown FastqReaderInterface: " + reader.getClass());
        }
        progress.logNumberOfVariantsProcessed();
        try {
            final Collection<MatcherStat> stats = decoder
                    .outputStats(IOUtils.makeMetricsFile(outputPrefix));
            stats.forEach(s -> logger.info("Found {} records for {} ({}).",
                    Formats.commaFmt.format(s.RECORDS), s.SAMPLE, s.BARCODE));
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Run single-end mode
     */
    private void runSingle(final FastqLogger progress) {
        final Iterator<FastqRecord> it = ((FastqReaderSingleInterface) reader).iterator();
        while (it.hasNext()) {
            final FastqRecord record = readNameBarcodeArguments.normalizeRecordName(it.next());
            final String[] barcode = BarcodeMethods.getSeveralBarcodesFromName(record.getReadHeader());
            final String best = decoder.getBestBarcode(barcode);
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                // TODO: get the standard
                writer.write(best, record);
            } else {
                writer.write(best, changeBarcode(record, best, 0));
            }
            progress.add();
        }
    }

    /**
     * Run pair-end mode
     */
    private void runPaired(final FastqLogger progress) {
        final Iterator<FastqPairedRecord> it = ((FastqReaderPairedInterface) reader).iterator();
        while (it.hasNext()) {
            final FastqPairedRecord record =
                    readNameBarcodeArguments.normalizeRecordName(it.next());
            final String[] barcode = pairedBarcodesConsensus(record);
            final String best = decoder.getBestBarcode(barcode);
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                // TODO: get the standard
                writer.write(best, record);
            } else {
                writer.write(best, changeBarcodeInPaired(record, best));
            }
            progress.add();
        }
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(writer);
        CloserUtil.close(reader);
    }

    private static String[] pairedBarcodesConsensus(final FastqPairedRecord record) {
        final String[] barcode1 = BarcodeMethods.getSeveralBarcodesFromName(record.getRecord1().getReadHeader());
        final String[] barcode2 = BarcodeMethods.getSeveralBarcodesFromName(record.getRecord2().getReadHeader());
        if (barcode1 == null) {
            return barcode2;
        }
        if (Arrays.equals(barcode1, barcode2)) {
            return barcode1;
        }
        throw new SAMException("Barcodes from FastqPairedRecord do not match: " +
                Arrays.toString(barcode1) + "-" + Arrays.toString(barcode2));
    }

    /**
     * Change the barcode name in a FASTQ record, adding the number of pair provided
     *
     * @param record       the record to update
     * @param newBarcode   the new barcode to add
     * @param numberOfPair the number of read for this record
     *
     * @return the updated record
     */
    private static FastqRecord changeBarcode(final FastqRecord record, final String newBarcode,
            final int numberOfPair) {
        return new FastqRecord(String
                .format("%s%s%s%s%s",
                        BarcodeMethods.getNameWithoutBarcode(record.getReadHeader()),
                        RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER, newBarcode,
                        BarcodeMethods.READ_PAIR_SEPARATOR, numberOfPair), record.getReadString(),
                record.getBaseQualityHeader(), record.getBaseQualityString());
    }

    /**
     * Change the barcode name in a pair record, adding the \1 and \2 indicating that they are
     * paired
     *
     * @param record     the record to update
     * @param newBarcode the new barcode to add
     *
     * @return the updated record
     */
    public static FastqPairedRecord changeBarcodeInPaired(final FastqPairedRecord record,
            final String newBarcode) {
        return new FastqPairedRecord(changeBarcode(record.getRecord1(), newBarcode, 1),
                changeBarcode(record.getRecord2(), newBarcode, 2));
    }
}
