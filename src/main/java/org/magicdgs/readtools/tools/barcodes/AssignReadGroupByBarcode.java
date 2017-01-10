/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.argumentcollections.BarcodeDetectorArgumentCollection;
import org.magicdgs.readtools.cmd.argumentcollections.RTOutputArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.ReadToolsProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsWalker;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.stats.MatcherStat;
import org.magicdgs.readtools.utils.misc.Formats;
import org.magicdgs.readtools.utils.read.RTReadUtils;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixRawBarcodeTagsReadTransformer;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixReadNameBarcodesReadTransformer;
import org.magicdgs.readtools.utils.read.writer.NullGATKWriter;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import scala.Tuple2;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tool for assign barcodes to any kind of input and output a standard ReadTools read.
 * See the summary for more information.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Assigns read groups based on barcode tag(s) for all kind of sources for ReadTools.",
        summary = "Assigns the read groups present in the file(s) based on the barcode present in "
                + "the raw barcode tag(s). Read groups are assigned by matching the ones provided "
                + "in the barcode file against the present in the tag(s), allowing mismatches and "
                + "unknown bases (Ns) in the sequence. Ambiguous barcodes, defined as the ones that "
                + "have a concrete distance with the second match (at least one mismatch of difference), "
                + "are also discarded. If several indexed are used and none of then identify uniquely "
                + "the read group, the read group is assigned by majority vote.\n"
                + "Note: for pair-end reads, only one read is used to assing the barcode.\n"
                + "\nWARNING: If several barcodes are present and one of then identify uniquely the "
                + "read group, this is assigned directly. Thus, it is recommended to provide all the "
                + "barcodes present in the library to the parameter.",
        programGroup = ReadToolsProgramGroup.class)
public final class AssignReadGroupByBarcode extends ReadToolsWalker {

    @ArgumentCollection
    public RTOutputArgumentCollection outputBamArgumentCollection =
            RTOutputArgumentCollection.splitOutput();

    @Argument(fullName = RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME, shortName = RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME, doc = "Include the barcodes encoded in this tag(s) in the read name. Note: this is not necessary for input FASTQ files.", optional = true,
            mutex = {RTStandardArguments.USER_READ_NAME_BARCODE_NAME})
    public List<String> rawBarcodeTags = new ArrayList<>(Collections.singleton(SAMTag.BC.name()));

    @Argument(fullName = RTStandardArguments.USER_READ_NAME_BARCODE_NAME, shortName = RTStandardArguments.USER_READ_NAME_BARCODE_NAME, doc = "Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files.", optional = true,
            mutex = {RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME})
    public boolean useReadNameBarcode = false;

    @ArgumentCollection
    public BarcodeDetectorArgumentCollection barcodeDetectorArgumentCollection =
            new BarcodeDetectorArgumentCollection();

    @Argument(fullName = RTStandardArguments.KEEP_DISCARDED_NAME, shortName = RTStandardArguments.KEEP_DISCARDED_NAME, doc = "Keep reads does not assigned to any record in a separate file.")
    public boolean keepDiscarded = false;

    // the writer for the reads
    private GATKReadWriter writer;

    private GATKReadWriter discardedWriter;

    // the transformer for fixing the reads
    private ReadTransformer transformer;

    private BarcodeDecoder decoder;

    // validate the barcode detector arguments
    protected String[] customCommandLineValidation() {
        barcodeDetectorArgumentCollection.validateArguments();
        return super.customCommandLineValidation();
    }

