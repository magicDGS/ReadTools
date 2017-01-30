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

import org.magicdgs.readtools.cmd.plugin.TrimmerPluginDescriptor;
import org.magicdgs.readtools.utils.read.transformer.trimming.CutReadTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrailingNtrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;
import org.magicdgs.readtools.utils.tests.BaseTest;
import org.magicdgs.readtools.utils.trimming.TrimAndFilterPipeline;

import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;
import org.broadinstitute.hellbender.cmdline.GATKPlugin.GATKReadFilterPluginDescriptor;
import org.broadinstitute.hellbender.cmdline.TestProgramGroup;
import org.broadinstitute.hellbender.engine.filters.PlatformReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingPipelineArgumentCollectionUnitTest extends BaseTest {

    @DataProvider(name = "trimmersAndFilters")
    public Object[][] getTrimmersAndFilter() throws Exception {
        final TrimmingFunction tNt = new TrailingNtrimmer();
        final TrimmingFunction crt = new CutReadTrimmer(1, 1);
        return new Object[][] {
                {Collections.singletonList(tNt), Collections.emptyList()},
                {Collections.singletonList(tNt),
                        Collections.singletonList(new PlatformReadFilter())},
                {Arrays.asList(tNt, crt), Collections.emptyList()},
                {Arrays.asList(tNt, crt), Collections.singletonList(new PlatformReadFilter())},
        };
    }

    @Test(dataProvider = "trimmersAndFilters")
    public void testGetPipeline(final List<TrimmingFunction> defaultTrimmers,
            final List<ReadFilter> userFilters) throws Exception {
        // set up the GATKReadFilterPluginDescriptor -> defaults null because they does not matter
        final GATKReadFilterPluginDescriptor filterDescriptor =
                new GATKReadFilterPluginDescriptor(null);
        // this is like parsing the arguments with Barclay
        userFilters.stream().map(ReadFilter::getClass).forEach(rf -> {
            filterDescriptor.userReadFilterNames.add(rf.getSimpleName());
            try {
                filterDescriptor.getInstance(rf);
            } catch (IllegalAccessException | InstantiationException e) {
                Assert.fail(e.getMessage());
            }
        });

        // get the trimming pipeline arguments
        final TrimmingPipelineArgumentCollection args = new TrimmingPipelineArgumentCollection();
        final TrimAndFilterPipeline pipeline = args.getPipeline(
                new TrimmerPluginDescriptor(defaultTrimmers), filterDescriptor);

        // check that the pipeline contains the same number of trimmers/filters
        Assert.assertEquals(pipeline.getTrimmingStats().size(), defaultTrimmers.size());
        Assert.assertEquals(pipeline.getFilterStats().size(), userFilters.size());
    }

    @Test(dataProvider = "trimmersAndFilters")
    public void testGetPipelineIncompatibleWithGATKReadFilterPluginDescriptor(
            final List<TrimmingFunction> defaultTrimmers,
            final List<ReadFilter> defaultFilters) throws Exception {

        // get the trimming pipeline arguments
        final TrimmingPipelineArgumentCollection args = new TrimmingPipelineArgumentCollection();
        final TrimAndFilterPipeline pipeline = args.getPipeline(
                new TrimmerPluginDescriptor(defaultTrimmers),
                new GATKReadFilterPluginDescriptor(defaultFilters));

        // check that the pipeline contains the same number of trimmers/filters
        Assert.assertEquals(pipeline.getTrimmingStats().size(), defaultTrimmers.size());
        Assert.assertEquals(pipeline.getFilterStats().size(), defaultFilters.size());
    }


    @Test(expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testGetEmptyPipelineBlowsUp() throws Exception {
        final TrimmingPipelineArgumentCollection args = new TrimmingPipelineArgumentCollection();
        // empty argument collection
        args.getPipeline(
                new TrimmerPluginDescriptor(null),
                new GATKReadFilterPluginDescriptor(null));
    }

    @CommandLineProgramProperties(summary = "Tool with trimming pipeline arguments", oneLineSummary = "Tool with trimming pipeline arguments", programGroup = TestProgramGroup.class)
    private final static class ToolWithTrimmingPipelineArgs extends CommandLineProgram {

        @ArgumentCollection
        public TrimmingPipelineArgumentCollection args = new TrimmingPipelineArgumentCollection();

        @Override
        protected Object doWork() {
            return "parsed";
        }
    }

    @DataProvider(name = "mutexArgs")
    public Object[][] getMutexArgs() {
        return new Object[][] {
                {false, false},
                {true, false},
                {false, true}
        };
    }

    // TODO: it would be nice to allow that both parameters are supplied but are complementary
    // TODO: this require that they are not mutex, but handled differently
    // TODO: see also https://github.com/broadinstitute/barclay/issues/26
    @Test(dataProvider = "mutexArgs", enabled = false)
    public void testMutexArgsParsing(final boolean disable5pTrim, final boolean disable3pTrim)
            throws Exception {
        final CommandLineProgram clp = new ToolWithTrimmingPipelineArgs();
        final Object result = clp.instanceMain(new ArgumentsBuilder()
                .addBooleanArgument("disable5pTrim", disable5pTrim)
                .addBooleanArgument("disable3pTrim", disable3pTrim)
                .getArgsArray());
        Assert.assertEquals(result, "parsed");
    }

    @Test(expectedExceptions = CommandLineException.class)
    public void testWrongMutexArgs()
            throws Exception {
        final CommandLineProgram clp = new ToolWithTrimmingPipelineArgs();
        clp.instanceMain(new ArgumentsBuilder()
                .addBooleanArgument("disable5pTrim", true)
                .addBooleanArgument("disable3pTrim", true)
                .getArgsArray());
    }
}