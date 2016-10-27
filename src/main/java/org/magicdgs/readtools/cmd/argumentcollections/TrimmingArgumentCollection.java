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

import org.magicdgs.readtools.tools.ToolNames;
import org.magicdgs.readtools.tools.cmd.OptionUtils;
import org.magicdgs.readtools.tools.trimming.trimmers.Trimmer;
import org.magicdgs.readtools.tools.trimming.trimmers.TrimmerBuilder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Arguments for trimming algorithm
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingArgumentCollection {

    /** The default quality score. */
    public static final int DEFAULT_QUALTITY_SCORE = 20;

    /** The default minimum length. */
    private static final int DEFAULT_MINIMUM_LENGTH = 40;

    private static final String QUALITY_THRESHOLD_LONG_NAME = "quality-threshold";
    private static final String QUALITY_THRESHOLD_SHORT_NAME = "q";
    private static final String QUALITY_THRESHOLD_DOC =
            "Minimum average quality. A modified Mott algorithm is used for trimming, and the threshold is used for calculating a score: quality_at_base - threshold.";

    private static final String DISCARD_INTERNAL_N_LONG_NAME = "discard-internal-N";
    private static final String DISCARD_INTERNAL_N_SHORT_NAME = "N";
    private static final String DISCARD_INTERNAL_N_DOC =
            "If set reads having internal Ns will be discarded";

    private static final String MINIMUM_LENGTH_LONG_NAME = "minimum-length";
    private static final String MINIMUM_LENGTH_SHORT_NAME = "m";
    private static final String MINIMUM_LENGTH_DOC =
            "The minimum length of the read after trimming.";

    private static final String MAXIMUM_LENGTH_LONG_NAME = "maximum-length";
    private static final String MAXIMUM_LENGTH_SHORT_NAME = "max";
    private static final String MAXIMUM_LENGTH_DOC =
            "The maximum length of the read after trimming.";

    private static final String NO_TRIM_QUALITY_LONG_NAME = "no-trim-quality";
    private static final String NO_TRIM_QUALITY_SHORT_NAME = "nq";
    private static final String NO_TRIM_QUALITY_DOC = "Switch off quality trimming";

    private static final String NO_TRIM_5P_LONG_NAME = "no-5p-trim";
    private static final String NO_TRIM_5P_SHORT_NAME = "n5p";
    private static final String NO_TRIM_5P_DOC =
            "Disable 5'-trimming (quality and 'N'); May be useful for the identification of duplicates when using trimming of reads. Duplicates are usually identified by the 5' mapping position which should thus not be modified by trimming";

    // TODO: this options does not belong to here, I guess....
    private static final String KEEP_DISCARDED_LONG_NAME = "keep-discarded";
    private static final String KEEP_DISCARDED_SHORT_NAME = "k";
    private static final String KEEP_DISCARDED_DOC =
            "Keep the reads completely trimmed or that does not pass the thresholds in a discarded file (original reads stored). May be useful for quality control.";

    /**
     * Get the trimmer from the parameters
     *
     * @param single {@code true} if single-end; {@code false} otherwise
     */
    public static Trimmer getTrimmer(final CommandLine cmd, final boolean single) {
        int qualThreshold;
        try {
            String qualOpt = OptionUtils.getUniqueValue(cmd, "quality-threshold");
            qualThreshold = (qualOpt == null) ? TrimmingArgumentCollection.DEFAULT_QUALTITY_SCORE
                    : Integer.parseInt(qualOpt);
            if (qualThreshold < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new ToolNames.ToolException("Quality threshold should be a positive integer");
        }
        // minimum length
        int minLength;
        try {
            String minOpt = OptionUtils.getUniqueValue(cmd, "m");
            minLength = (minOpt == null) ? DEFAULT_MINIMUM_LENGTH : Integer.parseInt(minOpt);
            if (minLength < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new ToolNames.ToolException("Minimum length should be a positive integer");
        }
        // maximum length
        int maxLength;
        try {
            String maxOpt = OptionUtils.getUniqueValue(cmd, "max");
            maxLength = (maxOpt == null) ? Integer.MAX_VALUE : Integer.parseInt(maxOpt);
            if (maxLength < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new ToolNames.ToolException("Maximum length should be a positive integer");
        }
        boolean discardRemainingNs = cmd.hasOption("discard-internal-N");
        boolean dontTrimQuality = cmd.hasOption("no-trim-quality");
        boolean no5ptrim = cmd.hasOption("no-5p-trim");
        return new TrimmerBuilder(single)
                .setTrimQuality(!dontTrimQuality)
                .setQualityThreshold(qualThreshold)
                .setMinLength(minLength)
                .setMaxLength(maxLength)
                .setDiscardRemainingNs(discardRemainingNs)
                .setNo5pTrimming(no5ptrim)
                .build();
    }

    public static void addTrimmingArguments(final Options options) {
        Option quality_threshold = Option.builder(QUALITY_THRESHOLD_SHORT_NAME)
                .longOpt(QUALITY_THRESHOLD_LONG_NAME)
                .desc(QUALITY_THRESHOLD_DOC + " [Default=" + DEFAULT_QUALTITY_SCORE + "]")
                .hasArg().numberOfArgs(1).argName("INT")
                .optionalArg(true).build();
        Option discard_internal_N = Option.builder(DISCARD_INTERNAL_N_SHORT_NAME)
                .longOpt(DISCARD_INTERNAL_N_LONG_NAME)
                .desc(DISCARD_INTERNAL_N_DOC).hasArg(false)
                .optionalArg(true).build();
        Option min_length = Option.builder(MINIMUM_LENGTH_SHORT_NAME)
                .longOpt(MINIMUM_LENGTH_LONG_NAME)
                .desc(MINIMUM_LENGTH_DOC + " [Default=" + DEFAULT_MINIMUM_LENGTH + "]")
                .hasArg()
                .numberOfArgs(1).argName("INT").optionalArg(true).build();
        Option max_length = Option.builder(MAXIMUM_LENGTH_SHORT_NAME)
                .longOpt(MAXIMUM_LENGTH_LONG_NAME)
                .desc(MAXIMUM_LENGTH_DOC + " [Default=" + Integer.MAX_VALUE + "]")
                .hasArg()
                .numberOfArgs(1).argName("INT").optionalArg(true).build();
        Option no_trim_qual = Option.builder(NO_TRIM_QUALITY_SHORT_NAME)
                .longOpt(NO_TRIM_QUALITY_LONG_NAME)
                .desc(NO_TRIM_QUALITY_DOC)
                .hasArg(false).optionalArg(false).build();
        Option no_5p_trim = Option.builder(NO_TRIM_5P_SHORT_NAME)
                .longOpt(NO_TRIM_5P_LONG_NAME)
                .desc(NO_TRIM_5P_DOC)
                .hasArg(false).optionalArg(true).build();
        Option keep_discard = Option.builder(KEEP_DISCARDED_SHORT_NAME)
                .longOpt(KEEP_DISCARDED_LONG_NAME)
                .desc(KEEP_DISCARDED_DOC)
                .hasArg(false).optionalArg(true).build();

        options.addOption(keep_discard);
        options.addOption(no_5p_trim);
        options.addOption(no_trim_qual);
        options.addOption(min_length);
        options.addOption(max_length);
        options.addOption(discard_internal_N);
        options.addOption(quality_threshold);
    }

}
