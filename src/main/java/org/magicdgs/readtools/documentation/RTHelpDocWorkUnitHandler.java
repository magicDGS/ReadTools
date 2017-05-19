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
        // TODO: add custom tags?
        return "";
    }

    /**
     * Use the {@link CommandLineProgramProperties#summary} instead of the javadoc annotation.
     * If it is not a command line, it uses the default implementation.
     */
    @Override
    protected String getDescription(final DocWorkUnit currentWorkUnit) {
        // TODO: uncomment when there is a better description in the javadoc
        // TODO: in that case, this method will delegate into the current implementation is there is no javadoc
//        final String description = super.getDescription(currentWorkUnit);
//        if (!(description == null || description.isEmpty())) {
//            return description;
//        }
        // TODO: remove this if the previous lines are uncommented
        final CommandLineProgramProperties properties = currentWorkUnit.getCommandLineProperties();
        if (properties == null) {
            return super.getDescription(currentWorkUnit);
        }
        return properties.summary();
    }

    @Override
    public String getDestinationFilename(final DocWorkUnit workUnit) {
        // TODO: should use getDoclet().getOutputFileExtension() but requires that it is public
        return workUnit.getName() + ".md";
    }
}
