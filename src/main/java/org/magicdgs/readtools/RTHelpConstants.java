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

import org.magicdgs.readtools.utils.read.RTReadUtils;

import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Constants and utility methods for help/documentation purposes.
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

    /** Documentation name for Mapped reads. */
    public static final String DOC_CAT_MAPPED = "Mapped reads";
    /** Documentation description for Mapped reads. */
    public static final String DOC_CAT_MAPPED_SUMMARY =
            "Tools operating on already mapped reads (SAM/BAM/CRAM only)";

    /** Documentation name for Mappability tools */
    public static final String DOC_CAT_MAPPABILITY = "Mappabilty";
    /** Documentation description for Mappability tools. */
    public static final String DOC_CAT_MAPPABILITY_SUMMARY =
            "Tools related with mappability on the genome";

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
            // do this only on demand since we only need it during docgen
            groupToSuperCategory = new HashMap<>();

            // supercat Tools
            groupToSuperCategory.put(DOC_CAT_READS_MANIPULATION, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);
            groupToSuperCategory.put(DOC_CAT_READS_CONVERSION, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);
            groupToSuperCategory.put(DOC_CAT_DISTMAP, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);
            groupToSuperCategory.put(DOC_CAT_MAPPED, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);
            groupToSuperCategory.put(DOC_CAT_MAPPABILITY, picard.util.help.HelpConstants.DOC_SUPERCAT_TOOLS);
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

    /**
     * Output versions of important dependencies to the logger.
     *
     * <p>This methods <b>MUST</b> be used as following in every tool extending directly
     * {@link CommandLineProgram} or sub-classes not present in ReadTools:
     *
     * <ul>
     *     <li>Override the {@link CommandLineProgram#printLibraryVersions()}</li>
     *     <li>Call this method</li>
     *     <li><b>SHOULD NOT</b> call the super method</li>
     * </ul>
     */
    public static void printLibraryVersions(final Class<? extends CommandLineProgram> callerClazz, final Logger logger) {
        // print versions from the MANIFEST
        String htsjdkVersion = null;
        String gatkVersion = null;
        try {
            final String classPath = callerClazz.getResource(callerClazz.getSimpleName() + ".class").toString();
            if (classPath.startsWith("jar")) {
                final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
                try ( final InputStream manifestStream = new URL(manifestPath).openStream() ) {
                    final Attributes manifestAttributes = new Manifest(manifestStream).getMainAttributes();
                    htsjdkVersion = manifestAttributes.getValue("htsjdk-Version");
                    gatkVersion = manifestAttributes.getValue("GATK-Version");

                }
            }
        } catch (final IOException ignored) {
            // intentionally ignored
        }
        // log the versions
        logger.info("HTSJDK Version: " + (htsjdkVersion != null ? htsjdkVersion : "unknown"));
        logger.info("GATK Version: " + (gatkVersion != null ? gatkVersion : "unknown"));
        // log that we are using a patched version of GATK
        // TODO: remove once https://github.com/magicDGS/ReadTools/issues/443 is fixed
        logger.info("Using GATK patch from https://github.com/bioinformagik/gatk");
    }

    /**
     * Output a curated set of important settings to the logger.
     *
     * <p>This methods <b>MUST</b> be used as following in every tool extending directly
     * {@link CommandLineProgram} or sub-classes not present in ReadTools:
     *
     * <ul>
     *     <li>Override the {@link CommandLineProgram#printSettings()}</li>
     *     <li>Implementation <b>SHOULD</b> call first the super method</li>
     *     <li>Then call this method</li>
     * </ul>
     */
    public static void printSettings(final Logger logger) {
        logger.info("Barcode sequence ({}) separator: '{}'",
                () -> RTReadUtils.RAW_BARCODE_TAG,
                () -> RTDefaults.BARCODE_INDEX_DELIMITER);
        logger.debug("Barcode sequence split pattern: '{}'",
                RTReadUtils.DEFAULT_BARCODE_INDEX_SPLIT);
        logger.info("Barcode quality ({}) separator: '{}'",
                () -> RTReadUtils.RAW_BARCODE_QUALITY_TAG,
                () -> RTDefaults.BARCODE_QUALITY_DELIMITER);
        logger.debug("Barcode quality split pattern: '{}'",
                RTReadUtils.DEFAULT_BARCODE_QUALITY_SPLIT);
        logger.info("Number of records to detect quality: {}",
                () -> RTDefaults.MAX_RECORDS_FOR_QUALITY);
        // for debugging
        logger.debug("sampling_quality_checking_frequency : {}",
                () -> RTDefaults.SAMPLING_QUALITY_CHECKING_FREQUENCY);
        logger.debug("force_overwrite : {}",
                () -> RTDefaults.FORCE_OVERWRITE);
        logger.debug("discarded_output_suffix : {}",
                () -> RTDefaults.DISCARDED_OUTPUT_SUFFIX);
    }

    /**
     * Gets the String containing information about hot to get support.
     *
     * <p>This methods <b>MUST</b> be used as following in every tool extending directly
     * {@link CommandLineProgram} or sub-classes not present in ReadTools:
     *
     * <ul>
     *     <li>Override the {@link CommandLineProgram#getSupportInformation()}</li>
     *     <li>Call this method</li>
     *     <li><b>SHOULD NOT</b> call the super method</li>
     * </ul>
     *
     * @return ReadTools specific support information.
     */
    public static String getSupportInformation() {
        return "For support and documentation go to " + DOCUMENTATION_PAGE;
    }
}
