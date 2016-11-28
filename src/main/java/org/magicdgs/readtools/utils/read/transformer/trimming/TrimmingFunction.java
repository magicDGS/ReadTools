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
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Trimming function is a especial {@link ReadTransformer} that updates in-place the trimming tags
 * on a read passed to it. If the read is already trimmed, the function will not be applied in the
 * default implementation.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @see org.magicdgs.readtools.utils.read.RTReadUtils#updateTrimmingPointTags(GATKRead, int, int)
 * @see org.magicdgs.readtools.utils.read.RTReadUtils#updateTrimmingStartPointTag(GATKRead, int)
 * @see org.magicdgs.readtools.utils.read.RTReadUtils#updateTrimmingEndPointTag(GATKRead, int)
 * @see org.magicdgs.readtools.utils.read.ReservedTags#ts
 * @see org.magicdgs.readtools.utils.read.ReservedTags#te
 */
public interface TrimmingFunction extends ReadTransformer {
    public static final long serialVersionUID = 1L;

    /**
     * Default implementation check if the read is already trimmed, and if so it skips the
     * implementation.
     */
    @Override
    default GATKRead apply(final GATKRead read) {
        Utils.nonNull(read, "null read");
        if (!RTReadUtils.isCompletelyTrimRead(read)) {
            update(read);
        }
        return read;
    }

    /** Update the read with the result of this trim function. */
    void update(final GATKRead read);

}
