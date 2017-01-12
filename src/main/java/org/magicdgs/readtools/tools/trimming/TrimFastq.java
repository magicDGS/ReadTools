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

package org.magicdgs.readtools.tools.trimming;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.io.readers.fastq.FastqReaderInterface;
import org.magicdgs.io.readers.fastq.paired.FastqReaderPairedInterface;
import org.magicdgs.io.readers.fastq.single.FastqReaderSingleInterface;
import org.magicdgs.io.writers.fastq.ReadToolsFastqWriter;
import org.magicdgs.io.writers.fastq.SplitFastqWriter;
import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.cmd.argumentcollections.TrimmingArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.RawDataProgramGroup;
import org.magicdgs.readtools.tools.ReadToolsBaseTool;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.tools.trimming.trimmers.Trimmer;
import org.magicdgs.readtools.utils.logging.FastqLogger;
import org.magicdgs.readtools.utils.misc.IOUtils;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Class that implements the trimming algorithm from Kofler et al. 2011
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Implementation of the trimming algorithm from Kofler et al. (2011).",
        summary =  "The program removes 'N' - characters at the beginning and the end of the provided reads. If any remaining 'N' "
                + "characters are found the read is discarded. Quality removal is done using a modified Mott-algorithm: for "
                + "each base a score is calculated (score_base = quality_base - threshold). While scanning along the read "
                + "a running sum of this score is calculated; If the score drops below zero the score is set to zero; The "
                + "highest scoring region of the read is finally reported.\n\nCitation of the method: Kofler et al. (2011), "
                + "PLoS ONE 6(1), e15925, doi:10.1371/journal.pone.0015925",
        programGroup = RawDataProgramGroup.class)
public final class TrimFastq extends ReadToolsBaseTool {

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

    @ArgumentCollection(doc = "Trimming parameters")
    public TrimmingArgumentCollection trimmingArgumentCollection = new TrimmingArgumentCollection();

    private FastqReaderInterface reader;
    private ReadToolsFastqWriter writer;
    private Trimmer trimmer;

    @Override
    protected String[] customCommandLineValidation() {
        trimmingArgumentCollection.validateArguments();
        return super.customCommandLineValidation();
    }

    @Override
    protected void onStartup() {
        super.onStartup();
        reader = getFastqReaderFromInputs(input1, input2);
        final boolean single = !(reader instanceof FastqReaderPairedInterface);
        try {
            writer = (trimmingArgumentCollection.keepDiscard)
                    ? getFastqSplitWritersFromInput(outputPrefix, null, single)
                    : getSingleOrPairWriter(outputPrefix, single);
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
        trimmer = trimmingArgumentCollection.getTrimmer(single);
    }

    @Override
    protected Object doWork() {
        final FastqLogger progress;
        if (reader instanceof FastqReaderSingleInterface) {
            logger.debug("Running single end");
            progress = new FastqLogger(logger, 1000000, "Processed", "read-pairs");
            processSE(progress);
        } else if (reader instanceof FastqReaderPairedInterface) {
            logger.debug("Running paired end");
            progress = new FastqLogger(logger);
            processPE(progress);
        } else {
            logger.debug(
                    "ERROR: FastqReaderInterface is not an instance of Single or Paired interfaces");
            throw new IllegalArgumentException("Unreachable code");
        }
        // final line of progress
        progress.logNumberOfVariantsProcessed();
        // print the metrics file
        final Path metricsFile = IOUtils.makeMetricsFile(outputPrefix);
        try {
            trimmer.printTrimmerMetrics(metricsFile);
        } catch (IOException e) {
            throw new UserException.CouldNotCreateOutputFile(metricsFile.toString(),
                    e.getMessage(), e);
        }
        return null;
    }

    /**
     * Process the files in pair-end mode
     */
    private void processPE(final FastqLogger progress) {
        final FastqReaderPairedInterface reader = (FastqReaderPairedInterface) this.reader;
        while (reader.hasNext()) {
            final FastqPairedRecord record = reader.next();
            final FastqPairedRecord newRecord =
                    trimmer.trimFastqPairedRecord(record, reader.getFastqQuality());
            if (newRecord.isComplete()) {
                writer.write(newRecord);
            } else if (newRecord.containRecords()) {
                if (newRecord.getRecord1() == null) {
                    writer.write(newRecord.getRecord2());
                    maybeWriteUnknown(record.getRecord1());
                } else {
                    writer.write(newRecord.getRecord1());
                    maybeWriteUnknown(record.getRecord2());
                }
            } else {
                maybeWriteUnknown(record);
            }
            progress.add();
        }
    }

    /**
     * Process the files in single-end mode
     */
    private void processSE(final FastqLogger progress) {
        final FastqReaderSingleInterface reader = (FastqReaderSingleInterface) this.reader;
        while (reader.hasNext()) {
            final FastqRecord record = reader.next();
            final FastqRecord newRecord = trimmer.trimFastqRecord(record, reader.getFastqQuality());
            if (newRecord != null) {
                writer.write(newRecord);
            } else {
                maybeWriteUnknown(record);
            }
            progress.add();
        }
    }

    /**
     * Writes the record in the unknown barcode if keep them.
     */
    private void maybeWriteUnknown(final FastqRecord unassignedRecord) {
        if (trimmingArgumentCollection.keepDiscard) {
            ((SplitFastqWriter) writer).write(BarcodeMatch.UNKNOWN_STRING, unassignedRecord);
        }
    }

    /**
     * Writes the record in the unknown barcode if keep them.
     */
    private void maybeWriteUnknown(final FastqPairedRecord unassignedRecord) {
        if (trimmingArgumentCollection.keepDiscard) {
            ((SplitFastqWriter) writer).write(BarcodeMatch.UNKNOWN_STRING, unassignedRecord);
        }
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(writer);
        CloserUtil.close(reader);
    }

}