    /**
     * Initializes:
     * - The transformer for fix the barcodes in the name.
     * - Decoder to use for assign barcodes.
     * - Writers for output (including discarded).
     */
    @Override
    public void onTraversalStart() {
        if (useReadNameBarcode) {
            logger.debug("Using barcodes from read names");
            transformer = new FixReadNameBarcodesReadTransformer();
        } else if (rawBarcodeTags.isEmpty()
                || (rawBarcodeTags.size() == 1 && rawBarcodeTags.get(0).equals(SAMTag.BC.name()))) {
            logger.debug("Not using barcode tags: {}", rawBarcodeTags);
            transformer = ReadTransformer.identity();
        } else {
            logger.debug("Using barcode tags: {}", rawBarcodeTags);
            transformer = new FixRawBarcodeTagsReadTransformer(rawBarcodeTags);
        }
        // setting up the barcode decoder engine
        decoder = barcodeDetectorArgumentCollection.getBarcodeDecoder();

        // update with the read groups in the decoder and the unknown
        final SAMFileHeader headerForWriter = getHeaderForReads().clone();
        if (!headerForWriter.getReadGroups().isEmpty()) {
            logger.warn("Read group in the input file(s) will be removed in the output.");
        }
        headerForWriter.setReadGroups(decoder.getDictionary().getSampleReadGroups());

        // output the writer
        writer = outputBamArgumentCollection.outputWriter(headerForWriter,
                () -> getProgramRecord(headerForWriter), true, getReferenceFile()
        );

        // discarded writer
        discardedWriter = (keepDiscarded)
                ? discardedWriter = outputBamArgumentCollection.getWriterFactory()
                .setReferenceFile(getReferenceFile())
                .createWriter(outputBamArgumentCollection
                                .getOutputNameWithSuffix(RTDefaults.DISCARDED_OUTPUT_SUFFIX),
                        getHeaderForReads(), true)
                : new NullGATKWriter();
    }

    /**
     * Applies the transformer for fix the barcode, assigns the read group by barcode and writes
     * the read to the output using {@link #writeRead(GATKRead)}.
     */
    @Override
    protected void apply(final GATKRead read) {
        logger.debug("Read = {}", () -> read);
        // assumes that the transformed read is modified in place
        transformer.apply(read);
        decoder.assignReadGroupByBarcode(read);
        writeRead(read);
    }

    /**
     * Applies the transformer for fix the barcode, assigns the read group by barcode and writes
     * the read to the output using {@link #writeRead(GATKRead)}. In addition, the reads
     * {@link SAMTag#BC} tag are coupled in case they are not present.
     *
     * Note: the second read read group is identified using the information from the first read.
     */
    @Override
    protected void apply(final Tuple2<GATKRead, GATKRead> pair) {
        // only assing the one in the first read
        logger.debug("First: {}", () -> pair._1);
        logger.debug("Second: {}", () -> pair._2);
        // this only works if it is modified in place
        final GATKRead read1 = transformer.apply(pair._1);
        final GATKRead read2 = transformer.apply(pair._2);
        // now we have to fix the barcode tags
        RTReadUtils.fixPairTag(SAMTag.BC.name(), read1, read2);
        decoder.assignReadGroupByBarcode(read1);
        // now use the read1 information for read2
        // assuming that the barcodes are the same for both reads
        read2.setReadGroup(read1.getReadGroup());
        // and write the reads
        writeRead(read1);
        writeRead(read2);
    }

    /**
     * If the read is the unknown barcode, removes the read group and output to the discarded
     * writer; otherwise, writes the read to the default output.
     */
    private void writeRead(final GATKRead read) {
        final String rg = read.getReadGroup();
        if (rg == null || rg.equals(BarcodeMatch.UNKNOWN_STRING)) {
            read.setReadGroup(null);
            discardedWriter.addRead(read);
        } else {
            writer.addRead(read);
        }
    }

    /**
     * Prints the statistics for each barcode into a metrics file and logs the number of records
     * per barcode.
     */
    @Override
    public Object onTraversalSuccess() {
        final Path metricsFile = outputBamArgumentCollection.makeMetricsFile(null);
        try {
            final Collection<MatcherStat> stats = decoder.outputStats(metricsFile);
            stats.forEach(s -> logger.info("Found {} records for {} ({}).",
                    Formats.commaFmt.format(s.RECORDS), s.SAMPLE, s.BARCODE));
            return null;
        } catch (final IOException e) {
            // TODO: use the Path exception after https://github.com/broadinstitute/gatk/pull/2282
            throw new UserException.CouldNotCreateOutputFile(metricsFile.toString(), e.getMessage(),
                    e);
        }
    }

    @Override
    public void closeTool() {
        CloserUtil.close(writer);
        CloserUtil.close(discardedWriter);
    }
}
