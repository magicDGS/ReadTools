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

import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.cmdline.TestProgramGroup;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTHelpDocWorkUnitHandlerUnitTest extends RTBaseTest {

    private static final RTHelpDocWorkUnitHandler HANDLER =
            new RTHelpDocWorkUnitHandler(RTHelpDocletUnitTest.DOCLET);

    @Test
    public void testTemplateName() {
        final File template = new File(RTHelpDocletUnitTest.DOCUMENTATION_TEMPLATES_FOLDER,
                HANDLER.getTemplateName(null));
        // check that our index template exists in the resources folder
        Assert.assertTrue(template.exists(),
                "index template does not exists: " + template);
    }

    @Test
    public void testGetTagFilterPrefix() {
        Assert.assertEquals(HANDLER.getTagFilterPrefix(), "");
    }

    @Test
    public void testGetDestinationFilename() {
        final DocWorkUnit mockedWorkUnit = Mockito.mock(DocWorkUnit.class);
        Mockito.when(mockedWorkUnit.getName()).thenReturn("MyToolName");
        Assert.assertEquals(HANDLER.getDestinationFilename(mockedWorkUnit), "MyToolName.md");
    }

    @Test
    public void testGetDescriptionFromCommandLine() {
        @CommandLineProgramProperties(summary = "Summary for CLP", oneLineSummary = "One line summary for CLP", programGroup = TestProgramGroup.class)
        @DocumentedFeature(summary = "Summary for DocumentedFeature")
        final class CLPClass {}
        final DocWorkUnit mockedWorkUnit = mockedWorkUnit(CLPClass.class);
        Assert.assertEquals(HANDLER.getDescription(mockedWorkUnit), "Summary for CLP");
    }

    @Test
    public void testGetDescriptionFromDocumentedFeature() {
        @DocumentedFeature(summary = "Summary for DocumentedFeature")
        final class DocumentedClass {}
        final DocWorkUnit mockedWorkUnit = mockedWorkUnit(DocumentedClass.class);
        Assert.assertEquals(HANDLER.getDescription(mockedWorkUnit), "Summary for DocumentedFeature");
    }

    private static final DocWorkUnit mockedWorkUnit(final Class<?> clazz) {
        final DocWorkUnit mockedWorkUnit = Mockito.mock(DocWorkUnit.class);
        Mockito.when(mockedWorkUnit.getCommandLineProperties())
                .thenReturn(clazz.getAnnotation(CommandLineProgramProperties.class));
        Mockito.when(mockedWorkUnit.getDocumentedFeature())
                .thenReturn(clazz.getAnnotation(DocumentedFeature.class));
        return mockedWorkUnit;
    }

}