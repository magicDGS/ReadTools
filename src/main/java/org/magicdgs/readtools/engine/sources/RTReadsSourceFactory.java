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

package org.magicdgs.readtools.engine.sources;

import org.magicdgs.readtools.engine.sources.fastq.FastqReadsSource;
import org.magicdgs.readtools.engine.sources.sam.SamReadsSource;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;

import org.broadinstitute.hellbender.exceptions.UserException;

/**
 * Factory implementation for retrieve read sources.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTReadsSourceFactory {

    /**
     * Gets a single-end source of reads.
     *
     * @param singleEndSource the name of the source.
     *
     * @return a single-end source of reads.
     */
    public RTReadsSource getSingleEndSource(final String singleEndSource) {
        return getMaybePairedReadSource(singleEndSource, false);
    }

    /**
     * Gets a pair-end source of reads stored in two sources.
     *
     * @param firstSource  the name of source of reads containing the first reads of the pair.
     * @param secondSource the name of source of reads containing the first reads of the pair.
     *
     * @return a pair-end source of reads.
     */
    public RTReadsSource getSplitPairEndSource(final String firstSource,
            final String secondSource) {
        final RTReadsSource first = getSingleEndSource(firstSource);
        final RTReadsSource second = getSingleEndSource(secondSource);
        return new PairedEndSplitReadsSource(first, second);
    }

    /**
     * Gets a pair-end source of reads stored as an interleaved file.
     *
     * @param interleavedSource the name of the interleaved source of reads.
     *
     * @return a pair-end source of reads.
     */
    public RTReadsSource getInterleavedPairEndSource(final String interleavedSource) {
        return getMaybePairedReadSource(interleavedSource, true);
    }

    /**
     * Gets a maybe paired read source.
     *
     * <p>Note: this method is used internally and can be overriden to allow custom
     * implementations, but is is not supposed to be called by factory users.
     *
     * <p>Default implementation check for FASTQ format and then for SAM/BAM/CRAM using
     * {@link ReadToolsIOFormat}.
     *
     * @param source      the name of the source.
     * @param interleaved {@code true} if the source is interleaved; {@code false} otherwise.
     *
     * @return a source of reads, paired or not.
     */
    protected RTReadsSource getMaybePairedReadSource(final String source,
            final boolean interleaved) {
        if (ReadToolsIOFormat.isFastq(source)) {
            return new FastqReadsSource(source, interleaved);
        } else if (ReadToolsIOFormat.isSamBamOrCram(source)) {
            return new SamReadsSource(source, interleaved);
        }
        // TODO: add IO format for Distmap
        throw new UserException("Not recognized input by extension: " + source);
    }

}
