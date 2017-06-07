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

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.utils.trimming.TrimmingUtil;

import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Trims trailing Ns (unknown bases) in the read sequence, in one or both sides.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @ReadTools.warning If a previous trimmer left the read starting/ending with Ns, the read will not
 * be trimmed by the TrailingNtrimmer.
 */
@DocumentedFeature(groupName = RTHelpConstants.DOC_CAT_TRIMMERS, groupSummary = RTHelpConstants.DOC_CAT_TRIMMERS_SUMMARY, summary = "Trims the end of the read containing unknown bases.")
public final class TrailingNtrimmer extends TrimmingFunction {
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     *
     * @see TrimmingUtil#trimPointsTrailingNs(byte[])
     */
    @Override
    protected void fillTrimPoints(final GATKRead read, final int[] toFill) {
        final int[] points = TrimmingUtil.trimPointsTrailingNs(read.getBases());
        toFill[0] = points[0];
        toFill[1] = points[1];
    }
}
