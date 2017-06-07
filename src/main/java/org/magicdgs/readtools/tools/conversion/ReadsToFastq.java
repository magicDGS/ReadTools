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

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.cmd.argumentcollections.FixBarcodeAbstractArgumentCollection;
import org.magicdgs.readtools.cmd.argumentcollections.RTOutputArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.RTConversionProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsWalker;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import scala.Tuple2;

/**
 * Converts to the FASTQ format any kind of ReadTools source (SAM/BAM/CRAM/FASTQ), including
 * information from the barcodes (BC tag) in the read name (Illumina format) to allow keeping
 * sample data.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Converts any kind of ReadTools source to FASTQ format.",
        summary = ReadsToFastq.SUMMARY,
        programGroup = RTConversionProgramGroup.class)
@DocumentedFeature(extraDocs = StandardizeReads.class)
public final class ReadsToFastq extends ReadToolsWalker {

    protected static final String SUMMARY = "Converts SAM/BAM/CRAM/FASTQ formats into FASTQ, "
            + "including information from the barcodes (BC tag) in the read name (Illumina format) "
            + "to allow keeping sample data.\n\n"
            + "Find more information about this tool in "
            + RTHelpConstants.DOCUMENTATION_PAGE + "ReadsToFastq.html";

    @ArgumentCollection
    public RTOutputArgumentCollection outputBamArgumentCollection =
            RTOutputArgumentCollection.fastqOutput();

    @ArgumentCollection
    public FixBarcodeAbstractArgumentCollection fixBarcodeArguments =
            FixBarcodeAbstractArgumentCollection.getArgumentCollection(false);

    // the writer for the reads
    private GATKReadWriter writer;

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
    }

    @Override
    protected void apply(final GATKRead read) {
        writer.addRead(fixBarcodeArguments.fixBarcodeTags(read));
    }

    @Override
    protected void apply(final Tuple2<GATKRead, GATKRead> pair) {
        logger.debug("First: {}", pair._1);
        logger.debug("Second: {}", pair._2);
        // this only works if it is modified in place
        fixBarcodeArguments.fixBarcodeTags(pair);
        writer.addRead(pair._1);
        writer.addRead(pair._2);
    }

    @Override
    public void closeTool() {
        ReadWriterFactory.closeWriter(writer);
    }
}
