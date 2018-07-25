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

package org.magicdgs.readtools.utils.mappability.gem;

import htsjdk.samtools.util.Locatable;
import org.apache.commons.lang3.Range;
import org.broadinstitute.hellbender.utils.Utils;

/**
 * {@link Locatable} implementation of GEM-mappability record.
 *
 * <p>In GEM-mappability, every position of the genome is associated with a number of mappings. For
 * large number of mappings, the value is encoded as a char representing a range of values. This
 * class holds the information per-base.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class GemMappabilityRecord implements Locatable {
    private final String contig;
    private final int position;
    private final Range<Long> range;

    /**
     * Default constructor.
     *
     * @param contig   contig for the record.
     * @param position position for the record.
     * @param range    range of values in GEM-mappability file associated with this position.
     */
    public GemMappabilityRecord(final String contig, final int position,
            final Range<Long> range) {
        this.contig = Utils.nonNull(contig);
        this.position = position;
        this.range =  Utils.nonNull(range, () -> "null range for " + contig + ":" + position);
    }

    /**
     * Gets the range of values of this position reported by GEM-mappability.
     *
     * @return range of values.
     */
    public Range<Long> getRange() {
        return range;
    }

    @Override
    public String getContig() {
        return contig;
    }

    @Override
    public int getStart() {
        return position;
    }

    @Override
    public int getEnd() {
        return position;
    }

    @Override
    public String toString() {
        return contig + ":" + position + range;
    }
}
