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
import org.magicdgs.readtools.cmd.argumentcollections.ReadGroupLegacyArgumentCollection;
import org.magicdgs.readtools.cmd.argumentcollections.ReadNameBarcodeArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.MappedDataProgramGroup;
import org.magicdgs.readtools.tools.ReadToolsBaseTool;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionaryFactory;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.readtools.utils.fastq.BarcodeMethods;
import org.magicdgs.readtools.utils.logging.ProgressLoggerExtension;
import org.magicdgs.readtools.utils.misc.IOUtils;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollection;
import org.broadinstitute.hellbender.cmdline.CommandLineProgramProperties;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * Tool for split by barcode (in the read name) for BAM files.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Identify barcodes in the read name for a BAM file and assign to the ones used on the library",
        summary = "Detect barcodes in the header of the read name (based on the marker "
                + BarcodeMethods.NAME_BARCODE_SEPARATOR
                + ") and assign to a sample based on a provided dictionary. Barcodes in the input file that are larger than the used ones are cut in the last bases.",
        programGroup = MappedDataProgramGroup.class)
public final class BamBarcodeDetector extends ReadToolsBaseTool {

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME, optional = false,
            doc = "The input BAM file with barcodes in the read name.")
    public File input = null;

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.OUTPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.OUTPUT_SHORT_NAME, optional = false,
            doc = "The output file prefix")
    public String outputPrefix = null;

    @Argument(fullName = "sam", shortName = "s", optional = true, doc = "Output should be in SAM format instead of BAM")
    public Boolean samFormat = false;

    @ArgumentCollection
    public BarcodeArgumentCollection barcodeArguments = new BarcodeArgumentCollection();

    @ArgumentCollection
    public ReadNameBarcodeArgumentCollection readNameBarcodeArguments =
            new ReadNameBarcodeArgumentCollection();

    @ArgumentCollection(doc = "Arguments for RG information for output BAM/SAM")
    public ReadGroupLegacyArgumentCollection readGroupArgumentCollection =
            new ReadGroupLegacyArgumentCollection();

    // the reader that we use for processing
    private SamReader reader;
    private SplitSAMFileWriter writer;
    private BarcodeDecoder decoder;

    @Override
    protected void onStartup() {
        super.onStartup();
        try {
            // open the decoder with its corresponding dictionary
            decoder = barcodeArguments
                    .getBarcodeDecoderFromArguments(logger, readGroupArgumentCollection);
            // open the reader
            reader = getSamReaderFromInput(input);
            // create the new header adding the read groups
            final SAMFileHeader header = reader.getFileHeader();
            addReadGroupToHeader(header, decoder.getDictionary());
            header.addProgramRecord(getToolProgramRecord());
            // create the BAM writer
            writer = getBamWriterOrSplitWriterFromInput(outputPrefix, header,
                    barcodeArguments.split ? decoder.getDictionary() : null,
                    !samFormat);
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
    }

    @Override
    protected Object doWork() {
        final ProgressLoggerExtension progress = new ProgressLoggerExtension(logger);
        final SAMRecordIterator it = reader.iterator();
        it.stream().forEach(record -> {
            final String[] barcode = readNameBarcodeArguments
                    .getBarcodesInReadName(record.getReadName());
            final String best = decoder.getBestBarcode(barcode);
            final SAMReadGroupRecord rg = decoder.getDictionary().getReadGroupFor(best);
            if (!rg.equals(BarcodeDictionaryFactory.UNKNOWN_READGROUP_INFO)) {
                record.setReadName(readNameBarcodeArguments
                        .changeBarcodeToStandard(record.getReadName(), best));
            }
            record.setAttribute("RG", rg.getId());
            writer.addAlignment(record);
            progress.record(record);
        });
        progress.logNumberOfVariantsProcessed();
        decoder.logMatcherResult(logger);
        try {
            decoder.outputStats(IOUtils.makeMetricsFile(outputPrefix));
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(writer);
        CloserUtil.close(reader);
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
    private void addReadGroupToHeader(final SAMFileHeader header,
            final BarcodeDictionary dictionary) {
        final HashSet<SAMReadGroupRecord> sampleSet =
                new HashSet<>(dictionary.getSampleReadGroups());
        for (final SAMReadGroupRecord sample : sampleSet) {
            // catch the error and output a warning if the barcode already exists
            try {
                header.addReadGroup(sample);
            } catch (IllegalArgumentException e) {
                logger.warn("Read Group {} found in original file: previous tags will be removed",
                        sample.getId());
                final SAMReadGroupRecord rg = header.getReadGroup(sample.getId());
                for (final String tag : SAMReadGroupRecord.STANDARD_TAGS) {
                    rg.setAttribute(tag, sample.getAttribute(tag));
                }
            }
        }
    }

}
