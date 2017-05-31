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

import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DefaultDocWorkUnitHandler;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.barclay.help.HelpDoclet;

/**
 * The ReadTools Documentation work unit handler class that is the companion to
 * {@link RTHelpDocWorkUnitHandler}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTHelpDocWorkUnitHandler extends DefaultDocWorkUnitHandler {

    /** Default generic template for the documentation work unit. */
    public static final String DEFAULT_TEMPLATE_NAME = "generic.template.md";

    public RTHelpDocWorkUnitHandler(HelpDoclet doclet) {
        super(doclet);
    }

    /** Returns {@link #DEFAULT_TEMPLATE_NAME}. */
    @Override
    public String getTemplateName(final DocWorkUnit workUnit) {
        return DEFAULT_TEMPLATE_NAME;
    }

    /** No custom tags are output in ReadToools. */
    @Override
    protected String getTagFilterPrefix() {
        // TODO: add custom tags (https://github.com/magicDGS/ReadTools/issues/242)
        return "";
    }

    /**
     * Uses different descriptions depending on the work unit content:
     *
     * - If the unit is for a command line program, uses {@link CommandLineProgramProperties#summary()}.
     * - Gets the summary for the documented feature if available.
     * - Otherwise, use the default method.
     */
    // TODO: we need a different approach for this (see https://github.com/magicDGS/ReadTools/issues/241)
    // TODO: we should use the default (super method) and if it is nul or empty delegates in
    // TODO: currentWorkUnit.getCommandLineProperties().summary() - maybe this should be discouraged
    @Override
    protected String getDescription(final DocWorkUnit currentWorkUnit) {
        String description = "";
        final CommandLineProgramProperties properties = currentWorkUnit.getCommandLineProperties();

        // 1. Command line summary
        if (properties != null) {
            description = properties.summary();
        }

        // 2. Use the documented feature
        if (description.isEmpty()) {
            // the documented feature should not be null
            description = currentWorkUnit.getDocumentedFeature().summary();
        }

        // 3. Default implementation
        if (description.isEmpty()) {
            description = super.getDescription(currentWorkUnit);
        }

        return description;
    }

    /**
     * Uses the name for the unit with the Markdonw suffix (<i>.md</i>).
     *
     * <p>WARNING: does not honor the output extension (should be fixed in the future).
     */
    @Override
    public String getDestinationFilename(final DocWorkUnit workUnit) {
        // TODO: should use getDoclet().getOutputFileExtension() but requires https://github.com/broadinstitute/barclay/pull/60
        // TODO: see https://github.com/magicDGS/ReadTools/issues/240
        return workUnit.getName() + ".md";
    }
}
