/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.utils.trimming.TrimmingUtil;

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Computes trim points for quality drop under a certain threshold using the Mott algorithm
 * described in {@link TrimmingUtil#trimPointsMott(byte[], int)}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class MottQualityTrimmer extends TrimmingFunction {
    private static final long serialVersionUID = 1L;

    private static final String QUAL_THRESHOLD_LONG_NAME = "mottQualityThreshold";
    private static final String QUAL_THRESHOLD_SHORT_NAME = "mottQual";

    // TODO: improve doc
    /** The quality threshold to use for trimming. */
    @Argument(fullName = QUAL_THRESHOLD_LONG_NAME, shortName = QUAL_THRESHOLD_SHORT_NAME, doc = "Minimum average quality for the modified Mott algorithm. The threshold is used for calculating a score: quality_at_base - threshold.", optional = true)
    public int qualThreshold = 20;

    /** Constructor with default quality. */
    public MottQualityTrimmer() { }

    /** Constructor with a quality threshold. */
    public MottQualityTrimmer(final int qualThreshold) {
        Utils.validateArg(qualThreshold >= 0,
                () -> "qualityThreshold should be 0 or positive: " + qualThreshold);
        this.qualThreshold = qualThreshold;
    }

    /**
     * {@inheritDoc}
     *
     * @see TrimmingUtil#trimPointsMott(byte[], int).
     */
    @Override
    protected void fillTrimPoints(final GATKRead read, final int[] toFill) {
        final int[] trimPoints =
                TrimmingUtil.trimPointsMott(read.getBaseQualities(), qualThreshold);
        toFill[0] = trimPoints[0];
        toFill[1] = trimPoints[1];
    }

    /** Throws if there the quality threshold is negative. */
    @Override
    public void validateArgsUnsafe() {
        if (qualThreshold < 0) {
            throw new CommandLineException.BadArgumentValue("--" + QUAL_THRESHOLD_LONG_NAME,
                    String.valueOf(qualThreshold), "cannot be a negative value");
        }
    }
}
