package org.magicdgs.readtools.cmd;

import org.magicdgs.readtools.Main;
import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.hellbender.exceptions.UserException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTDeprecatedToolsRegistryIntegrationTest extends RTBaseTest {

    // legacy tools included in pre-release (<= 0.3.0)
    private final List<String> legacyTools = Arrays.asList(
            "QualityChecker", "StandardizeQuality",
            "BamBarcodeDetector", "FastqBarcodeDetector",
            "TaggedBamToFastq", "TrimFastq");
    private static final String firstMajorVersion = "1.0.0";

    @DataProvider
    public Iterator<Object[]> getLegacyToolNames() {
        return legacyTools.stream().map(s -> new Object[] {s}).iterator();
    }

    @Test(dataProvider = "getLegacyToolNames")
    public void testLegacyTool(final String toolName) {
        final UserException e = Assert.expectThrows(
                UserException.class,
                () -> new Main().instanceMain(new String[] {toolName})
        );

        // should contain the first major version message
        Assert.assertTrue(e.getMessage().contains(firstMajorVersion), e.getMessage());
        Assert.assertEquals(e.getMessage(), RTDeprecatedToolsRegistry.getToolDeprecationInfo(toolName));
    }

    @Test(dataProvider = "getLegacyToolNames")
    public void testMisspelledLegacyTool(final String toolName) {
        Assert.assertNull(RTDeprecatedToolsRegistry.getToolDeprecationInfo("L" + toolName));
    }
}
