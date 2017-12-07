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
import org.magicdgs.readtools.cmd.plugin.TrimmerPluginDescriptor;
import org.magicdgs.readtools.cmd.programgroups.DistmapProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsWalker;
import org.magicdgs.readtools.tools.conversion.StandardizeReads;
import org.magicdgs.readtools.utils.distmap.DistmapException;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;
import org.magicdgs.readtools.utils.trimming.TrimAndFilterPipeline;

import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineParser;
import org.broadinstitute.barclay.argparser.CommandLinePluginDescriptor;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.cmdline.GATKPlugin.GATKReadFilterPluginDescriptor;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Converts to the Distmap format
 * (<a href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0072614">
 * Pandey &amp; Schlötterer 2013</a>) any kind of ReadTools source, including information
 * from the barcodes (BC tag) in the read name (Illumina format) to allow keeping sample data.
 *
 * <p>If requested, it also applies a trimming/filtering pipeline to the reads.
 * </p>
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Converts any kind of ReadTools source to Distmap format.",
        summary = ReadsToDistmap.SUMMARY,
        programGroup = DistmapProgramGroup.class)
@DocumentedFeature(extraDocs = StandardizeReads.class)
public final class ReadsToDistmap extends ReadToolsWalker {

    protected static final String SUMMARY = "Converts SAM/BAM/CRAM/FASTQ formats into Distmap "
            + "format (Pandey & Schlötterer, PLoS ONE 8, 2013, e72614), including information from "
            + "the barcodes (BC tag) in the read name (Illumina format) to allow keeping sample "
            + "data.\n\n"
            + "Find more information about this tool in "
            + RTHelpConstants.DOCUMENTATION_PAGE + "ReadsToDistmap.html";

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

    // pipeline to trim and filter - if null, no trimming will be performed
    private TrimAndFilterPipeline pipeline;

    // track number of discarded single/pair-ends
    private long discarded = 0;

    @Override
    public List<? extends CommandLinePluginDescriptor<?>> getPluginDescriptors() {
        return Arrays.asList(
                // no default trimmers
                new TrimmerPluginDescriptor(new DistmapTrimmerPluginArgumentCollection(), Collections.emptyList()),
                new GATKReadFilterPluginDescriptor(
                        new DistmapFilterPluginArgumentCollection(),
                        // no default filters
                        Collections.emptyList())
        );
    }

    /** Validates the barcode-fixing arguments, and initializes the writer. */
    @Override
    public void onTraversalStart() {
        fixBarcodeTags.validateArguments();
        // TODO - activate trimming
        try {
            final CommandLineParser parser = getCommandLineParser();
            pipeline = TrimAndFilterPipeline.fromPluginDescriptors(
                    parser.getPluginDescriptor(TrimmerPluginDescriptor.class),
                    parser.getPluginDescriptor(GATKReadFilterPluginDescriptor.class)
            );
        } catch (CommandLineException.BadArgumentValue e) {
            pipeline = null;
        }

        writer = new ReadWriterFactory()
                .setForceOverwrite(forceOverwrite)
                .setHdfsBlockSize(blockSize)
                .createDistmapWriter(output, isPaired());
    }

    /** Fixes the barcode tag(s) and write the read down. */
    @Override
    protected void apply(final GATKRead read) {
        if (testRead(read)) {
            writer.addRead(fixBarcodeTags.fixBarcodeTags(read));
        } else {
            discarded++;
        }
    }

    @Override
    protected void apply(final Tuple2<GATKRead, GATKRead> pair) {
        final boolean firstPass = testRead(pair._1);
        final boolean secondPass = testRead(pair._2);

        // only if they pass, fix the barcode tags and write
        if (firstPass && secondPass) {
            fixBarcodeTags.fixBarcodeTags(pair);
            writer.addRead(pair._1);
            writer.addRead(pair._2);
        } else {
            discarded++;
        }
    }

    @Override
    public Object onTraversalSuccess() {
        if (pipeline != null) {
            final String pairedString = isPaired() ? "read pairs" : "reads";
            logger.info("After trimming, {} {} were discarded.", discarded, pairedString);
            pipeline.getTrimmingStats().forEach(stat -> {
                logger.info("{}: {}/{} 5'-trimmed {}",
                        stat.TRIMMER, stat.TRIMMED_5_P, stat.TOTAL, pairedString);
                logger.info("{}: {}/{} 3'-trimmed {}",
                        stat.TRIMMER, stat.TRIMMED_3_P, stat.TOTAL, pairedString);
                logger.info("{}: {}/{} completely trimmed {}",
                        stat.TRIMMER, stat.TRIMMED_COMPLETE, stat.TOTAL, pairedString);
            });
            pipeline.getFilterStats().forEach(stat ->
                    logger.info("{}: {}/{} passing {} after filtering",
                            stat.FILTER, stat.PASSED, stat.TOTAL, pairedString)
            );
        }
        return null;
    }

    // test that the read is trimmed or not
    private boolean testRead(final GATKRead read) {
        if (pipeline != null) {
            return pipeline.test(read);
        }
        return true;
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
