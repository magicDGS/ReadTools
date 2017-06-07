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
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
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
        // using a mocked unit to check if it is correct
        final DocWorkUnit workUnitMock = Mockito.mock(DocWorkUnit.class);
        Mockito.when(workUnitMock.getRootMap()).thenReturn(Collections.emptyMap());
        final GSONWorkUnit gsonWorkUnit = DOCLET.createGSONWorkUnit(workUnitMock, null, null);
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
    public void testCustomDoclet() throws Exception {
        // run javadoc with our custom doclet to check if everything is working
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
                // check the documentation of test classes
                "-sourcepath", "src/test/java", "org.magicdgs.readtools.documentation.classes",
                // uncomment next line to debug
                // "-verbose",
                "-cp", System.getProperty("java.class.path")
        );

        com.sun.tools.javadoc.Main.execute(docArgList.toArray(new String[] {}));

        // check all the generated files and assess that they are not breaking the behaviour
        final File expectedDir = getClassTestDirectory();
        for (final String fileName : expectedDir.list()) {
            final File expectedFile = new File(expectedDir, fileName);
            final File generatedFile = new File(outputDir, fileName);
            IntegrationTestSpec.assertEqualTextFiles(generatedFile, expectedFile);
        }
    }
}