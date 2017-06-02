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

package org.magicdgs.readtools.documentation;

import org.magicdgs.readtools.RTBaseTest;
import org.magicdgs.readtools.TestResourcesUtils;

import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.GSONWorkUnit;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTHelpDocletUnitTest extends RTBaseTest {

    /** Folder where templates live. */
    protected static final String DOCUMENTATION_TEMPLATES_FOLDER =
            TestResourcesUtils.READTOOLS_MAIN_RESOURCES_DIRECTORY
                    + "org/magicdgs/readtools/documentation";

    protected static final RTHelpDoclet DOCLET = new RTHelpDoclet();

    @Test
    public void testGetIndexTemplateName() throws Exception {
        final File indexTemplate = new File(DOCUMENTATION_TEMPLATES_FOLDER,
                DOCLET.getIndexTemplateName());
        // check that our index template exists in the resources folder
        Assert.assertTrue(indexTemplate.exists(),
                "index template does not exists: " + indexTemplate);
    }

    @Test
    public void testcreateGSONWorkUnit() throws Exception {
        // should not fail by using all null, because we have no extra information yet
        final GSONWorkUnit gsonWorkUnit = DOCLET.createGSONWorkUnit(null, null, null);
        Assert.assertEquals(gsonWorkUnit.getClass(), RTGSONWorkUnit.class);
    }

    @Test
    public void testGetGroupMap() throws Exception {
        final DocWorkUnit mockedUnit = Mockito.mock(DocWorkUnit.class);
        Mockito.when(mockedUnit.getGroupName()).thenReturn("group_name");
        Mockito.when(mockedUnit.getGroupSummary()).thenReturn("group_summary");
        final Map<String, String> groupMap = DOCLET.getGroupMap(mockedUnit);
        Assert.assertEquals(groupMap.get("id"), "group_name");
        Assert.assertEquals(groupMap.get("name"), "group_name");
        Assert.assertEquals(groupMap.get("summary"), "group_summary");
        Assert.assertEquals(groupMap.get("supercat"), "other");
    }

    @Test
    public void testTrimmersDocGenDontBlowUp() throws Exception {
        // run javadoc with our custom doclet to check if everything is working
        // only test if the output folder is not empty
        final File outputDir = createTestTempDir("DocGenTest");
        final List<String> docArgList = Arrays.asList(
                "-build-timestamp", "2016/01/01 01:01:01",      // dummy, constant timestamp
                "-absolute-version", "11.1",                    // dummy version
                "-output-file-extension", "md",                 // testing markdown output
                // TODO: undocument after https://github.com/magicDGS/ReadTools/issues/243
                // "-index-file-extension", "yml",
                "-docletpath", "build/libs",
                "-settings-dir", DOCUMENTATION_TEMPLATES_FOLDER,
                "-d", outputDir.getAbsolutePath(),
                "-doclet", DOCLET.getClass().getName(),
                "-sourcepath", "src/main/java",
                // check if the trimmers do not fail documentation
                "org.magicdgs.readtools.utils.read.transformer.trimming",
                "-verbose",
                "-cp", System.getProperty("java.class.path")
        );
        com.sun.tools.javadoc.Main.execute(docArgList.toArray(new String[]{}));
        final List<String> generatedFiles = Arrays.asList(outputDir.list());
        // there are at least 1 trimmers is implemented (md + json)
        Assert.assertTrue(generatedFiles.size() > 2, "Trimmers and index should be present: " + generatedFiles);
    }
}