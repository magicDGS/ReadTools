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

package org.magicdgs.readtools;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants for help/documentation purposes.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class RTHelpConstants {

    // cannot be instantiated
    private RTHelpConstants() {}

    /** Software's name. */
    public static final String PROGRAM_NAME = "ReadTools";

    // the ReadTools main page hosting the code
    private final static String GITHUB_URL = "https://github.com/magicDGS/ReadTools";

    /** Issue tracker page. */
    public static final String ISSUE_TRACKER = GITHUB_URL + "/issues";

    /** Documentation main page. */
    public static final String DOCUMENTATION_PAGE = "http://magicdgs.github.io/ReadTools/";

    //////////////////////////////////////////////////
    // PROGRAM GROUP NAMES AND DESCRIPTIONS FOR TOOLS

    /** Documentation name for reads manipulation. */
    public static final String DOC_CAT_READS_MANIPULATION = "Reads manipulation";
    /** Documentation description for reads manipulation. */
    public static final String DOC_CAT_READS_MANIPULATION_SUMMARY =
            "Tools for manipulating any supported read source (SAM/BAM/CRAM/FASTQ)";

    /** Documentation name for reads conversion. */
    public static final String DOC_CAT_READS_CONVERSION = "Reads conversion";
    /** Documentation description for reads conversion. */
    public static final String DOC_CAT_READS_CONVERSION_SUMMARY =
            "Tools for converting any supported read source (SAM/BAM/CRAM/FASTQ)";

    /** Documentation name for Dismtap integration. */
    public static final String DOC_CAT_DISTMAP = "Distmap integration";
    /** Documentation description for Distmap integration. */
    public static final String DOC_CAT_DISTMAP_SUMMARY =
            "Tools for integration with the DistMap (Pandey & Schlötterer 2013).";

    ///////////////////////////////
    // DOCUMENTATION FOR UTILITIES

    /** Documentation name for Trimmer 'utilities'. */
    public static final String DOC_CAT_TRIMMERS = "Trimmers";
    public static final String DOC_CAT_TRIMMERS_SUMMARY = "Algorithms used to trim the reads.";

    // map each group name to a super-category
    private static Map<String, String> groupToSuperCategory;

    // initialize on demand the mapping between supercategories and group names
    private static Map<String, String> getSuperCategoryMap() {
        if (groupToSuperCategory == null) {
            // TODO: initialize with GATK's and/or Picard's supercat map
            // TODO: https://github.com/magicDGS/ReadTools/issues/370
            // do this only on demand since we only need it during docgen
            groupToSuperCategory = new HashMap<>();

            // supercat Tools
            groupToSuperCategory.put(DOC_CAT_READS_MANIPULATION, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);
            groupToSuperCategory.put(DOC_CAT_READS_CONVERSION, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);
            groupToSuperCategory.put(DOC_CAT_DISTMAP, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);
            // include Picard's tool definitions
            groupToSuperCategory.put(picard.util.help.HelpConstants.DOC_CAT_DIAGNOSTICS_AND_QC, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);

            // supercat utilities (trimmers and filters)
            groupToSuperCategory.put(DOC_CAT_TRIMMERS, picard.util.help.HelpConstants.DOC_SUPERCAT_UTILITIES);
            groupToSuperCategory.put(org.broadinstitute.hellbender.utils.help.HelpConstants.DOC_CAT_READFILTERS, picard.util.help.HelpConstants.DOC_SUPERCAT_UTILITIES);
        }
        return groupToSuperCategory;
    }

    /** Supercategory not defined in the map. */
    private static final String DOC_SUPERCAT_OTHER = "other";

    /**
     * Returns the super-category for the group name; if not defined, the supercategory is {@link
     * #DOC_SUPERCAT_OTHER}
     */
    public static String getSuperCategoryProperty(final String groupName) {
        return getSuperCategoryMap().getOrDefault(groupName, DOC_SUPERCAT_OTHER);
    }
}
