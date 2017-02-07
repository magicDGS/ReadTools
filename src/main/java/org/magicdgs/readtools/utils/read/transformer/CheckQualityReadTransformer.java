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

package org.magicdgs.readtools.utils.read.transformer;

import org.magicdgs.readtools.RTDefaults;

import com.google.common.annotations.VisibleForTesting;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.QualityUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Checks for and errors out when it detects reads with base qualities that are not encoded with
 * phred-scaled quality scores, each {@link RTDefaults#SAMPLING_QUALITY_CHECKING_FREQUENCY}.
 *
 * Note: this is adapted from the GATK3 not back-ported behaviour of
 * {@link org.broadinstitute.hellbender.transformers.MisencodedBaseQualityReadTransformer}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class CheckQualityReadTransformer implements ReadTransformer {
    private static final long serialVersionUID = 1L;

    // sample 1 read each this number of reads
    private static final int SAMPLING_FREQUENCY = RTDefaults.SAMPLING_QUALITY_CHECKING_FREQUENCY;

    // atomic integer in case of concurrent usage of this transformer
    @VisibleForTesting
    protected AtomicInteger currentReadCounter = new AtomicInteger(0);

    @Override
    public GATKRead apply(GATKRead read) {
        // sample reads randomly for checking
        if (currentReadCounter.incrementAndGet() >= SAMPLING_FREQUENCY) {
            currentReadCounter.set(0);
            final byte[] quals = read.getBaseQualities();
            for (final byte qual : quals) {
                if (qual > QualityUtils.MAX_REASONABLE_Q_SCORE) {
                    throw new UserException.MisencodedQualityScoresRead(read,
                            "we encountered an extremely high quality score of " + (int) qual);
                }
            }
        }
        return read;
    }
}
