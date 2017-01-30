/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.plugin.TrimmerPluginDescriptor;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;
import org.magicdgs.readtools.utils.trimming.TrimAndFilterPipeline;

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLinePluginDescriptor;
import org.broadinstitute.hellbender.cmdline.GATKPlugin.GATKReadFilterPluginDescriptor;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Argument collection for trimming algorithms. Should be used with
 * {@link CommandLinePluginDescriptor} for {@link TrimmingFunction} and {@link ReadFilter}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingPipelineArgumentCollection implements Serializable {
    private static final long serialVersionUID = 1L;

    @Argument(fullName = RTStandardArguments.DISABLE_5P_TRIMING_LONG_NAME, shortName = RTStandardArguments.DISABLE_5P_TRIMING_SHORT_NAME, doc = "Disable 5'-trimming. May be useful for downstream mark of duplicate reads, usually identified by the 5' mapping position.", mutex = {
            RTStandardArguments.DISABLE_3P_TRIMING_LONG_NAME}, optional = true)
    public boolean disable5pTrim = false;

    @Argument(fullName = RTStandardArguments.DISABLE_3P_TRIMING_LONG_NAME, shortName = RTStandardArguments.DISABLE_3P_TRIMING_SHORT_NAME, doc = "Disable 3'-trimming.", mutex = {
            RTStandardArguments.DISABLE_5P_TRIMING_LONG_NAME}, optional = true)
    public boolean disable3pTrim = false;

    /**
     * Gets a trimming/filtering pipeline from the arguments and the plugin descriptors.
     *
     * The list of trimmers/filters to apply is constructed first with the default ones and then
     * with the user provided, in order.
     *
     * @param trimmingPlugin plugin to get the trimmer(s) from.
     * @param filterPlugin   plugin to get the read filter(s) from.
     *
     * @return a trimming/filtering pipeline.
     *
     * @throws CommandLineException.BadArgumentValue if no trimmer and filter instances are
     *                                               specified.
     */
    // TODO: change the signature once the new version of Barclay includes the method to get defaults
    public TrimAndFilterPipeline getPipeline(
            final TrimmerPluginDescriptor trimmingPlugin,
            final GATKReadFilterPluginDescriptor filterPlugin) {

        // add the default and afterwards the ones provided by the user
        final List<TrimmingFunction> trimmers =
                new ArrayList<>(trimmingPlugin.getDefaultInstances());
        trimmers.addAll(trimmingPlugin.getAllInstances());

        // the same for filters
        final List<ReadFilter> filters = new ArrayList<>(filterPlugin.getDefaultInstances());
        filters.addAll(filterPlugin.getAllInstances());

        // throw if not pipeline is specified
        if (trimmers.isEmpty() && filters.isEmpty()) {
            throw new CommandLineException.BadArgumentValue(String
                    .format("No trimmmer (--%s) nor filter (--%s) for pipeline was specified.",
                            trimmingPlugin.getDisplayName(), filterPlugin.getDisplayName()));
        }

        // returns the new pipeline
        return new TrimAndFilterPipeline(trimmers, disable5pTrim, disable3pTrim, filters);
    }

}
