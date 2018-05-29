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

package org.magicdgs.readtools.utils.read.stats.singlestat;

import org.magicdgs.readtools.utils.read.stats.SingleReadStatFunction;
import org.magicdgs.readtools.utils.read.stats.StatFunctionUtils;

import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Counts the number of reads containing indels (cigar 'I' or 'D').
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: add group for this and documented feature once is a plugin (https://github.com/magicDGS/ReadTools/issues/448)
public class ContainIndelCounter implements SingleReadStatFunction<Integer> {

    @Override
    public String getStatName() {
        return "read.contain.indel";
    }

    @Override
    public Integer compute(final GATKRead read) {
        return read.getCigarElements().stream().anyMatch(s -> s.getOperator().isIndel()) ? 1 : 0;
    }

    @Override
    public Integer reduce(final Integer current, final Integer accumulator) {
        return StatFunctionUtils.sumReduce(current, accumulator);
    }
}
