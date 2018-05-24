/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.tools.mapped;

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.engine.ReadToolsProgram;

import htsjdk.samtools.util.CloserUtil;
import org.apache.commons.math3.util.Pair;
import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.Hidden;
import org.broadinstitute.hellbender.cmdline.programgroups.ReadDataProgramGroup;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.engine.ReadsDataSource;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copied as it was (very bad implementation) with small modifications to be able to integrate with
 * barclay and GATKRead.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: improve docs
@CommandLineProgramProperties(oneLineSummary = "Count the number of proper read-pairs for an integer tag and a cutoff.",
        summary = "Count the number of read-pairs that falls under/over a cutoff for any integer tag (only proper-pairs -> mate map in the same chromosome).",
        programGroup = ReadDataProgramGroup.class) // TODO: add program group for mapped files
@BetaFeature // TODO: this should be experimental, which isn't integrated into the GATK version used yet
// TODO: this should probably be a converted to a SlidingWindowReadWalker
@Deprecated
public final class TagByWindow extends ReadToolsProgram {

    // TODO: maybe a different kind of argument would be necessary
    @Argument(fullName = RTStandardArguments.INPUT_LONG_NAME, shortName = RTStandardArguments.INPUT_SHORT_NAME, doc = "BAM file to have the statistic (indexed)")
    public String input;

    @Argument(fullName = RTStandardArguments.OUTPUT_LONG_NAME, shortName = RTStandardArguments.OUTPUT_SHORT_NAME, doc = "Output tab-delimited file")
    public String outputArg;

    @Argument(fullName = "window-size", doc = "Window size to perform the analysis")
    public Integer window;

    // TODO: tagged areguments would be great here!
    @Argument(fullName = "tag-count-greater-than", shortName = "tag-gt", doc = "Threshold for count reads with TAG larger than and integer. The format is TAG:INT. E.g.: NM:2 for NM>2", optional = true)
    public List<String> largerTags;

    // TODO: tagged areguments would be great here!
    @Argument(fullName = "tag-count-lower-than", shortName = "tag-lt", doc = "Threshold for count reads with TAG lower than an integer. The format is TAG:INT. E.g., NM:10 for NM<10", optional = true)
    public List<String> lowerTags;

    @Argument(fullName = "soft-clip", doc = "Count the number of soft clipped reads in the window")
    public boolean softclip = false;

    @Argument(fullName = "count-indel", doc = "Count the number of reads with insertion/deletions in the window")
    public boolean indel = false;

    @Advanced
    @Argument(fullName = "no-last-empty", doc = "Speed-up the results and avoid last windows in a reference when no more reads are in that contig")
    public boolean EMPTY = true; // TODO: change capital and default value to false (or true, but change flag name)?

    @Advanced
    @Hidden
    @Argument(fullName = "compatible", optional = true)
    public boolean compatible = true;

    // TODO: change by a ReadsDataSource - and change name
    // public SamReader bam_reader;
    private ReadsDataSource reads;
    private TagByWindowEngine engine;

    @Override
    public String[] customCommandLineValidation() {
        // TODO: super bad implementation!
        reads = new ReadsDataSource(IOUtils.getPath(input));

        final PrintWriter OUT_TAB;
        try {
            OUT_TAB = new PrintWriter(new FileWriter(outputArg), true);
        } catch (final IOException e) {
            throw new UserException.CouldNotCreateOutputFile(outputArg, e);
        }

        List<IntTagFunction> operations = Stream.concat(
                largerTags.stream().map(TagByWindow::parseTag).map(s -> IntTagFunction.getLargerThan(s.getFirst(), s.getValue())),
                lowerTags.stream().map(TagByWindow::parseTag).map(s -> IntTagFunction.getLowerThan(s.getFirst(), s.getValue()))
        ).collect(Collectors.toList());


        engine = new TagByWindowEngine(window, reads.getHeader(), operations, EMPTY, softclip, indel, OUT_TAB, compatible);

        return super.customCommandLineValidation();
    }

    private static Pair<String, Integer> parseTag(final String tag) {
        // TODO: validate
        final String[] pair = tag.split(":");
        return Pair.create(pair[0], Integer.parseInt(pair[1]));
    }

    @Override
    protected Object doWork() {
        try {
            logger.info("Starting TagByWindow.");

            final ProgressMeter progress = new ProgressMeter();
            progress.setRecordLabel("reads");
            progress.start();

            // for each record
            // TODO: this should be converted to an engine class, and the tool to a ReadWalker
            // TODO: that will provide a way to change the engine to a better implementation without changing the main class
            // TODO: and maybe create a Spark tool at some point for multi-thread
            // TODO: in that case, a ReadWalker should have a MappedReadFilter to avoid unmapped stuff
            for(final GATKRead read: reads) {
                engine.addRead(read);
                progress.update(read);
            }

            engine.close();
            // Print the final log and exit
            progress.stop();
        } finally {
            CloserUtil.close(Arrays.asList(reads, engine));
        }
        return null;
    }

}
