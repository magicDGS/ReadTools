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

import org.broadinstitute.barclay.argparser.CommandLineArgumentParser;
import org.broadinstitute.barclay.argparser.CommandLinePluginDescriptor;
import org.broadinstitute.barclay.argparser.CommandLinePluginProvider;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DefaultDocWorkUnitHandler;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.HelpDoclet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
     * Uses the name for the unit with the Markdonw suffix (<i>.md</i>).
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

    // hack for the bug in barclay - https://github.com/broadinstitute/barclay/issues/97
    // TODO - should be removed after it is solved
    @Override
    public void processWorkUnit(
            final DocWorkUnit workUnit,
            final List<Map<String, String>> featureMaps,
            final List<Map<String, String>> groupMaps) {

        CommandLineArgumentParser clp = null;
        List<? extends CommandLinePluginDescriptor<?>> pluginDescriptors = new ArrayList<>();

        // Not all DocumentedFeature targets are CommandLinePrograms, and thus not all can be instantiated via
        // a no-arg constructor. But we do want to generate a doc page for them. Any arguments associated with
        // such a feature will show up in the doc page for any referencing CommandLinePrograms, instead of in
        // the standalone feature page.
        //
        // Ex: We may want to document an input or output file format by adding @DocumentedFeature
        // to the format's reader/writer class (i.e. TableReader), and then reference that feature
        // in the extraDocs attribute in a CommandLineProgram that reads/writes that format.
        try {
            final Object argumentContainer = workUnit.getClazz().newInstance();
            if (argumentContainer instanceof CommandLinePluginProvider) {
                pluginDescriptors = ((CommandLinePluginProvider) argumentContainer).getPluginDescriptors();
                clp = new CommandLineArgumentParser(
                        argumentContainer, pluginDescriptors, Collections.emptySet()
                );
            } else {
                clp = new CommandLineArgumentParser(argumentContainer);
            }
        } catch (IllegalAccessException | InstantiationException e) {
            // only throw if the command line properties is set (always require a non-arg constructor)
            if (workUnit.getCommandLineProperties() != null) {
                throw new RuntimeException(e);
            }
        }

        workUnit.setProperty("groups", groupMaps);
        workUnit.setProperty("data", featureMaps);

        addHighLevelBindings(workUnit);
        addCommandLineArgumentBindings(workUnit, clp);
        addDefaultPlugins(workUnit, pluginDescriptors);
        addExtraDocsBindings(workUnit);
        addCustomBindings(workUnit);
    }

}
