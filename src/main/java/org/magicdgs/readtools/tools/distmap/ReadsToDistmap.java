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

package org.magicdgs.readtools.tools.distmap;

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.argumentcollections.FixBarcodeAbstractArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.DistmapProgramGroup;
import org.magicdgs.readtools.cmd.programgroups.RTConversionProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsWalker;
import org.magicdgs.readtools.utils.distmap.DistmapException;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;

import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

/**
 * Converts to the Distmap format any kind of ReadTools source. See the summary for more
 * information.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Converts any kind of ReadTools source to Distmap format.",
        summary = "This tool converts SAM/BAM/CRAM/FASTQ formats into Distmap format from "
                + "Pandey & Schlötterer (PLoS ONE 8, 2013, e72614), including information from "
                + "the barcodes (BC tag) in the read name (Illumina format) to allow keeping to "
                + "some extend sample information if necessary.\n"
                + "If the source is a SAM/BAM/CRAM file and the barcodes are encoded in the read "
                + "name, the option --" + RTStandardArguments.USER_READ_NAME_BARCODE_NAME
                + " should be used. If the barcode information is encoded in a different tag(s) "
                + "the option --" + RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME
                + " should be used.\n"
                + "\nNote: see " + RTHelpConstants.DOCUMENTATION_PAGE
                +" for more information about how standard barcode information is handled in "
                + "ReadTools and when it is useful.",
        programGroup = DistmapProgramGroup.class)
@DocumentedFeature
public final class ReadsToDistmap extends ReadToolsWalker {

    @Argument(fullName = RTStandardArguments.FORCE_OVERWRITE_NAME, shortName = RTStandardArguments.FORCE_OVERWRITE_NAME, doc = "Force output overwriting if it exists", common = true, optional = true)
    public Boolean forceOverwrite = false;

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME, shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, doc = "Output in Distmap format. Expected to be in an HDFS file system.", optional = false)
    public String output;

    @Advanced
    @Argument(fullName = RTStandardArguments.HDFS_BLOCK_SIZE_NAME, shortName = RTStandardArguments.HDFS_BLOCK_SIZE_NAME, doc = "Block-size (in bytes) for files in HDFS. If not provided, use default configuration.", optional = true)
    public Integer blockSize = null;

    @ArgumentCollection
    public FixBarcodeAbstractArgumentCollection fixBarcodeTags =
            FixBarcodeAbstractArgumentCollection.getArgumentCollection(false);

    // the writer for the reads
    private GATKReadWriter writer;

    /** Validates the barcode-fixing arguments, and initializes the writer. */
    @Override
    public void onTraversalStart() {
        fixBarcodeTags.validateArguments();
        writer = new ReadWriterFactory()
                .setForceOverwrite(forceOverwrite)
                .setHdfsBlockSize(blockSize)
                .createDistmapWriter(output, isPaired());
    }

    /** Fixes the barcode tag(s) and write the read down. */
    @Override
    protected void apply(final GATKRead read) {
        // we do not require an special handling of pair-end reads because the writer
        // is doing it for us
        // in addition, we do not have to assign the barcodes for the second pair, because
        // they should have the same and we assume that the first one includes it
        // see https://github.com/magicDGS/ReadTools/issues/159 for more detail
        writer.addRead(fixBarcodeTags.fixBarcodeTags(read));
    }

    /** Close the writer. */
    @Override
    public void closeTool() {
        try {
            ReadWriterFactory.closeWriter(writer);
        } catch (final DistmapException e) {
            // this exception is expected if there is a pair-end writer
            // and the second read has not being added to the writer
            throw new UserException.MalformedFile(e.getMessage());
        }
    }
}
