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

import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DefaultDocWorkUnitHandler;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.HelpDoclet;

import java.util.Arrays;

/**
 * The ReadTools Documentation work unit handler class that is the companion to
 * {@link RTHelpDocWorkUnitHandler}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTHelpDocWorkUnitHandler extends DefaultDocWorkUnitHandler {

    /** Default generic template for the documentation work unit. */
    public static final String DEFAULT_TEMPLATE_PREFIX = "generic.template.";

    public RTHelpDocWorkUnitHandler(HelpDoclet doclet) {
        super(doclet);
    }

    /**
     * Returns {@link #DEFAULT_TEMPLATE_PREFIX} with the output extension
     */
    @Override
    public String getTemplateName(final DocWorkUnit workUnit) {
        return DEFAULT_TEMPLATE_PREFIX + getDoclet().getOutputFileExtension();
    }

    /** Output tags starting with {@link RTHelpConstants#PROGRAM_NAME}. */
    @Override
    protected String getTagFilterPrefix() {
        return RTHelpConstants.PROGRAM_NAME;
    }

    // TODO: this method should be removed if https://github.com/broadinstitute/barclay/pull/70 is included in Barclay
    private String getTagPrefix() {
        String customPrefix = getTagFilterPrefix();
        return customPrefix == null ? null : "@" + customPrefix + ".";

    }

    /**
     * {@inheritDoc}
     *
     * It also include custom tags that are not in-line.
     */
    @Override
    protected void addCustomBindings(final DocWorkUnit currentWorkUnit) {
        // TODO: this method should be removed if https://github.com/broadinstitute/barclay/pull/70 is included in Barclay
        final String tagFilterPrefix = getTagPrefix();
        Arrays.stream(currentWorkUnit.getClassDoc().tags())
                .filter(t -> t.name().startsWith(tagFilterPrefix))
                .forEach(t -> currentWorkUnit.setProperty(t.name().substring(tagFilterPrefix.length()), t.text()));
    }

    /**
     * Uses different descriptions depending on the work unit content. The first non-empty of:
     *
     * 1. Default method to capture the javadoc description.
     * 2. {@link CommandLineProgramProperties#summary()} for tools.
     * 3. {@link org.broadinstitute.barclay.help.DocumentedFeature} summary.
     *
     * Otherwise, the description will be empty.
     */
    @Override
    protected String getDescription(final DocWorkUnit currentWorkUnit) {
        String description =  super.getDescription(currentWorkUnit);

        // 1. Default implementation
        if (!description.isEmpty()) {
           return description;
        }

        // 2. Command line summary
        final CommandLineProgramProperties properties = currentWorkUnit.getCommandLineProperties();
        if (properties != null) {
            description = properties.summary();
        }

        // 3. Use the documented feature
        if (description.isEmpty()) {
            // the documented feature should not be null
            description = currentWorkUnit.getDocumentedFeature().summary();
        }

        return description;
    }

    /**
     * Uses the name for the unit with the Markdown suffix (<i>.md</i>).
     *
     * <p>WARNING: does not honor the output extension (should be fixed in the future).
     */
    @Override
    public String getDestinationFilename(final DocWorkUnit workUnit) {
        return workUnit.getName() + "." + getDoclet().getOutputFileExtension();
    }

    @Override
    public String getJSONFilename(final DocWorkUnit workUnit) {
        return workUnit.getName() + ".json";
    }
}
