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
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import scala.Tuple2;

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
                + "\t- Raw barcodes: the BC/QT tags will be updated if requested by the barcode options. "
                + "This options may be useful if the information for the raw barcodes is present in "
                + "a different tag (e.g., while using illumina2bam with double indexing) or it was "
                + "not de-multiplexed before mapping using FASTQ file (e.g., barcodes should be "
                + "encoded in the read name if mapping with DistMap on a cluster). "
                + "Note: If several indexes are present, barcodes are separated by hyphens and qualities by space.\n"
                + "\t- FASTQ file(s): the output is a unmapped SAM/BAM/CRAM file with the quality "
                + "header in the CO tag and the PF binary tag if the read name is in the Casava "
                + "format. The raw barcode (BC) is extracted from the read name if present "
                + "(does not require any barcode option).\n"
                + "\nFind more information in " + RTHelpConstants.DOCUMENTATION_PAGE,
        programGroup = RTConversionProgramGroup.class)
public final class StandardizeReads extends ReadToolsWalker {

    @ArgumentCollection
    public RTOutputArgumentCollection outputBamArgumentCollection =
            RTOutputArgumentCollection.defaultOutput();

    @ArgumentCollection
    public FixBarcodeAbstractArgumentCollection fixBarcodeArguments =
            FixBarcodeAbstractArgumentCollection.getArgumentCollection(true);

    // the writer for the reads
    private GATKReadWriter writer;

    @Override
    public String[] customCommandLineValidation() {
        fixBarcodeArguments.validateArguments();
        return super.customCommandLineValidation();
    }

    @Override
    public void onTraversalStart() {
        final SAMFileHeader headerFromReads = getHeaderForReads();
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
