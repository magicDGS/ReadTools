package org.magicdgs.readtools.tools.mappability.gem;

import org.apache.commons.lang3.Range;
import org.broadinstitute.barclay.argparser.CommandLineParser;

import java.text.DecimalFormat;
import java.util.function.Function;

/**
 * Methods to get a score per-position for GEM-mappability.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: add other methods (https://github.com/magicDGS/ReadTools/issues/494)
public enum GemScoreMethod implements CommandLineParser.ClpEnum {
    /** Minimum of the range of mappings. */
    MIN(Range::getMinimum, null,
            "Minimum of the range"),
    /** Maximum of the range of mappings. */
    MAX(Range::getMaximum, null,
            "Maximum of the range"),
    /** Middle point of the range of mappings. */
    MID(r -> r.getMinimum() + (r.getMaximum() - r.getMinimum()) / 2d, new DecimalFormat("##.##"),
            "Middle point of the range");

    // function to convert to numeric score
    private final Function<Range<Long>, Number> func;
    // decimal format for values expected to have some precision; null otherwise
    private final DecimalFormat format;

    private final String description;

    GemScoreMethod(final Function<Range<Long>, Number> func, final DecimalFormat format,
            final String description) {
        this.func = func;
        this.format = format;
        this.description = description;
    }

    /**
     * Converts a range of mappability values into a numeric score.
     *
     * @param range mappability range to convert.
     *
     * @return numeric score value.
     */
    public Number score(final Range<Long> range) {
        return func.apply(range);
    }

    /**
     * Converts a range of mappability values into a formatted string.
     *
     * @param range mappability range to convert.
     * @return formatted score value.
     */
    public String formatScore(final Range<Long> range) {
        final Number n = func.apply(range);
        return (format != null) ? format.format(n) : n.toString();
    }

    @Override
    public String getHelpDoc() {
        return description;
    }
}
