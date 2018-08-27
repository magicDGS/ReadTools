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

import org.magicdgs.readtools.utils.read.RTReadUtils;

import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.clipping.ClippingOp;
import org.broadinstitute.hellbender.utils.clipping.ClippingRepresentation;
import org.broadinstitute.hellbender.utils.clipping.ReadClipper;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.util.Arrays;

/**
 * ReadTransformer for hard-clipping ("cut") reads based on the trimming flags
 * ({@link org.magicdgs.readtools.utils.read.ReservedTags#ct},
 * {@link org.magicdgs.readtools.utils.read.ReservedTags#ts},
 * {@link org.magicdgs.readtools.utils.read.ReservedTags#te}).
 * Trimming point tags are removed using {@link RTReadUtils#clearTrimmingPointTags(GATKRead)},
 * except {@link org.magicdgs.readtools.utils.read.ReservedTags#ct}.
 *
 * <p>Note: this read transformer assumes that the trimming tags are independent on the strand.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @see ReadClipper
 */
public final class ApplyTrimResultReadTransformer implements ReadTransformer {
    private static final long serialVersionUID = 1L;

    /**
     * If the read is completely trimmed, does nothing. Otherwise, the read is hard-clipped based
     * on the trimming flags.
     *
     * <p>Note: the read is modified in-place.
     *
     * @return the same read object, trimmed as necessary.
     */
    @Override
    public GATKRead apply(final GATKRead read) {
        Utils.nonNull(read, "null read");
        // use the safe version
        return (RTReadUtils.updateCompletelyTrimReadFlag(read))
                ? handleCompletelyTrimmed(read)
                : handleTrimmed(read);
    }

    private static GATKRead handleTrimmed(final GATKRead read) {
        // store the start and the end, and remove the tags
        // the tags should be removed here because the completely trimmed flag is set
        // before removing and thus if the read was trimmed the length is not longer the same
        final int start = RTReadUtils.getTrimmingStartPoint(read);
        final int end = RTReadUtils.getTrimmingEndPoint(read);
        RTReadUtils.clearTrimmingPointTags(read);

        if (read.isUnmapped()) {
            // it is safe to use the no-copy methods because we are doing a copy anyway
            final byte[] newBases = Arrays.copyOfRange(read.getBasesNoCopy(), start, end);
            final byte[] newQuals = Arrays.copyOfRange(read.getBaseQualitiesNoCopy(), start, end);
            read.setBases(newBases);
            read.setBaseQualities(newQuals);
        } else {
            final int readLength = read.getLength();
            final ReadClipper clipper = new ReadClipper(read);
            // we have to clip the end first, because the read clipper works in a step-wise way
            if (end != readLength) {
                clipper.addOp(new ClippingOp(end, readLength));
            }
            if (start != 0) {
                // we should remove 1 for correctly clipping
                clipper.addOp(new ClippingOp(0, start - 1));
            }
            // this should modify in place here
            modifyWithClipped(read, clipper.clipRead(ClippingRepresentation.HARDCLIP_BASES));
            // return the same read
        }

        // it is modified in place
        return read;
    }

    // required to modify in-place
    private static void modifyWithClipped(final GATKRead read, final GATKRead clipped) {
        // TODO: maybe this method requires something else if the ClippingRepresentation is settable
        // here we use the unsafe methods, because we are not doing anything with the clipped read
        read.setBaseQualities(clipped.getBaseQualitiesNoCopy());
        read.setBases(clipped.getBasesNoCopy());
        read.setCigar(clipped.getCigar());
        read.setPosition(clipped); // TODO: maybe this adds to much complexity
        if (ReadUtils.hasBaseIndelQualities(read)) {
            ReadUtils.setInsertionBaseQualities(read, ReadUtils.getBaseInsertionQualities(clipped));
            ReadUtils.setDeletionBaseQualities(read, ReadUtils.getBaseDeletionQualities(clipped));
        }
    }

    private static GATKRead handleCompletelyTrimmed(final GATKRead read) {
        // TODO: probably we should operate differently
        // TODO: for instance, all the bases transformed to Ns or qualities to 0?
        RTReadUtils.clearTrimmingPointTags(read);
        return read;
    }
}
