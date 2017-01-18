/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.utils.read.transformer.trimming;

import org.magicdgs.readtools.utils.read.RTReadUtils;

import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Trimmer for crop some bases in one or both sides of the read.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class CutReadTrimmer implements TrimmingFunction {
    private static final long serialVersionUID = 1L;

    // TODO: make both parameters!
    // TODO: the parameters should be mutually exclusive and required one of them
    /** The number of bases from the 5 prime of the read to trim. */
    public Integer fivePrime;
    /** The number of bases from the 3 prime of the read to trim. */
    public Integer threePrime;

    /**
     * Constructor with a 5/3 prime points.
     *
     * Note: at least one of the ends of the read should be trimmed.
     *
     * @param fivePrime  5 prime number of bases to cut. May be {@code null} or
     *                   {@code 0} for disable 5-prime cutting.
     * @param threePrime 3 prime number of bases to cut. May be {@code null} or
     *                   {@link Integer#MAX_VALUE} for disable 3-prime cutting.
     */
    public CutReadTrimmer(final Integer fivePrime, final Integer threePrime) {
        // setup
        this.fivePrime = (fivePrime == null || fivePrime == 0)
                ? null : fivePrime;
        this.threePrime = (threePrime == null || threePrime == Integer.MAX_VALUE)
                ? null : threePrime;

        // validate args
        Utils.validateArg(!(this.fivePrime == null && this.threePrime == null),
                "at least one of the sides of the read should be trimmed");
        Utils.validateArg(this.fivePrime == null || this.fivePrime > 0,
                "fivePrime should be a positive integer");
        Utils.validateArg(this.threePrime == null || this.threePrime > 0,
                "threePrime should be a positive integer");
    }

    @Override
    public void update(final GATKRead read) {
        final int readLength = read.getLength();
        if (fivePrime != null) {
            final int trimPoint = (fivePrime > readLength) ? readLength : fivePrime;
            RTReadUtils.updateTrimmingStartPointTag(read, trimPoint);
        }
        if (threePrime != null) {
            final int trimPoint = readLength - threePrime;
            RTReadUtils.updateTrimmingEndPointTag(read, (trimPoint < 0) ? 0 : trimPoint);
        }
    }
}
