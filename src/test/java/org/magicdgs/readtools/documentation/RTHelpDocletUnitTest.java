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
    public void testGetIndexTemplateName() {
        final File indexTemplate = new File(DOCUMENTATION_TEMPLATES_FOLDER,
                DOCLET.getIndexTemplateName());
        // check that our index template exists in the resources folder
        Assert.assertTrue(indexTemplate.exists(),
                "index template does not exists: " + indexTemplate);
    }

    @Test
    public void testcreateGSONWorkUnit() {
        // should not fail by using all null, because we have no extra information yet
        final GSONWorkUnit gsonWorkUnit = DOCLET.createGSONWorkUnit(null, null, null);
        Assert.assertEquals(gsonWorkUnit.getClass(), RTGSONWorkUnit.class);
    }

    @Test
    public void testGetGroupMap() {
        final DocWorkUnit mockedUnit = Mockito.mock(DocWorkUnit.class);
        Mockito.when(mockedUnit.getGroupName()).thenReturn("group_name");
        Mockito.when(mockedUnit.getGroupSummary()).thenReturn("group_summary");
        final Map<String, String> groupMap = DOCLET.getGroupMap(mockedUnit);
        Assert.assertEquals(groupMap.get("id"), "group_name");
        Assert.assertEquals(groupMap.get("name"), "group_name");
        Assert.assertEquals(groupMap.get("summary"), "group_summary");
        Assert.assertEquals(groupMap.get("supercat"), "other");
    }
}