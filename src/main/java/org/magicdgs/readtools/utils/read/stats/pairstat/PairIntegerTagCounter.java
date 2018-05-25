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
import org.magicdgs.readtools.utils.read.stats.StatFunctionUtils;

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

/**
 * Counts the number of read pairs whose integer SAM tag is compared to a threshold.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: add group for this and documented feature once is a plugin
public class PairIntegerTagCounter implements PairEndReadStatFunction<Integer, Boolean> {

    // TODO: maybe this could be more liek a TaggedArgument and then initialize the counters
    // TODO: something like --count-pair-int-tag:NM:EQ 2
    public static final String TAG_ARG_NAME = "count-pair-int-tag";
    public static final String TAG_ARG_DESCRIPTION = "Integer SAM tag to count for pairs";
    public static final String OP_ARG_NAME = "count-pair-int-tag-operator";
    public static final String OP_ARG_DESCRIPTION = "Operation for the integer SAM tag (with respect to the threshold)";
    public static final String VAL_ARG_NAME = "count-pair-int-tag-threshold";
    public static final String VAL_ARG_DESCRIPTION = "Threshold for the integer SAM tag (with respect to the operation)";

    @Argument(fullName = TAG_ARG_NAME, doc = TAG_ARG_DESCRIPTION)
    public String tag;

    @Argument(fullName = OP_ARG_NAME, doc = OP_ARG_DESCRIPTION)
    public RelationalOperator op;

    @Argument(fullName = VAL_ARG_NAME, doc = VAL_ARG_DESCRIPTION)
    public Integer threshold;

    /**
     * Default constructor.
     *
     * @param tag integer SAM tag.
     * @param operator relational operator for the comparison.
     * @param threshold threshold for the comparison.
     */
    public PairIntegerTagCounter(final String tag, final RelationalOperator operator, final int threshold) {
        this.tag = tag;
        this.op = operator;
        this.threshold = threshold;
        init();
    }

    /**
     * Empty constructor to enable usage as a plugin and as an
     * {@link org.broadinstitute.barclay.argparser.ArgumentCollection}
     *
     * <p>Note: {@link #init()} will fail if fields are not provided.
     */
    public PairIntegerTagCounter() { }

    /**
     * Validates that the field/arguments are non-null and that the tag is correctly formatted.
     */
    @Override
    public void init() {
        ReadUtils.assertAttributeNameIsLegal(tag);
        Utils.nonNull(op, () -> "operation should be provided");
        Utils.nonNull(threshold, () -> "threshold should be provided");
    }

    @Override
    public String getStatName() {
        return String.format("pair.%s.%s.%d", tag, op.name().toLowerCase(), threshold);
    }

    @Override
    public Boolean computeIntermediateFirst(final GATKRead read) {
        return op.test(read.getAttributeAsInteger(tag), threshold);
    }

    @Override
    public Integer mergePairValues(final Boolean first, final Boolean second) {
        return (first && second) ? 1 : 0;
    }

    @Override
    public Integer reduce(final Integer current, final Integer accumulator) {
        return StatFunctionUtils.sumReduce(current, accumulator);
    }
}
