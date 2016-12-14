/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.tools.quality;

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.argumentcollections.RTOutputArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.ReadToolsProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsWalker;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixRawBarcodeTagsReadTransformer;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixReadNameBarcodesReadTransformer;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * General tool for standardize any kind of input if the format is different from the expected by
 * ReadTools. See the summary for more information.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Standardize quality and format for all kind of sources for ReadTools.",
        summary = "This tool standardize the format of reads from both raw and mapped reads and "
                + "outputs a SAM/BAM/CRAM file:\n"
                + "\t- Quality encoding: the Standard quality is Sanger. Quality is detected "
                + "automatically, but is could be forced with --forceEncoding\n"
                + "\t- Raw barcodes: the BC/QT tags will be updated if requested by the barcode options."
                + "This options may be useful if the information for the raw barcodes is present in "
                + "a different tag (e.g., while using illumina2bam with double indexing) or it was "
                + "not de-multiplexed before mapping using FASTQ file (e.g., barcodes should be "
                + "encoded in the read name if mapping with DistMap on a cluster)."
                + "Note: If several indexes are present, they are separated by hyphens.\n"
                + "\t- FASTQ file(s): the output is a unmapped SAM/BAM/CRAM file with the quality "
                + "header in the CO tag and the PF binary tag if the read name is in the Casava "
                + "format. The raw barcode (BC) is extracted from the read name if present "
                + "(does not require any barcode option).",
        programGroup = ReadToolsProgramGroup.class)
public final class StandardizeReads extends ReadToolsWalker {

    @ArgumentCollection
    public RTOutputArgumentCollection outputBamArgumentCollection =
            RTOutputArgumentCollection.defaultOutput();

    @Argument(fullName = RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME, shortName = RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME, doc = "Use the barcode encoded in this tag(s) as raw barcodes. WARNING: this tag(s) will be removed.", optional = true,
            mutex = {RTStandardArguments.USER_READ_NAME_BARCODE_NAME})
    public List<String> rawBarcodeTags = new ArrayList<>();

    @Argument(fullName = RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME, shortName = RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME, doc =
            "Use the qualities encoded in this tag(s) as raw barcode qualities. Requires --"
                    + RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME
                    + ". WARNING: this tag(s) will be removed.", optional = true,
            mutex = {RTStandardArguments.USER_READ_NAME_BARCODE_NAME})
    public List<String> rawBarcodeQualsTags = new ArrayList<>();

    @Argument(fullName = RTStandardArguments.USER_READ_NAME_BARCODE_NAME, shortName = RTStandardArguments.USER_READ_NAME_BARCODE_NAME, doc = "Use the barcode encoded in the read name as raw barcodes. WARNING: the read name will be modified.", optional = true,
            mutex = {RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME,
                    RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME})
    public boolean useReadNameBarcode = false;

    // the writer for the reads
    private GATKReadWriter writer;

    // the transformer for fixing the reads
    private ReadTransformer transformer;

    @Override
    public String[] customCommandLineValidation() {
        if (rawBarcodeTags.isEmpty() && !rawBarcodeQualsTags.isEmpty()) {
            throw new CommandLineException.MissingArgument(
                    RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME,
                    "required if --" + RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME
                            + "is specified.");
        }
        if (!rawBarcodeQualsTags.isEmpty() && rawBarcodeTags.size() != rawBarcodeQualsTags.size()) {
            // TODO: I don't know if we should allow different number of tags
            // TODO: but this requires a change in the implementation on how to handle them
            // TODO: let's see if some user request it
            throw new CommandLineException(
                    "--" + RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME
                            + " and --" + RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME
                            + " should be provided the same number of times.");
        }
        return super.customCommandLineValidation();
    }

    @Override
    public void onTraversalStart() {
        final SAMFileHeader headerFromReads = getHeaderForReads();
        writer = outputBamArgumentCollection.outputWriter(headerFromReads,
                () -> getProgramRecord(headerFromReads), true, getReferenceFile()
        );
        if (useReadNameBarcode) {
            transformer = new FixReadNameBarcodesReadTransformer();
        } else if (!rawBarcodeTags.isEmpty()) {
            logger.debug("BC tags: {}; QT tags: {}",
                    () -> rawBarcodeTags, () -> rawBarcodeQualsTags);
            transformer = (rawBarcodeQualsTags.isEmpty())
                    ? new FixRawBarcodeTagsReadTransformer(rawBarcodeTags)
                    : new FixRawBarcodeTagsReadTransformer(rawBarcodeTags, rawBarcodeQualsTags);
        } else {
            transformer = ReadTransformer.identity();
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
        fixPairTag(SAMTag.BC.name(), read1, read2);
        fixPairTag(SAMTag.QT.name(), read1, read2);
        writer.addRead(read1);
        writer.addRead(read2);
    }

    // helper function to fix a pair-end tag for barcodes and qualities
    private void fixPairTag(final String tag, final GATKRead read1, final GATKRead read2) {
        final String tagVal1 = read1.getAttributeAsString(tag);
        final String tagVal2 = read2.getAttributeAsString(tag);
        if (tagVal1 == null && tagVal2 != null) {
            read1.setAttribute(tag, tagVal2);
        } else if (tagVal2 == null && tagVal1 != null) {
            read2.setAttribute(tag, tagVal1);
        }
        // TODO: should we also check if they are the same?
    }

    @Override
    public void closeTool() {
        CloserUtil.close(writer);
    }

}
