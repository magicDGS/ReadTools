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

package org.magicdgs.readtools.tools.trimming;

import org.magicdgs.readtools.RTBaseTest;
import org.magicdgs.readtools.cmd.plugin.TrimmerPluginDescriptor;
import org.magicdgs.readtools.utils.read.transformer.trimming.CutReadTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrailingNtrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;
import org.magicdgs.readtools.utils.trimming.TrimAndFilterPipeline;

import org.broadinstitute.barclay.argparser.CommandLineArgumentParser;
import org.broadinstitute.barclay.argparser.CommandLineParser;
import org.broadinstitute.hellbender.cmdline.GATKPlugin.GATKReadFilterPluginDescriptor;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimReadsTrimmerPluginArgumentCollectionIntegrationTest extends RTBaseTest {

    @DataProvider(name = "pipelineArguments")
    public Object[][] getPipelineArguments() throws Exception {
        // this is always the first read filter
        final String completelyTrimName = "CompletelyTrimReadFilter";
        return new Object[][] {
                // without arguments
                {new ArgumentsBuilder(),
                        Collections.singletonList(new TrailingNtrimmer()),
                        Collections.emptyList(),
                        Collections.singletonList("TrailingNtrimmer"),
                        Collections.singletonList(completelyTrimName)},
                {new ArgumentsBuilder(),
                        Collections.singletonList(new TrailingNtrimmer()),
                        Collections.singletonList(ReadFilterLibrary.MAPPED),
                        Collections.singletonList("TrailingNtrimmer"),
                        Arrays.asList(completelyTrimName, "MappedReadFilter")},
                {new ArgumentsBuilder(),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Collections.emptyList(),
                        Arrays.asList("TrailingNtrimmer", "CutReadTrimmer"),
                        Collections.singletonList(completelyTrimName)},
                {new ArgumentsBuilder(),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Collections.singletonList(ReadFilterLibrary.GOOD_CIGAR),
                        Arrays.asList("TrailingNtrimmer", "CutReadTrimmer"),
                        Arrays.asList(completelyTrimName,"GoodCigarReadFilter")},
                {new ArgumentsBuilder(),
                        Collections.singletonList(new TrailingNtrimmer()),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED),
                        Collections.singletonList("TrailingNtrimmer"),
                        Arrays.asList(completelyTrimName,"GoodCigarReadFilter", "MappedReadFilter")},
                // with arguments to enable new ones
                {new ArgumentsBuilder().addArgument("trimmer", "MottQualityTrimmer"),
                        Collections.singletonList(new TrailingNtrimmer()),
                        Collections.emptyList(),
                        Arrays.asList("TrailingNtrimmer", "MottQualityTrimmer"),
                        Collections.singletonList(completelyTrimName)},
                {new ArgumentsBuilder().addArgument("readFilter", "GoodCigarReadFilter"),
                        Collections.singletonList(new TrailingNtrimmer()),
                        Collections.emptyList(),
                        Collections.singletonList("TrailingNtrimmer"),
                        Arrays.asList(completelyTrimName, "GoodCigarReadFilter")},
                // with arguments to disable default ones
                {new ArgumentsBuilder().addArgument("disableTrimmer", "TrailingNtrimmer"),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Collections.emptyList(),
                        Collections.singletonList("CutReadTrimmer"),
                        Collections.singletonList(completelyTrimName)},
                {new ArgumentsBuilder().addArgument("disableReadFilter", "GoodCigarReadFilter"),
                        Collections.singletonList(new TrailingNtrimmer()),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED),
                        Collections.singletonList("TrailingNtrimmer"),
                        Arrays.asList(completelyTrimName, "MappedReadFilter")},
                // disabling all trimmers and/or filters (no user-provided)
                {new ArgumentsBuilder().addBooleanArgument("disableAllDefaultTrimmers", true),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED),
                        Collections.emptyList(),
                        Arrays.asList(completelyTrimName,"GoodCigarReadFilter", "MappedReadFilter")},
                {new ArgumentsBuilder().addBooleanArgument("disableToolDefaultReadFilters", true),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED),
                        Arrays.asList("TrailingNtrimmer", "CutReadTrimmer"),
                        Collections.singletonList(completelyTrimName)},
                // disabling default trimmers and/or filters, with user provided
                {new ArgumentsBuilder().addBooleanArgument("disableAllDefaultTrimmers", true)
                        .addArgument("trimmer", "MottQualityTrimmer"),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED),
                        Collections.singletonList("MottQualityTrimmer"),
                        Arrays.asList(completelyTrimName,"GoodCigarReadFilter", "MappedReadFilter")},
                {new ArgumentsBuilder().addBooleanArgument("disableToolDefaultReadFilters", true)
                        .addArgument("readFilter", "FirstOfPairReadFilter"),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED),
                        Arrays.asList("TrailingNtrimmer", "CutReadTrimmer"),
                        Arrays.asList(completelyTrimName, "FirstOfPairReadFilter")},
                // disable all default trimmers and/or filters, and enable again in different order
                {new ArgumentsBuilder().addBooleanArgument("disableAllDefaultTrimmers", true)
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("trimmer", "TrailingNtrimmer"),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED),
                        Arrays.asList("CutReadTrimmer", "TrailingNtrimmer"),
                        Arrays.asList(completelyTrimName,"GoodCigarReadFilter", "MappedReadFilter")},
                {new ArgumentsBuilder().addBooleanArgument("disableToolDefaultReadFilters", true)
                        .addArgument("readFilter", "MappedReadFilter")
                        .addArgument("readFilter", "GoodCigarReadFilter"),
                        Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED),
                        Arrays.asList("TrailingNtrimmer", "CutReadTrimmer"),
                        Arrays.asList(completelyTrimName, "MappedReadFilter", "GoodCigarReadFilter")},
        };
    }

    @Test(dataProvider = "pipelineArguments")
    public void testParsingAndPipelineFromPlugin(final ArgumentsBuilder arguments,
            final List<TrimmingFunction> defaultTrimmers, final List<ReadFilter> defaultFilters,
            final List<String> expectedTrimmerName, final List<String> expectedFilterNames)
            throws Exception {
        final CommandLineParser clp = new CommandLineArgumentParser(new Object(), Arrays.asList(
                new GATKReadFilterPluginDescriptor(defaultFilters),
                new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(), defaultTrimmers)),
                Collections.emptySet());
        clp.parseArguments(NULL_PRINT_STREAM, arguments.getArgsArray());
        final TrimAndFilterPipeline pipeline = TrimAndFilterPipeline.fromPluginDescriptors(
                clp.getPluginDescriptor(TrimmerPluginDescriptor.class),
                clp.getPluginDescriptor(GATKReadFilterPluginDescriptor.class));

        final List<String> trimmerNames = pipeline.getTrimmingStats().stream()
                .map(fs -> fs.TRIMMER).collect(Collectors.toList());
        final List<String> filterNames = pipeline.getFilterStats().stream()
                .map(fs -> fs.FILTER).collect(Collectors.toList());

        Assert.assertEquals(trimmerNames, expectedTrimmerName, trimmerNames.toString());
        Assert.assertEquals(filterNames, expectedFilterNames, filterNames.toString());
    }
}