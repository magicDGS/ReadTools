package org.magicdgs.readtools.cmd;

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.tools.barcodes.AssignReadGroupByBarcode;
import org.magicdgs.readtools.tools.conversion.ReadsToFastq;
import org.magicdgs.readtools.tools.conversion.StandardizeReads;
import org.magicdgs.readtools.tools.quality.QualityEncodingDetector;
import org.magicdgs.readtools.tools.trimming.TrimReads;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Deprecated tool registry to inform for removal of tools in following versions.
 *
 * <p>Note: based on GATK's {@link org.broadinstitute.hellbender.cmdline.DeprecatedToolsRegistry}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class RTDeprecatedToolsRegistry {

    private RTDeprecatedToolsRegistry() {}

    // Mapping from tool name to string describing the major version number where the tool first disappeared and
    // optional recommended alternatives
    private static Map<String, Pair<String, String>> deprecatedTools = null;

    private static final String LEGACY_PREFIX = "Pre-release tool substituted by ";

    private static Map<String, Pair<String, String>> getDeprecatedTools() {
        if (deprecatedTools == null) {
            deprecatedTools = new HashMap<>();
            // legacy tools
            deprecatedTools.put("QualityChecker", Pair.of("1.0.0", LEGACY_PREFIX + QualityEncodingDetector.class.getSimpleName()));
            deprecatedTools.put("StandardizeQuality", Pair.of("1.0.0", LEGACY_PREFIX + StandardizeReads.class.getSimpleName()));
            deprecatedTools.put("BamBarcodeDetector", Pair.of("1.0.0", LEGACY_PREFIX + AssignReadGroupByBarcode.class.getSimpleName()));
            deprecatedTools.put("FastqBarcodeDetector", Pair.of("1.0.0", LEGACY_PREFIX + AssignReadGroupByBarcode.class.getSimpleName()));
            deprecatedTools.put("TaggedBamToFastq", Pair.of("1.0.0", LEGACY_PREFIX + ReadsToFastq.class.getSimpleName()));
            deprecatedTools.put("TrimFastq", Pair.of("1.0.0", LEGACY_PREFIX + TrimReads.class.getSimpleName()));

        }
        return deprecatedTools;
    }

    /**
     * Utility method to pull up the version number at which a tool was deprecated and the suggested replacement, if any
     *
     * @param toolName   the tool class name (not the full package) to check
     */
    public static String getToolDeprecationInfo(final String toolName) {
        final Pair<String, String> info = getDeprecatedTools().get(toolName);
        return info != null ?
                String.format("%s is no longer included in %s as of version %s. %s",
                        toolName,
                        RTHelpConstants.PROGRAM_NAME,
                        info.getLeft(),
                        info.getRight()
                ) :
                null;
    }
}
