/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.tools.trimming.trimmers.Trimmer;
import org.magicdgs.readtools.tools.trimming.trimmers.TrimmerBuilder;

import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;
import org.broadinstitute.hellbender.exceptions.UserException;

/**
 * Argument collection for trimming algorithm
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingArgumentCollection implements ArgumentCollectionDefinition {
    private static final long serialVersionUID = 1L;

    private static final String QUALITY_THRESHOLD_LONG_NAME = "quality-threshold";
    private static final String QUALITY_THRESHOLD_SHORT_NAME = "q";
    private static final String QUALITY_THRESHOLD_DOC =
            "Minimum average quality. A modified Mott algorithm is used for trimming, and the threshold is used for calculating a score: quality_at_base - threshold.";

    @Argument(fullName = QUALITY_THRESHOLD_LONG_NAME, shortName = QUALITY_THRESHOLD_SHORT_NAME, optional = true, doc = QUALITY_THRESHOLD_DOC)
    public Integer qualThreshold = 20;

    private static final String DISCARD_INTERNAL_N_LONG_NAME = "discard-internal-N";
    private static final String DISCARD_INTERNAL_N_SHORT_NAME = "N";
    private static final String DISCARD_INTERNAL_N_DOC =
            "If set reads having internal Ns will be discarded";

    @Argument(fullName = DISCARD_INTERNAL_N_LONG_NAME, shortName = DISCARD_INTERNAL_N_SHORT_NAME, optional = true, doc = DISCARD_INTERNAL_N_DOC)
    public Boolean discardRemainingNs = false;

    private static final String MINIMUM_LENGTH_LONG_NAME = "minimum-length";
    private static final String MINIMUM_LENGTH_SHORT_NAME = "m";
    private static final String MINIMUM_LENGTH_DOC =
            "The minimum length of the read after trimming.";

    @Argument(fullName = MINIMUM_LENGTH_LONG_NAME, shortName = MINIMUM_LENGTH_SHORT_NAME, optional = true, doc = MINIMUM_LENGTH_DOC)
    public Integer minLength = 40;

    private static final String MAXIMUM_LENGTH_LONG_NAME = "maximum-length";
    private static final String MAXIMUM_LENGTH_SHORT_NAME = "max";
    private static final String MAXIMUM_LENGTH_DOC =
            "The maximum length of the read after trimming. If null, no threshold will be applied";

    @Argument(fullName = MAXIMUM_LENGTH_LONG_NAME, shortName = MAXIMUM_LENGTH_SHORT_NAME, optional = true, doc = MAXIMUM_LENGTH_DOC)
    public Integer maxLength = null;

    private static final String NO_TRIM_QUALITY_LONG_NAME = "no-trim-quality";
    private static final String NO_TRIM_QUALITY_SHORT_NAME = "nq";
    private static final String NO_TRIM_QUALITY_DOC = "Switch off quality trimming";

    @Argument(fullName = NO_TRIM_QUALITY_LONG_NAME, shortName = NO_TRIM_QUALITY_SHORT_NAME, optional = true, doc = NO_TRIM_QUALITY_DOC)
    public Boolean dontTrimQuality = false;

    private static final String NO_TRIM_5P_LONG_NAME = "no-5p-trim";
    private static final String NO_TRIM_5P_SHORT_NAME = "n5p";
    private static final String NO_TRIM_5P_DOC =
            "Disable 5'-trimming (quality and 'N'); May be useful for the identification of duplicates when using trimming of reads. Duplicates are usually identified by the 5' mapping position which should thus not be modified by trimming";

    @Argument(fullName = NO_TRIM_5P_LONG_NAME, shortName = NO_TRIM_5P_SHORT_NAME, optional = true, doc = NO_TRIM_5P_DOC)
    public Boolean no5ptrim = false;

    // TODO: this options does not belong to here, I guess....
    private static final String KEEP_DISCARDED_LONG_NAME = "keep-discarded";
    private static final String KEEP_DISCARDED_SHORT_NAME = "k";
    private static final String KEEP_DISCARDED_DOC =
            "Keep the reads completely trimmed or that does not pass the thresholds in a discarded file (original reads stored). May be useful for quality control.";

    @Argument(fullName = KEEP_DISCARDED_LONG_NAME, shortName = KEEP_DISCARDED_SHORT_NAME, optional = true, doc = KEEP_DISCARDED_DOC)
    public Boolean keepDiscard = false;

    /**
     * Gets the trimmer from the parameters.
     *
     * @param single {@code true} if single-end; {@code false} otherwise.
     */
    public Trimmer getTrimmer(final boolean single) {
        return new TrimmerBuilder(single)
                .setTrimQuality(!dontTrimQuality)
                .setQualityThreshold(qualThreshold)
                .setMinLength(minLength)
                .setMaxLength(maxLength == null ? Integer.MAX_VALUE : maxLength)
                .setDiscardRemainingNs(discardRemainingNs)
                .setNo5pTrimming(no5ptrim)
                .build();
    }

    /**
     * Validates that the arguments are in the correct range.
     *
     * @throws UserException.BadArgumentValue if found a wrong provided value.
     */
    public void validateArguments() {
        if (qualThreshold < 0) {
            throw new UserException.BadArgumentValue(QUALITY_THRESHOLD_LONG_NAME,
                    qualThreshold.toString(), "cannot be a negative value");
        }
        if (minLength < 1) {
            throw new UserException.BadArgumentValue(MINIMUM_LENGTH_LONG_NAME,
                    minLength.toString(), "should be a positive integer");
        }
        if (maxLength != null && maxLength < 1) {
            throw new UserException.BadArgumentValue(MAXIMUM_LENGTH_LONG_NAME,
                    minLength.toString(), "should be a positive integer");
        }
        if (maxLength != null && minLength > maxLength) {
            throw new UserException.BadArgumentValue(String.format(
                    "--%s (%s) should be smaller or equal than --%s (%s)",
                    MINIMUM_LENGTH_LONG_NAME, MAXIMUM_LENGTH_LONG_NAME,
                    minLength, maxLength));
        }
    }
}
