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

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Trimmer for crop some bases in one or both sides of the read.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class CutReadTrimmer extends TrimmingFunction {
    private static final long serialVersionUID = 1L;

    private static final String FIVE_PRIME_LONG_NAME = "cut5primeBases";
    private static final String FIVE_PRIME_SHORT_NAME = "c5p";
    private static final String THREE_PRIME_LONG_NAME = "cut3primeBases";
    private static final String THREE_PRIME_SHORT_NAME = "c3p";

    /** The number of bases from the 5 prime of the read to trim. */
    @Argument(fullName = FIVE_PRIME_LONG_NAME, shortName = FIVE_PRIME_SHORT_NAME, doc = "Number of bases (in bp) to cut in the 5 prime of the read. For disable, use 'null'.", optional = true)
    public Integer fivePrime;
    /** The number of bases from the 3 prime of the read to trim. */
    @Argument(fullName = THREE_PRIME_LONG_NAME, shortName = THREE_PRIME_SHORT_NAME, doc = "Number of bases (in bp) to cut in the 3 prime of the read. For disable, use 'null'.", optional = true)
    public Integer threePrime;

    /** Constructor with default values. */
    public CutReadTrimmer() {}

    /**
     * Constructor with a 5/3 prime points.
     *
     * Note: at least one of the ends of the read should be trimmed.
     *
     * @param fivePrime  5 prime number of bases to cut. Use {@code 0} for disable 5-prime cutting.
     * @param threePrime 3 prime number of bases to cut. Use {@code 0} for disable 3-prime cutting.
     */
    // TODO: this have a bug and/or a design problem
    public CutReadTrimmer(final int fivePrime, final int threePrime) {
        // disable if the ints are 0
        this.fivePrime = (fivePrime == 0) ? null : fivePrime;
        this.threePrime = (threePrime == 0) ? null : threePrime;

        // validate args
        try {
            validateArgs();
        } catch (CommandLineException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
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

    @Override
    public void validateArgs() {
        if (this.fivePrime == null && this.threePrime == null) {
            throw new CommandLineException.BadArgumentValue(
                    "Both ends of the read are disable for CutReadTrimmer.");
        }
        if (this.fivePrime != null && this.fivePrime < 0) {
            throw new CommandLineException.BadArgumentValue("--" + FIVE_PRIME_LONG_NAME,
                    fivePrime.toString(), "should be a positive integer");
        }
        if (this.threePrime != null && this.threePrime < 0) {
            throw new CommandLineException.BadArgumentValue("--" + THREE_PRIME_LONG_NAME,
                    threePrime.toString(), "threePrime should be a positive integer");
        }
    }
}
