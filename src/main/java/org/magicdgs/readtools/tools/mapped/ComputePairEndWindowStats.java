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
import org.magicdgs.readtools.exceptions.RTUserExceptions;
import org.magicdgs.readtools.utils.read.stats.SingleReadStatFunction;
import org.magicdgs.readtools.utils.read.stats.WindowStatsEngine;
import org.magicdgs.readtools.utils.read.stats.singlestat.ContainIndelCounter;
import org.magicdgs.readtools.utils.read.stats.singlestat.ContainSoftclipCounter;
import org.magicdgs.readtools.utils.read.stats.pairstat.PairIntegerTagListCounter;

import htsjdk.samtools.SAMSequenceDictionary;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.programgroups.ReadDataProgramGroup;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.ReadWalker;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.Shard;
import org.broadinstitute.hellbender.engine.ShardBoundary;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "TODO",
        summary = "TODO",
        programGroup = ReadDataProgramGroup.class) // TODO: add program group for mapped files)
// TODO: this should be also ExperimentalFeature (not in the current version of barclay)
@BetaFeature
// TODO: read walkers does not have the quality converter (needed?)
public final class ComputePairEndWindowStats extends ReadWalker {

    // TODO: better description
    @Argument(fullName = RTStandardArguments.OUTPUT_LONG_NAME, shortName = RTStandardArguments.OUTPUT_SHORT_NAME, doc = "Output tab-delimited file")
    public String outputArg;

    // TODO: allow sliding-window approach?
    @Argument(fullName = "window-size", doc = "Window size to perform the analysis.")
    public Integer window;

    @Argument(fullName = "print-all", doc = "If set, print all the windows even if there are no reads")
    public boolean printAll = false;

    // TODO: this should be added to a plugin descriptor
    @ArgumentCollection
    public PairIntegerTagListCounter tagListCounter = new PairIntegerTagListCounter();
    @Argument(fullName = "ContainSoftclipCounter", doc = "Count the number of soft clipped reads in the window")
    public boolean softclip = false;
    @Argument(fullName = "ContainIndelCounter", doc = "Count the number of reads with insertion/deletions in the window")
    public boolean indel = false;
    // TODO: END plugin descriptor!

    @Override
    public List<ReadFilter> getDefaultReadFilters() {
        // using the mapped filter speeds-up processing
        return Collections.singletonList(ReadFilterLibrary.MAPPED);
    }

    @Override
    public String[] customCommandLineValidation() {
        tagListCounter.init();
        return super.customCommandLineValidation();
    }

    // engine for computing the stats
    private WindowStatsEngine engine;

    @Override
    public void onTraversalStart() {
        try {
            // TODO: requires the arguments for the real implementation
            final Path path = IOUtils.getPath(outputArg);
            if (Files.exists(path)) {
                throw new RTUserExceptions.OutputFileExists(outputArg);
            }

            // TODO: this should be constructed by the plugin by providing the windows, output string and print-all options
            // TODO: because the stats should be handled there
            final SAMSequenceDictionary dictionary = getBestAvailableSequenceDictionary();
            engine = new WindowStatsEngine(
                    dictionary,
                    makeWindows(dictionary, window),
                    getSingleStats(),
                    Collections.singletonList(tagListCounter),
                    IOUtils.getPath(outputArg),
                    printAll);
        } catch (final IOException e) {
            throw new UserException.CouldNotCreateOutputFile(outputArg, e.getMessage(), e);
        } catch (final IllegalStateException e) {
            throw new CommandLineException.BadArgumentValue(e.getMessage());
        }
    }


    // TODO: this should be handled by the plugin
    private List<SingleReadStatFunction> getSingleStats() {
        final List<SingleReadStatFunction> stats = new ArrayList<>(2);
        if (indel) {
            stats.add(new ContainIndelCounter());
        }
        if (softclip) {
            stats.add(new ContainSoftclipCounter());
        }
        return stats;
    }

    private List<SimpleInterval> makeWindows(final SAMSequenceDictionary dictionary, final int window) {
        // TODO: access the intervals for traversal instead of for all the sequence!
        // TODO: and remove this warning!
        if (hasIntervals()) {
            logger.warn("Windows are provided along the whole genome. Provided intervals limit which reads might be included in the analysis. This limitation might be removed in the future.");
        }
        return IntervalUtils.getAllIntervalsForReference(dictionary).stream()
                .flatMap(i -> Shard.divideIntervalIntoShards(i, window, 0, dictionary).stream())
                .map(ShardBoundary::getInterval)
                .collect(Collectors.toList());
    }


    @Override
    public void apply(final GATKRead read, final ReferenceContext referenceContext, final FeatureContext featureContext) {
        // let the engine do the work
        engine.addRead(read);
    }

    @Override
    public void closeTool() {
        try {
            if (engine != null) {
                engine.close();
            }
        } catch (final IOException e) {
            throw new UserException.CouldNotCreateOutputFile(outputArg, e.getMessage(), e);
        }
    }
}
