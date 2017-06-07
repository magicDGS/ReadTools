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
 * General tool for standardize any kind of read source (both raw and mapped reads).
 *
 * <p>This tool outputs a SAM/BAM/CRAM file as defined in the
 * <a href="http://samtools.github.io/hts-specs/SAMv1.pdf">SAM specifications</a>:</p>
 *
 * <ul>
 *
 * <li><b>Quality encoding</b>: the Standard quality is Sanger. Quality is detected automatically,
 * but it could be forced with <code>--forceEncoding</code></li>
 *
 * <li><b>Raw barcodes</b>: the standard barcode tags are BC for the sequence and QT for the
 * quality. To correctly handle information in a SAM/BAM/CRAM file with misencoded barcode tags,
 * one of the following options could be used:
 *
 * <ul>
 *
 * <li>Barcodes in the read name: use <code>--barcodeInReadName</code> option. This may be useful
 * for files produced by mapping a multiplexed library stored as FASTQ files. </li>
 *
 * <li>Barcodes in a different tag(s): use <code>--rawBarcodeSequenceTags</code>. This may be
 * useful
 * if the barcode is present in a different tag (e.g., when using <a
 * href="http://gq1.github.io/illumina2bam/">illumina2bam</a> with
 * dual indexing, the second index will be in the B2 tag)</li>
 *
 * </ul></li>
 *
 * <li><b>FASTQ file(s)</b>: the output is an unmapped SAM/BAM/CRAM file with the quality header
 * added to the CO tag. The raw barcode is extracted from the read name if present independently of
 * the read name encoding (Casava or Illumina legacy).
 * In the case of the Casava's read name encoding, the PF binary tag is also updated.</li>
 *
 * </ul>
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @ReadTools.note FASTQ files does not require the <code>--barcodeInReadName</code> option.
 * @ReadTools.warning If several barcode indexes are present, barcodes are separated by hyphens and
 * qualities by space as defined in the <a href="http://samtools.github.io/hts-specs/SAMv1.pdf">SAM
 * specifications</a>.
 */
@CommandLineProgramProperties(oneLineSummary = "Standardize quality and format for all kind of sources for ReadTools.",
        summary = StandardizeReads.SUMMARY,
        programGroup = RTConversionProgramGroup.class)
@DocumentedFeature
public final class StandardizeReads extends ReadToolsWalker {

    protected static final String SUMMARY = "Standardizes the format of reads from both "
            + "raw and mapped reads and outputs a SAM/BAM/CRAM file with:\n"
            + "\t- Standard quality encoding (Sanger)\n"
            + "\t- Raw barcode sequence/quality in the correct tags (BC/QT)\n\n\n"
            + "correct tags (BC/QT).\n\n"
            + "Find more information about this tool in "
            + RTHelpConstants.DOCUMENTATION_PAGE + "StandardizeReads.html";

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
