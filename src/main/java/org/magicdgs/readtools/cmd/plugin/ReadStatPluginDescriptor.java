/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.cmd.plugin;

import org.magicdgs.readtools.utils.read.stats.PairEndReadStatFunction;
import org.magicdgs.readtools.utils.read.stats.SingleReadStatFunction;
import org.magicdgs.readtools.utils.read.stats.StatFunction;

import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLinePluginDescriptor;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: it will be better to implement once the barclay version is the latest (stable API)!
public class ReadStatPluginDescriptor extends CommandLinePluginDescriptor<StatFunction<Object, GATKRead>> {

    // TODO: if in sub-packaged, would it recognize?
    private static final List<String> pluginPackages = Arrays.asList(
            "org.magicdgs.readtools.utils.read.stats.pairstat",
            "org.magicdgs.readtools.utils.read.stats.singlestat"
    );
    private static final Class<?> pluginBaseClass = org.magicdgs.readtools.utils.read.stats.StatFunction.class;

    @Override
    public Class<?> getPluginClass() {
        return pluginBaseClass;
    }

    @Override
    public List<String> getPackageNames() {
        return pluginPackages;
    }

    @Override
    public Object getInstance(Class<?> pluggableClass) throws IllegalAccessException, InstantiationException {
        // TODO: store in a ordered list
        return pluggableClass.newInstance();
    }

    @Override
    public Set<String> getAllowedValuesForDescriptorArgument(final String longArgName) {
        // TODO: implement
        return null;
    }

    @Override
    public boolean isDependentArgumentAllowed(final Class<?> dependentClass) {
        // TODO: implement
        return false;
    }

    @Override
    public void validateArguments() throws CommandLineException {
        // TODO: implement
    }

    @Override
    public List<Object> getDefaultInstances() {
        // TODO: implement
        return null;
    }

    @Override
    public List<StatFunction<Object, GATKRead>> getAllInstances() {
        // TODO: implement
        return null;
    }

    @Override
    public Class<?> getClassForInstance(String pluginName) {
        // TODO: implement
        return null;
    }

    /*
    public WindowStatsEngine getEngine(final List<SimpleInterval> intervals,
            final String output, final boolean printAll)  {
        try {
            return new WindowStatsEngine(intervals,
                    getSingleReadInstances(),
                    getPairEndInstances(),
                    IOUtils.getPath(output),
                    printAll);
        } catch (final IOException e) {
            throw new UserException.CouldNotCreateOutputFile(output, e.getMessage(), e);
        }
    }
    */

    private List<SingleReadStatFunction> getSingleReadInstances() {
        // TODO: implement
        return Collections.emptyList();
    }

    private List<PairEndReadStatFunction> getPairEndInstances() {
        // TODO: implement
        return Collections.emptyList();
    }
}
