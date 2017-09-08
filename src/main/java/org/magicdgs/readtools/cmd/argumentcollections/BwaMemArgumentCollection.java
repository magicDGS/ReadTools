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

package org.magicdgs.readtools.cmd.argumentcollections;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAligner;
import org.broadinstitute.hellbender.utils.bwa.BwaMemIndex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Argument collection for including options for the {@link org.broadinstitute.hellbender.utils.bwa.BwaMemAligner}.
 *
 * <p>Note: all arguments are advanced and optional.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: maybe we should have an argument collection for each kind of option
// TODO: for example, this is just for algorithm options
public class BwaMemArgumentCollection implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Logger logger = LogManager.getLogger(this.getClass());

    /////////////////////////////////////////////////////////
    // ADVANCE OPTIONS FOR BWA-MEM - if null, they use defaults
    // TODO: include more?

    @Advanced
    @Argument(fullName = "matchScore", doc = "score for a sequence match, which scales options -TdBOELU unless overridden (- A option in bwa-mem)", optional = true)
    public Integer matchScore = null;

    @Advanced
    @Argument(fullName = "mismatchPenalty", doc = "penalty for a mismatch (-B option in bwa-mem)", optional = true)
    public Integer mismatchPenalty = null;

    @Advanced
    @Argument(fullName = "gapOpenPenalty", doc = "gap open penalties for deletions and insertions (-O option in bwa-mem)", optional = true, maxElements = 2)
    public List<Integer> gapOpenPenalties = new ArrayList<>();

    @Advanced
    @Argument(fullName = "gapExtensionPenalty", doc = "gap extension penalty (-E option in bwa-mem); a gap of size k cost '{-O} + {-E}*k'", optional = true, maxElements = 2)
    public List<Integer> gapExtensionPenalties = new ArrayList<>();

    @Advanced
    @Argument(fullName = "clippingPenalty", doc = "penalty for 5'- and 3'-end clipping (-L option in bwa-mem)", optional = true, maxElements = 2)
    public List<Integer> clippingPenalties = new ArrayList<>();

    /**
     * Construct with default values provided by the library.
     */
    public BwaMemArgumentCollection() {
        // no-arg constructor
    }

    // TODO: maybe we should create a builder for this..
    public BwaMemArgumentCollection(final Integer matchScore, final Integer mismatchPenalty,
            final List<Integer> gapOpenPenalties, final List<Integer> gapExtensionPenalties,
            final List<Integer> clippingPenalties) {
        this.matchScore = matchScore;
        this.mismatchPenalty = mismatchPenalty;

        // lists are just filled, to allow constant lists to be passed
        this.gapOpenPenalties.addAll(gapOpenPenalties);
        this.gapExtensionPenalties.addAll(gapExtensionPenalties);
        this.clippingPenalties.addAll(clippingPenalties);
    }

    // TODO: maybe we should have a method only to apply the options
    public BwaMemAligner getNewBwaMemAligner(final String bwaMemIndexImage) {
        // initialize the aligner with the index file
        final BwaMemAligner aligner = new BwaMemAligner(new BwaMemIndex(bwaMemIndexImage));

        // and setting options
        setAlignerOption(aligner, matchScore, BwaMemAligner::setMatchScoreOption, "match score");
        setAlignerOption(aligner, mismatchPenalty, BwaMemAligner::setMismatchPenaltyOption,
                "mismatch penalty");
        setMaybeTwoOptions(aligner, gapOpenPenalties, BwaMemAligner::setDGapOpenPenaltyOption,
                BwaMemAligner::setIGapOpenPenaltyOption, "gap open penalty", "deletion",
                "insertion");
        setMaybeTwoOptions(aligner, gapExtensionPenalties, BwaMemAligner::setDGapExtendPenaltyOption,
                BwaMemAligner::setIGapExtendPenaltyOption, "gap extension penalty", "deletion",
                "insertion");
        setMaybeTwoOptions(aligner, clippingPenalties, BwaMemAligner::setClip5PenaltyOption,
                BwaMemAligner::setClip3PenaltyOption, "clipping penalty", "5'-end", "3'-end");

        return aligner;
    }


    private <T> void setAlignerOption(
            final BwaMemAligner aligner,
            final T optionValue,
            final BiConsumer<BwaMemAligner, T> setter,
            final String optionName) {
        if (optionValue != null) {
            logger.debug("Setting advance option: {}", optionName);
            setter.accept(aligner, optionValue);
        }
    }

    private <T> void setMaybeTwoOptions(
            final BwaMemAligner aligner,
            final List<T> optionValues,
            final BiConsumer<BwaMemAligner, T> firstSetter,
            final BiConsumer<BwaMemAligner, T> secondSetter,
            final String optionName,
            final String firstValueName,
            final String secondValueName) {
        if (optionValues != null && !optionValues.isEmpty()) {

            if (optionValues.size() == 1) {
                logger.debug("Setting advance option: {} and {} {} (same value)", firstValueName,
                        secondValueName, optionName);
                firstSetter.accept(aligner, optionValues.get(0));
                secondSetter.accept(aligner, optionValues.get(0));
            } else if (optionValues.size() == 2) {
                setAlignerOption(aligner, optionValues.get(0), firstSetter, optionName + " " + firstValueName);
                setAlignerOption(aligner, optionValues.get(1), firstSetter, optionName + " " + secondValueName);
            } else {
                throw new GATKException.ShouldNeverReachHereException("Argument parser failed to set maxElements");
            }
        }
    }

}
