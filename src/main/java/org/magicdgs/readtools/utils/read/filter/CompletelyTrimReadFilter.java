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

package org.magicdgs.readtools.utils.read.filter;

import org.magicdgs.readtools.utils.read.RTReadUtils;

import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Filters completely trim reads based on the reserved trimming tags
 * ({@link org.magicdgs.readtools.utils.read.ReservedTags#ct},
 * {@link org.magicdgs.readtools.utils.read.ReservedTags#ts},
 * {@link org.magicdgs.readtools.utils.read.ReservedTags#te}).
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @see RTReadUtils#updateCompletelyTrimReadFlag(GATKRead).
 */
public final class CompletelyTrimReadFilter extends ReadFilter {
    private static final long serialVersionUID = 1L;

    /**
     * Filters the read based on the {@link org.magicdgs.readtools.utils.read.ReservedTags#ct}.
     *
     * Note: the read will be updated for {@link org.magicdgs.readtools.utils.read.ReservedTags#ct}
     * using the trimming coordinates.
     */
    @Override
    public boolean test(final GATKRead read) {
        return !RTReadUtils.updateCompletelyTrimReadFlag(read);
    }
}
