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

import org.magicdgs.readtools.utils.read.RTReadUtils;

import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.clipping.ClippingOp;
import org.broadinstitute.hellbender.utils.clipping.ClippingRepresentation;
import org.broadinstitute.hellbender.utils.clipping.ReadClipper;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Arrays;

/**
 * ReadTransformer for hard-clipping ("cut") reads based on the trimming flags
 * ({@link org.magicdgs.readtools.utils.read.ReservedTags#ct},
 * {@link org.magicdgs.readtools.utils.read.ReservedTags#ts},
 * {@link org.magicdgs.readtools.utils.read.ReservedTags#te}).
 *
 * Note: this read transformer assumes that the trimming tags are independent on the strand.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @see ReadClipper
 */
public final class ApplyTrimResultReadTransfomer implements ReadTransformer {
    private static final long serialVersionUID = 1L;

    /** If {@code true}, does not trim the 5' of the read. */
    private final boolean no5prime;
    /** If {@code false}, does not trim the 3' of the read. */
    private final boolean no3prime;

    public ApplyTrimResultReadTransfomer() {
        this(false, false);
    }

    /**
     * Constructor for setting up if the 5' or 3' end should be hard-clipped or not.
     *
     * @param no5prime if {@code true}, does not hard-clip the 5' of the read.
     * @param no3prime if {@code true}, does not hard-clip the 3' of the read.
     *
     * @throws IllegalArgumentException if both end are switch off.
     */
    public ApplyTrimResultReadTransfomer(final boolean no5prime, final boolean no3prime) {
        Utils.validateArg(!(no5prime && no3prime), "at least one end (5' or 3') should be trimmed");
        this.no5prime = no5prime;
        this.no3prime = no3prime;
    }

    /**
     * If the read is completely trimmed, does nothing. Otherwise, the read is hard-clipped based
     * on the trimming flags.
     */
    @Override
    public GATKRead apply(final GATKRead read) {
        Utils.nonNull(read, "null read");
        // use the safe version
        return (RTReadUtils.updateCompletelyTrimReadFlag(read))
                ? handleCompletelyTrimmed(read)
                : handleTrimmed(read, no5prime, no3prime);
    }

    private static GATKRead handleTrimmed(final GATKRead read, boolean no5prime, boolean no3prime) {
        final int readLength = read.getLength();
        final int start = (no5prime) ? 0 : RTReadUtils.getTrimmingStartPoint(read);
        final int end = (no3prime) ? readLength : RTReadUtils.getTrimmingEndPoint(read);
        if (read.isUnmapped()) {
            final byte[] newBases = Arrays.copyOfRange(read.getBases(), start, end);
            final byte[] newQuals = Arrays.copyOfRange(read.getBaseQualities(), start, end);
            // TODO: should we modify in place or not?
            read.setBases(newBases);
            read.setBaseQualities(newQuals);
            return read;
        } else {
            final ReadClipper clipper = new ReadClipper(read);
            // we have to clip the end first, because the read clipper works in a step-wise way
            if (end != readLength) {
                clipper.addOp(new ClippingOp(end, readLength));
            }
            if (start != 0) {
                // we should remove 1 for correctly clipping
                clipper.addOp(new ClippingOp(0, start - 1));
            }
            return clipper.clipRead(ClippingRepresentation.HARDCLIP_BASES);
        }
    }

    private static GATKRead handleCompletelyTrimmed(final GATKRead read) {
        // TODO: this is just an identity function
        // TODO: but probably we should operate differently
        // TODO: for instance, all the bases transformed to Ns or qualities to 0?
        return read;
    }
}
