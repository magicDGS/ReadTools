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
import org.magicdgs.readtools.utils.fastq.RTFastqContstants;
import org.magicdgs.readtools.utils.logging.FastqLogger;
import org.magicdgs.readtools.utils.misc.Formats;
import org.magicdgs.readtools.utils.misc.IOUtils;
import org.magicdgs.readtools.utils.record.FastqRecordUtils;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.File;
import java.io.IOException;
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
            final String[] barcode = FastqRecordUtils.getBarcodesInName(record);
            final String best = decoder.getBestBarcode(barcode);
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                // TODO: get the standard
                writer.write(best, record);
            } else {
                writer.write(best, FastqRecordUtils.changeBarcodeInSingle(record, best));
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
            final String[] barcode = FastqRecordUtils.getBarcodesInName(record);
            final String best = decoder.getBestBarcode(barcode);
            if (best.equals(BarcodeMatch.UNKNOWN_STRING)) {
                // TODO: get the standard
                writer.write(best, record);
            } else {
                writer.write(best, FastqRecordUtils.changeBarcodeInPaired(record, best));
            }
            progress.add();
        }
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(writer);
        CloserUtil.close(reader);
    }
}
