/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.tools.conversion;

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.argumentcollections.RTOutputArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.ReadToolsConversionProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsWalker;
import org.magicdgs.readtools.utils.read.RTReadUtils;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixRawBarcodeTagsReadTransformer;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixReadNameBarcodesReadTransformer;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts to the FASTQ format any kind of ReadTools source. See the summary for more information.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Converts any kind of ReadTools source to FASTQ format.",
        summary = "This tool converts SAM/BAM/CRAM/FASTQ formats into FASTQ, including information "
                + "from the barcodes (BC tag) in the read name (Illumina format) to allow keeping "
                + "to some extend sample information if necessary.\n"
                + "If the source is a SAM/BAM/CRAM file and the barcodes are encoded in the read "
                + "name, the option --" + RTStandardArguments.USER_READ_NAME_BARCODE_NAME
                + "should be used. If the barcode information is encoded in a different tag(s) "
                + "the option --" + RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME
                + " should be used.\n"
                + "Note: see the help for StandardizeReads for more information about the standard "
                + "barcode information is handled in ReadTools and when it is useful.",
        programGroup = ReadToolsConversionProgramGroup.class)
public final class ReadsToFastq extends ReadToolsWalker {

    // this tags are already handled in the FASTQ writer
    private final static List<String> RAW_BARCODES_FASTQ_TAGS =
            Collections.singletonList(SAMTag.BC.name());

    @ArgumentCollection
    public RTOutputArgumentCollection outputBamArgumentCollection =
            RTOutputArgumentCollection.fastqOutput();

    @Argument(fullName = RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME, shortName = RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME, doc = "Include the barcodes encoded in this tag(s) in the read name. Note: this is not necessary for input FASTQ files.", optional = true,
            mutex = {RTStandardArguments.USER_READ_NAME_BARCODE_NAME})
    public List<String> rawBarcodeTags = new ArrayList<>(RAW_BARCODES_FASTQ_TAGS);

    @Argument(fullName = RTStandardArguments.USER_READ_NAME_BARCODE_NAME, shortName = RTStandardArguments.USER_READ_NAME_BARCODE_NAME, doc = "Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files.", optional = true,
            mutex = {RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME})
    public boolean useReadNameBarcode = false;

    // the writer for the reads
    private GATKReadWriter writer;

    // the transformer for fixing the reads
    private ReadTransformer transformer;

    @Override
    public void onTraversalStart() {
        final SAMFileHeader headerFromReads = getHeaderForReads();
        // log some information for errors
        final SAMFileHeader.SortOrder order = headerFromReads.getSortOrder();
        if (order.equals(SAMFileHeader.SortOrder.coordinate) || order
                .equals(SAMFileHeader.SortOrder.duplicate)) {
            logger.warn(
                    "Input is sorted by {} and ReadTools does not handle FASTQ files in this order.",
                    order);
            logger.warn(
                    "FASTQ output files are expected to be sorted by queryname, and this could cause downstream problems");
            headerFromReads.setSortOrder(SAMFileHeader.SortOrder.queryname);
        }
        writer = outputBamArgumentCollection.outputWriter(headerFromReads,
                () -> getProgramRecord(headerFromReads), true, getReferenceFile()
        );
        if (useReadNameBarcode) {
            logger.debug("Using barcodes from read names");
            transformer = new FixReadNameBarcodesReadTransformer();
        } else if (rawBarcodeTags.isEmpty() || RAW_BARCODES_FASTQ_TAGS.equals(rawBarcodeTags)) {
            logger.debug("Not using barcode tags: {}", rawBarcodeTags);
            transformer = ReadTransformer.identity();
        } else {
            logger.debug("Using barcode tags: {}", rawBarcodeTags);
            transformer = new FixRawBarcodeTagsReadTransformer(rawBarcodeTags);
        }
    }

    @Override
    protected void apply(final GATKRead read) {
        writer.addRead(transformer.apply(read));
    }

    @Override
    protected void apply(final Tuple2<GATKRead, GATKRead> pair) {
        logger.debug("First: {}", pair._1);
        logger.debug("Second: {}", pair._2);
        // this only works if it is modified in place
        final GATKRead read1 = transformer.apply(pair._1);
        final GATKRead read2 = transformer.apply(pair._2);
        // now we have to fix the barcode tags
        RTReadUtils.fixPairTag(SAMTag.BC.name(), read1, read2);
        writer.addRead(read1);
        writer.addRead(read2);
    }

    @Override
    public void closeTool() {
        CloserUtil.close(writer);
    }
}
