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
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Trimming function is a especial {@link ReadTransformer} that updates in-place the trimming tags
 * on a read passed to it. If the read is already trimmed, the function will not be applied in the
 * default implementation.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @see RTReadUtils#updateCompletelyTrimReadFlag(GATKRead)
 * @see RTReadUtils#updateTrimmingPointTags(GATKRead, int, int)
 * @see RTReadUtils#updateTrimmingStartPointTag(GATKRead, int)
 * @see RTReadUtils#updateTrimmingEndPointTag(GATKRead, int)
 * @see org.magicdgs.readtools.utils.read.ReservedTags#ts
 * @see org.magicdgs.readtools.utils.read.ReservedTags#te
 */
public abstract class TrimmingFunction implements ReadTransformer, Serializable {
    public static final long serialVersionUID = 1L;

    private boolean disable5prime = false;
    private boolean disable3prime = false;

    // supplier for get the exception if it should thrown
    private Supplier<IllegalStateException> stateException = null;

    /**
     * Update and check if the read is already trimmed, and if so it skips the trimming. Otherwise,
     * it gets the trimming points from {@link #fillTrimPoints(GATKRead, int[])} and apply then to
     * the reads.
     *
     * Note: the read is updated in-place and returned.
     */
    @Override
    public final GATKRead apply(final GATKRead read) {
        Utils.nonNull(read, "null read");
        // set the illegal state exception if not present
        setIllegalStateException(() -> "apply");
        // update the completely trim read flag and only apply if not completely trimmed
        if (!RTReadUtils.updateCompletelyTrimReadFlag(read)) {
            // get the trimming points from the implementation -> initial values set to not trim
            final int[] trimmingPoints = new int[] {0, Integer.MAX_VALUE};
            fillTrimPoints(read, trimmingPoints);
            // update the trimming points
            RTReadUtils.updateTrimmingPointTags(read,
                    // use 0 because internally it conserves the right most position
                    (disable5prime) ? 0 : trimmingPoints[0],
                    // use the max value because internally there is a check for the length
                    (disable3prime) ? Integer.MAX_VALUE : trimmingPoints[1]);
        }
        return read;
    }

    /**
     * Check if it is safe to use {@link #setDisableEnds(boolean, boolean)}. They cannot be set if:
     * - It was already set.
     * - The function was already applied.
     *
     * @return {@code true} if {@link #setDisableEnds(boolean, boolean)} can be called;
     * {@code false} otherwise.
     */
    public final boolean canSetDisableEnds() {
        return stateException != null;
    }

    /**
     * Sets if some end should be disabled.
     *
     * Note: this should be used before any other public method is called except
     * {@link #isDisable3prime()} or {@link #isDisable5prime()}.
     *
     * @throws IllegalStateException if {@link #canSetDisableEnds()} returns {@code false}.
     * @see #canSetDisableEnds()
     */
    public final void setDisableEnds(final boolean disable5prime, final boolean disable3prime) {
        if (canSetDisableEnds()) {
            throw stateException.get();
        }
        Utils.validateArg(!(disable5prime & disable3prime),
                "cannot disable both ends in a trimmer");
        this.disable5prime = disable5prime;
        this.disable3prime = disable3prime;
        setIllegalStateException(() -> "setDisableEnds");
    }

    /** If {@code true}, this trimmer does not trim the 5 prime of the read. */
    public final boolean isDisable5prime() {
        return disable5prime;
    }

    /** If {@code true}, does not trim the 3 prime of the read. */
    public final boolean isDisable3prime() {
        return disable3prime;
    }

    /**
     * Gets the trimming points for applying this trimmer. If one end is not trimmed, it does not
     * require to update that index.
     *
     * @param read   the read to use for getting the trimming points. It should not be modified.
     * @param toFill array of length 2, to fill in the trimming points. Index 0 should have the
     *               first and index 1 the second.
     */
    protected abstract void fillTrimPoints(final GATKRead read, final int[] toFill);

    /**
     * Unsafe validation of arguments (may change if {@link #setDisableEnds(boolean, boolean)} is
     * called, which throws a command line exception or user exception depending on the
     * implementation. For a safe validation, use {@link #validateArgs()}.
     *
     * WARNING: use only if a validation is required before calling
     * {@link #setDisableEnds(boolean, boolean)}
     *
     * Default behaviour does not perform any validation. Implementations should verify that their
     * arguments are compatible with {@link #isDisable5prime()} and/or {@link #isDisable3prime()}.
     *
     * @throws org.broadinstitute.barclay.argparser.CommandLineException if arguments are invalid.
     * @throws org.broadinstitute.hellbender.exceptions.UserException    if arguments are invalid.
     */
    public void validateArgsUnsafe() { }

    /**
     * Validates the arguments and throws a command line exception or user exception if the
     * arguments are invalid.
     *
     * @throws org.broadinstitute.barclay.argparser.CommandLineException if arguments are invalid.
     * @throws org.broadinstitute.hellbender.exceptions.UserException    if arguments are invalid.
     */
    public final void validateArgs() {
        setIllegalStateException(() -> "validateArgs");
        validateArgsUnsafe();
    }

    // sets the IllegalStateException to throw if not already set
    private final void setIllegalStateException(final Supplier<String> methodName) {
        if (this.stateException == null) {
            this.stateException = () -> new IllegalStateException(String
                    .format("Setting disable ends when %s() was already called for %s",
                            methodName.get(), this.toString()));
        }
    }
}
