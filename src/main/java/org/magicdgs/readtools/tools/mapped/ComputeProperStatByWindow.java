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

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.programgroups.MappedProgramGroup;
import org.magicdgs.readtools.engine.RTReadWalker;
import org.magicdgs.readtools.exceptions.RTUserExceptions;
import org.magicdgs.readtools.utils.read.stats.engine.ProperStatWindowEngine;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.ExperimentalFeature;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.engine.FeatureContext;
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
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Computes statistics for properly-paired reads over non-overlapping windows.
 *
 * <p>Statistics are computed only for proper reads (mapped on the same contig). Nevertheless,
 * statistics might use only single-read information (e.g., <code>ContainIndelCounter</code>,
 * which counts the number of reads on the window containing indels) or from both pairs (e.g.,
 * <code>PairIntegerTagCounter</code> for NM&lt;2 would count the number of reads on the window where
 * both reads on the pair has more than 2 mismatches stored in the NM tag).
 * </p>
 *
 * <h3>Caveats</h3>
 *
 * <ul>
 *     <li>Pair-end data is required even for computing only single read statistics.</li>
 *     <li>Coordinate-sorted SAM/BAM/CRAM is required.</li>
 *     <li>Intervals are not allowed in this tool. The statistics are computed over the genome.</li>
 *     <li>
 *         It is recommended that the file includes all the pair-end data (not only a subset of the reads).
 *         Otherwise, missing pairs would not be used for the statistics.
 *     </li>
 * </ul>
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @ReadTools.note In this tool, proper pairs are defined as mapping on the same contig, without
 * taking into consideration the SAM flag (0x2).
 * @ReadTools.warning Please, note that disabling default read filters on this tool will produce
 * wrong results.
 */
@DocumentedFeature
@ExperimentalFeature
@CommandLineProgramProperties(oneLineSummary = "Computes proper-paired reads statistics over windows ",
        summary = ComputeProperStatByWindow.SUMMARY,
        programGroup = MappedProgramGroup.class)
public final class ComputeProperStatByWindow extends RTReadWalker {

    protected static final String SUMMARY = "Computes statistics for proper reads (mapped on the "
            + "same contig) over non-overlapping windows."
            + "Statistics are computed for single-reads (e.g., number of reads on the window "
            + "containing indels) or taking in consideration the read pair (e.g., number of reads "
            + "on the window where NM>2 for both pairs).\n\n"
            + "WARNING: disabling default read filters on tis tool will produce wrong results."
            + "\n\n"
            + "Find more information about this tool in "
            + RTHelpConstants.DOCUMENTATION_PAGE + "ComputeProperStatByWindow.html";

    /**
     * Tab-delimited output file with the statistic over the windows. A header defines the order of
     * each statistic and the first column the window in the form contig:start-end.
     */
    @Argument(fullName = RTStandardArguments.OUTPUT_LONG_NAME, shortName = RTStandardArguments.OUTPUT_SHORT_NAME, doc = "Tab-delimited output file with the statistic over the windows")
    public String outputArg;

    @Argument(fullName = RTStandardArguments.FORCE_OVERWRITE_NAME, shortName = RTStandardArguments.FORCE_OVERWRITE_NAME, doc = RTStandardArguments.FORCE_OVERWRITE_DOC, optional = true, common = true)
    public Boolean forceOverwrite = false;

    // name for the contig argument
    private static final String WINDOW_CONTIG_NAME = "contig";
    /**
     * Limit the computation to the provided contig(s). This argument is used instead of
     * interval arguments and might be removed in the future if intervals are supported.
     */
    @Argument(fullName = WINDOW_CONTIG_NAME, doc = "Limit the computation to the provided contig(s)", optional = true)
    public List<String> contig = new ArrayList<>();

    // TODO: support sliding-window (https://github.com/magicDGS/ReadTools/issues/466)
    @Argument(fullName = "window-size", doc = "Window size to perform the analysis", minValue = 1)
    public Integer window;

    // TODO: this should be a plugin (https://github.com/magicDGS/ReadTools/issues/448)
    @ArgumentCollection
    public ComputeProperStatByWindowArgs engineArgs = new ComputeProperStatByWindowArgs();

    @Override
    public List<ReadFilter> getDefaultReadFilters() {
        // using the mapped filter speeds-up processing
        return Arrays.asList(ReadFilterLibrary.MAPPED, ReadFilterLibrary.PRIMARY_LINE);
    }

    // engine for computing the stats
    private ProperStatWindowEngine engine;

    @Override
    public void onTraversalStart() {
        // first check if the path exits or not
        final Path path = IOUtils.getPath(outputArg);
        if (!forceOverwrite && Files.exists(path)) {
            throw new RTUserExceptions.OutputFileExists(outputArg);
        }

        // TODO: get rid of this limitation (https://github.com/magicDGS/ReadTools/issues/466)
        if (getHeaderForReads().getSortOrder() != SAMFileHeader.SortOrder.coordinate) {
            throw new UserException(String.format(
                    "%s only supports coordinate-sorted inputs (found %s)",
                    getToolName(), getHeaderForReads().getSortOrder()));
        }

        // TODO: get rid of this limitation (https://github.com/magicDGS/ReadTools/issues/466)
        if (hasIntervals()) {
            throw new UserException(String.format(
                    "%s does not support intervals (use --%s to limit contigs in the output). This limitation might be removed in the future.",
                    getToolName(), WINDOW_CONTIG_NAME));
        }

        // validate that there is a sequence dictionary
        final SAMSequenceDictionary dictionary = getBestAvailableSequenceDictionary();
        engine = engineArgs
                .getProperStatWindowEngine(path, makeWindows(dictionary, contig, window), dictionary);
    }

    private static List<SimpleInterval> makeWindows(final SAMSequenceDictionary dictionary,
            final List<String> contigs,
            final int window) {
        final Predicate<SimpleInterval> contigFilter = (contigs.isEmpty()) ?
                s -> true : s -> contigs.contains(s.getContig());
        return IntervalUtils.getAllIntervalsForReference(dictionary).stream()
                .filter(contigFilter)
                .flatMap(i -> Shard.divideIntervalIntoShards(i, window, 0, dictionary).stream())
                .map(ShardBoundary::getInterval)
                .collect(Collectors.toList());
    }

    @Override
    public void apply(final GATKRead read, final ReferenceContext referenceContext,
            final FeatureContext featureContext) {
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
