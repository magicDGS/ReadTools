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

package org.magicdgs.readtools.utils.read.transformer.trimming;

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

    public CutReadTrimmer(final int fivePrime, final int threePrime) {
        // disable if the ints are 0
        this.fivePrime = (fivePrime == 0) ? null : fivePrime;
        this.threePrime = (threePrime == 0) ? null : threePrime;

        // validate args
        try {
            validateArgsUnsafe();
        } catch (CommandLineException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillTrimPoints(final GATKRead read, final int[] toFill) {
        final int readLength = read.getLength();
        if (fivePrime != null) {
            toFill[0] = (fivePrime > readLength) ? readLength : fivePrime;
        }
        if (threePrime != null) {
            final int trimPoint = readLength - threePrime;
            toFill[1] = (trimPoint < 0) ? 0 : trimPoint;
        }
    }

    /**
     * Validates the arguments and throws {@link CommandLineException.BadArgumentValue} if:
     *
     * - Both ends of the read are disabled.
     * - Values for parameters are not positive integers.
     * - A value is set for trimming when it is disabled.
     */
    @Override
    public void validateArgsUnsafe() {
        // both ends cannot be disabled
        if (this.fivePrime == null && this.threePrime == null) {
            throw new CommandLineException.BadArgumentValue(
                    "Both ends of the read are disable for CutReadTrimmer.");
        }
        // if the five prime is not null, check its value
        if (fivePrime != null) {
            if (this.fivePrime < 0) {
                throw new CommandLineException.BadArgumentValue("--" + FIVE_PRIME_LONG_NAME,
                        fivePrime.toString(), "Should be a positive integer");
            } else if (isDisable5prime()) {
                throw new CommandLineException.BadArgumentValue("--" + FIVE_PRIME_LONG_NAME,
                        fivePrime.toString(), "Cannot be used in when 5 prime trimming is disabled");
            }
        }
        // the same for three prime
        if (this.threePrime != null) {
            if (this.threePrime < 0) {
                throw new CommandLineException.BadArgumentValue("--" + THREE_PRIME_LONG_NAME,
                        threePrime.toString(), "Should be a positive integer");
            } else if (isDisable3prime()) {
                throw new CommandLineException.BadArgumentValue("--" + THREE_PRIME_LONG_NAME,
                        threePrime.toString(),
                        "Cannot be used in when 3 prime trimming is disabled");
            }
        }
    }
}
