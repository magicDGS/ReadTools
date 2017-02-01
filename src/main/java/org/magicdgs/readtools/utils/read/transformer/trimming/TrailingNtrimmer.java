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

import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Computes trim points for trailing Ns on the read sequence using the algorithm described in
 * {@link TrimmingUtil#trimPointsTrailingNs(byte[])}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class TrailingNtrimmer extends TrimmingFunction {
    private static final long serialVersionUID = 1L;

    /** Default constructor. */
    public TrailingNtrimmer() {}

    /**
     * {@inheritDoc}
     *
     * @see TrimmingUtil#trimPointsTrailingNs(byte[]).
     */
    @Override
    protected void fillTrimPoints(final GATKRead read, final int[] toFill) {
        final int[] points = TrimmingUtil.trimPointsTrailingNs(read.getBases());
        toFill[0] = points[0];
        toFill[1] = points[1];
    }
}
