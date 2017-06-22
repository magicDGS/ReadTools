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

import org.magicdgs.readtools.RTHelpConstants;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.barclay.help.GSONWorkUnit;
import org.broadinstitute.barclay.help.HelpDoclet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Custom Barclay-based Javadoc Doclet used for generating ReadTools help/documentation.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTHelpDoclet extends HelpDoclet {

    /** Markdown extension is our default. */
    public static final String MARKDOWN_OUTPUT_FILE_EXTENSION = "md";

    /** YML is our default index extension. */
    public static final String YML_INDEX_FILE_EXTENSION = "yml";

    /** Generic index template name for ReadTools documentation. */
    public static final String INDEX_TEMPLATE_PREFIX = "generic.index.template.";

    /** Constructor with our {@link #MARKDOWN_OUTPUT_FILE_EXTENSION}. */
    public RTHelpDoclet() {
        super();
        // default extension is Markdown for features and yml for index
        outputFileExtension = MARKDOWN_OUTPUT_FILE_EXTENSION;
        indexFileExtension = YML_INDEX_FILE_EXTENSION;
    }

    /**
     * Create a doclet of the appropriate type and generate the FreeMarker templates properties.
     */
    public static boolean start(final RootDoc rootDoc) throws IOException {
        return new org.magicdgs.readtools.documentation.RTHelpDoclet().startProcessDocs(rootDoc);
    }

    ///////////////////////////////////////////////
    // Tag names
    private static final String NOTE_MAP_ENTRY = "note";
    private static final String WARNING_MAP_ENTRY = "warning";


    /**
     * Returns {@link #INDEX_TEMPLATE_PREFIX} with the index file extension.
     *
     * <p>Note: it does not honor the index file extension option.
     */
    @Override
    public String getIndexTemplateName() {
        return INDEX_TEMPLATE_PREFIX + getIndexFileExtension();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Note: uses the {@link RTHelpDocWorkUnitHandler}.
     */
    @Override
    protected DocWorkUnit createWorkUnit(
            final DocumentedFeature documentedFeature,
            final ClassDoc classDoc,
            final Class<?> clazz) {
        return new DocWorkUnit(
                new RTHelpDocWorkUnitHandler(this),
                documentedFeature,
                classDoc,
                clazz);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Note: uses the {@link RTGSONWorkUnit}, adding custom tags.
     */
    @Override
    protected GSONWorkUnit createGSONWorkUnit(
            final DocWorkUnit workUnit,
            final List<Map<String, String>> groupMaps,
            final List<Map<String, String>> featureMaps) {
        final RTGSONWorkUnit gsonWorkUnit = new RTGSONWorkUnit();
        // set the note and the warning from javadocs
        gsonWorkUnit.setNote((String)workUnit.getProperty(NOTE_MAP_ENTRY));
        gsonWorkUnit.setWarning((String)workUnit.getProperty(WARNING_MAP_ENTRY));
        return gsonWorkUnit;
    }

    /**
     * Adds a 'supercat' entry to the map using the {@link RTHelpConstants} for the work unit group
     * name.
     */
    @Override
    protected final Map<String, String> getGroupMap(final DocWorkUnit docWorkUnit) {
        final Map<String, String> root = super.getGroupMap(docWorkUnit);
        root.put("supercat", RTHelpConstants.getSuperCategoryProperty(docWorkUnit.getGroupName()));
        return root;
    }
}
