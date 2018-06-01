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

import org.magicdgs.readtools.utils.read.stats.PairEndReadStatFunction;
import org.magicdgs.readtools.utils.read.stats.SingleReadStatFunction;
import org.magicdgs.readtools.utils.read.stats.engine.ProperStatWindowEngine;
import org.magicdgs.readtools.utils.read.stats.pairstat.PairIntegerTagListCounter;
import org.magicdgs.readtools.utils.read.stats.singlestat.ContainIndelCounter;
import org.magicdgs.readtools.utils.read.stats.singlestat.ContainSoftclipCounter;

import htsjdk.samtools.SAMSequenceDictionary;
import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Argument collection for {@link ComputeProperStatByWindow}.
 *
 * <p>Note: this class will be removed in the future in favor of an implementation for
 * {@link org.broadinstitute.barclay.argparser.CommandLinePluginDescriptor}, to
 * support new implementations.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: this shoudl be a plugin (https://github.com/magicDGS/ReadTools/issues/448)
class ComputeProperStatByWindowArgs {

    @Advanced
    @Argument(fullName = "do-not-print-all", doc = "If set, skip printing windows with 0 reads")
    public boolean doNotPrintAll = false;

    @ArgumentCollection
    public PairIntegerTagListCounter tagListCounter = new PairIntegerTagListCounter();
    @Argument(fullName = "ContainSoftclipCounter", doc = "Counts the number of reads containing soft-clips (cigar 'S')")
    public boolean softclip = false;
    @Argument(fullName = "ContainIndelCounter", doc = "Counts the number of reads containing indels (cigar 'I' or 'D')")
    public boolean indel = false;

    /**
     * Gets the engine for computing proper-pair statistics.
     *
     * @param output     path to output the results.
     * @param windows    windows to compute the statistics on.
     * @param dictionary sequence dictionary for the data.
     *
     * @return engine construted from the arguments.
     */
    public ProperStatWindowEngine getProperStatWindowEngine(
            final Path output,
            final List<SimpleInterval> windows,
            final SAMSequenceDictionary dictionary) {

        try {
            return new ProperStatWindowEngine(
                    dictionary,
                    windows,
                    getSingleReadStats(),
                    getPairEndReadStats(),
                    output,
                    !doNotPrintAll);
        } catch (final IOException e) {
            throw new UserException.CouldNotCreateOutputFile(output.toUri().toString(),
                    e.getMessage(), e);
        }
    }

    // constructs requested single-read statistics
    private List<SingleReadStatFunction> getSingleReadStats() {
        final List<SingleReadStatFunction> stats = new ArrayList<>(2);
        if (indel) {
            stats.add(new ContainIndelCounter());
        }
        if (softclip) {
            stats.add(new ContainSoftclipCounter());
        }
        return stats;
    }

    // constructs requested pair-end statistics
    private List<PairEndReadStatFunction> getPairEndReadStats() {
        try {
            tagListCounter.init();
        } catch (final IllegalArgumentException e) {
            throw new UserException(e.getMessage());
        }
        return Collections.singletonList(tagListCounter);
    }
}
