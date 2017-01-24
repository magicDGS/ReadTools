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

import java.io.Serializable;

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
public abstract class TrimmingFunction implements ReadTransformer, Serializable {
    public static final long serialVersionUID = 1L;

    /**
     * Check if the read is already trimmed, and if so it skips the {@link #update(GATKRead)}
     * implementation.
     */
    @Override
    public final GATKRead apply(final GATKRead read) {
        Utils.nonNull(read, "null read");
        if (!RTReadUtils.isCompletelyTrimRead(read)) {
            update(read);
        }
        return read;
    }

    /** Update the read with the result of this trim function. */
    protected abstract void update(final GATKRead read);

    /**
     * Validates the arguments and throws a command line exception or user exception depending on
     * the implementation.
     *
     * Default behaviour does not perform any validation.
     *
     * @throws org.broadinstitute.barclay.argparser.CommandLineException if arguments are invalid.
     * @throws org.broadinstitute.hellbender.exceptions.UserException if arguments are invalid.
     * */
    public void validateArgs() { }
}
