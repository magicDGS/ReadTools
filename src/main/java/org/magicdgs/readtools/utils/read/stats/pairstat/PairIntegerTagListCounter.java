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

package org.magicdgs.readtools.utils.read.stats.pairstat;

import org.magicdgs.readtools.utils.math.RelationalOperator;
import org.magicdgs.readtools.utils.read.stats.PairEndReadStatFunction;

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Counts the number of read pairs whose integer SAM tag(s) is compared to a threshold(s).
 *
 * <p>If the tag(s) is not present for any of the read pairs, that pair is not included in that tag
 * count.</p>
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: add group for this and documented feature once is a plugin (https://github.com/magicDGS/ReadTools/issues/448)
public class PairIntegerTagListCounter implements PairEndReadStatFunction<List<Integer>, List<Boolean>> {

    public static final String TAG_ARG_NAME = PairIntegerTagCounter.TAG_ARG_NAME + "-list";
    public static final String OP_ARG_NAME = PairIntegerTagCounter.OP_ARG_NAME + "-list";
    public static final String VAL_ARG_NAME = PairIntegerTagCounter.VAL_ARG_NAME + "-list";

    // TODO: maybe we should change to a tagged argument (https://github.com/magicDGS/ReadTools/issues/449)
    @Argument(fullName = TAG_ARG_NAME, doc = PairIntegerTagCounter.TAG_ARG_NAME)
    public List<String> tag;

    @Argument(fullName = OP_ARG_NAME, doc = PairIntegerTagCounter.OP_ARG_DESCRIPTION)
    public List<RelationalOperator> op;

    @Argument(fullName = VAL_ARG_NAME, doc = PairIntegerTagCounter.VAL_ARG_DESCRIPTION)
    public List<Integer> threshold;

    // counters to be used
    private final List<PairIntegerTagCounter> counters;

    /**
     * Default constructor.
     *
     * @param tag integer SAM tag(s)
     * @param operator relational operator(s) for the comparison.
     * @param threshold threshold(s) for the comparison.
     */
    public PairIntegerTagListCounter(final List<String> tag, final List<RelationalOperator> operator, final List<Integer> threshold) {
        this.tag = Utils.nonNull(tag);
        this.threshold = threshold;
        this.op = operator;
        this.counters = new ArrayList<>(this.tag.size());
        init();
    }

    /**
     * Empty constructor to enable usage as a plugin and as an
     * {@link org.broadinstitute.barclay.argparser.ArgumentCollection}
     *
     * <p>Note: {@link #init()} will fail if fields are not provided.
     */
    public PairIntegerTagListCounter() {
        this.counters = new ArrayList<>();
    }

    /**
     * First perform validation (non-null and non-empty list arguments, size of the lists are
     * the same) and initialize a list of {@link PairIntegerTagCounter} with the arguments.
     *
     * <p>Note: for each tag-operation-threshold, the {@link PairIntegerTagCounter} constructor
     * might throw {@link IllegalArgumentException} if validation fails.
     */
    @Override
    public void init() {
        Utils.nonEmpty(tag, "tag(s) should be provided");
        Utils.nonEmpty(op, "operation(s) should be provided");
        Utils.nonEmpty(threshold, "threshold(s) should be provided");
        Utils.validateArg(tag.size() == op.size(), () ->  String.format(
                "tags(s) and operations(s) should have the same size but found %d vs. %d",
                tag.size(), op.size()));

        Utils.validateArg(tag.size() == threshold.size(), () -> String.format(
                    "tags(s) and operations(s) should have the same size but found %d vs. %d",
                    tag.size(), threshold.size()));

        // initialize the counters
        for (int i = 0; i < tag.size(); i++) {
            counters.add(new PairIntegerTagCounter(tag.get(i), op.get(i), threshold.get(i)));
        }
    }

    @Override
    public String getStatName() {
        // tab-delimited (following the contract)
        return counters.stream().map(PairIntegerTagCounter::getStatName)
                .collect(Collectors.joining("\t"));
    }

    @Override
    public List<Boolean> computeIntermediateFirst(final GATKRead read) {
        return counters.stream().map(c -> c.computeIntermediateFirst(read))
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> mergePairValues(final List<Boolean> first, final List<Boolean> second) {
        final List<Integer> res = new ArrayList<>(counters.size());
        for (int i = 0; i < counters.size(); i++) {
            res.add(counters.get(i).mergePairValues(first.get(i), second.get(i)));
        }
        return res;
    }

    @Override
    public List<Integer> reduce(final List<Integer> current, final List<Integer> accumulator) {
        Utils.nonNull(current);
        Utils.validateArg(current.size() == counters.size(), () -> "invalid number of current values " + current.size());
        // edge case
        if (accumulator == null) {
            return current;
        }
        Utils.validateArg(accumulator.size() == counters.size(), () -> "invalid number of accumulator values " + accumulator.size());
        final List<Integer> res = new ArrayList<>(counters.size());
        for (int i = 0; i < counters.size(); i++) {
            res.add(counters.get(i).reduce(current.get(i), accumulator.get(i)));
        }
        return res;
    }

    @Override
    public String tableResultFormat(final List<Integer> result) {
        // edge case
        if (result == null) {
            return tableMissingFormat();
        }
        Utils.validateArg(result.size() == counters.size(), () -> "invalid number of results " + result.size());
        final List<String> res = new ArrayList<>(counters.size());
        for (int i = 0; i < counters.size(); i++) {
            res.add(counters.get(i).tableResultFormat(result.get(i)));
        }
        return String.join("\t", res);
    }

    @Override
    public String tableMissingFormat() {
        return counters.stream().map(PairIntegerTagCounter::tableMissingFormat)
                .collect(Collectors.joining("\t"));
    }

}
