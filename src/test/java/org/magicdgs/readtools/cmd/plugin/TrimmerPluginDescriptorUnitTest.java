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

package org.magicdgs.readtools.cmd.plugin;

import org.magicdgs.readtools.utils.read.transformer.trimming.CutReadTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.MottQualityTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;
import org.magicdgs.readtools.utils.tests.BaseTest;

import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLinePluginDescriptor;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;
import org.broadinstitute.hellbender.cmdline.TestProgramGroup;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmerPluginDescriptorUnitTest extends BaseTest {

    // TODO: this set should be updated every time that a new trimmer is implemented
    // TODO: we should come up with a way of testing this without this problem
    private static final Set<String> ALL_TRIMMERS = new TreeSet<>(Arrays.asList(
            "CutReadTrimmer",
            "MottQualityTrimmer",
            "TrailingNtrimmer"
    ));

    @Test
    public void testAnonymousClassAsToolDefault() throws Exception {
        final TrimmingFunction anonymous = new TrimmingFunction() {
            @Override
            protected void update(GATKRead read) {
                // do nothing
            }
        };

        final TrimmerPluginDescriptor pluginDescriptor =
                new TrimmerPluginDescriptor(Collections.singletonList(anonymous));
        final List<TrimmingFunction> allInstances = pluginDescriptor.getAllInstances();

        Assert.assertEquals(allInstances.size(), 1);
        Assert.assertSame(allInstances.get(0), anonymous);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIllegalArgumentsOnConstruction() throws Exception {
        // this trimmer default constructor does not have valid arguments
        final CutReadTrimmer trimmer = new CutReadTrimmer();
        // this should blow up
        new TrimmerPluginDescriptor(Collections.singletonList(trimmer));
    }

    @Test
    public void testGetInstanceThrowByCollision() throws Exception {
        final TrimmerPluginDescriptor pluginDescriptor =
                new TrimmerPluginDescriptor(Collections.emptyList());
        // pass twice the class to exercise IllegalArgumentException path
        // this will only happen if there are duplicated packages
        pluginDescriptor.getInstance(MottQualityTrimmer.class);
        Assert.assertThrows(IllegalArgumentException.class,
                () -> pluginDescriptor.getInstance(MottQualityTrimmer.class));
    }

    @DataProvider(name = "defaultTrimmingFunctionsForHelp")
    public Object[][] defaultTrimmingFunctionsForHelp() {
        final TrimmingFunction cut = new CutReadTrimmer(1, 1);
        final String cutName = "CutReadTrimmer";
        final TrimmingFunction mott = new MottQualityTrimmer();
        final String mottName = "MottQualityTrimmer";
        return new Object[][] {
                {null, Collections.emptySet()},
                {Collections.emptyList(), Collections.emptySet()},
                {Collections.singletonList(cut), Collections.singleton(cutName)},
                {Collections.singletonList(mott), Collections.singleton(mottName)},
                {Arrays.asList(cut, mott), new LinkedHashSet<>(Arrays.asList(cutName, mottName))},
                {Arrays.asList(mott, cut), new LinkedHashSet<>(Arrays.asList(mottName, cutName))},
        };
    }

    @Test(dataProvider = "defaultTrimmingFunctionsForHelp")
    public void testGetAllowedValuesForDescriptorArgument(final List<TrimmingFunction> defaults,
            final Set<String> expectedDefaults) throws Exception {
        final TrimmerPluginDescriptor pluginDescriptor = new TrimmerPluginDescriptor(defaults);
        // test valid trimmers
        final Set<String> allowedTrimmers = pluginDescriptor
                .getAllowedValuesForDescriptorArgument("trimmer");
        Assert.assertEquals(allowedTrimmers, ALL_TRIMMERS);

        // test default trimmers
        final Set<String> allowedDisabledTrimmers = pluginDescriptor
                .getAllowedValuesForDescriptorArgument("disableTrimmer");
        Assert.assertEquals(allowedDisabledTrimmers, expectedDefaults);

        // test invalid long name
        Assert.assertThrows(IllegalArgumentException.class,
                () -> pluginDescriptor.getAllowedValuesForDescriptorArgument("trimmingAlgorithm"));
    }

    //////////////////////////////////
    // TESTS in a CommandLine program

    @CommandLineProgramProperties(summary = "Test tool with trimmer plugin", oneLineSummary = "Test tool with trimmer plugin", programGroup = TestProgramGroup.class)
    private static class ClpWithTrimmingPlugin extends CommandLineProgram {

        private final List<TrimmingFunction> defaultTrimmers;

        private ClpWithTrimmingPlugin(final List<TrimmingFunction> defaultTrimmers) {
            this.defaultTrimmers = defaultTrimmers;
        }

        protected final List<? extends CommandLinePluginDescriptor<?>> getPluginDescriptors() {
            return Collections.singletonList(new TrimmerPluginDescriptor(defaultTrimmers));
        }

        @Override
        protected final Object doWork() {
            return commandLineParser.getPluginDescriptor(TrimmerPluginDescriptor.class)
                    .getAllInstances();
        }
    }


    private static final ClpWithTrimmingPlugin getClpWithNotDefaultTrimmers() {
        return new ClpWithTrimmingPlugin(Collections.emptyList());
    }

    private static final ClpWithTrimmingPlugin getClpWithMottDefaultTrimmers() {
        return new ClpWithTrimmingPlugin(Collections.singletonList(new MottQualityTrimmer()));
    }

    @DataProvider(name = "correctArguments")
    public Object[][] getArgumentsForTesting() throws Exception {
        return new Object[][] {
                // no arguments
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder(),
                        Collections.emptyList()},
                {getClpWithMottDefaultTrimmers(), new ArgumentsBuilder(),
                        Collections.singletonList(MottQualityTrimmer.class)},
                // test disabling trimmers (all or specifically)
                {getClpWithMottDefaultTrimmers(), new ArgumentsBuilder()
                        .addBooleanArgument("disableAllTrimmers", true),
                        Collections.emptyList()},
                {getClpWithMottDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "MottQualityTrimmer"),
                        Collections.emptyList()},
                // test adding a trimmer
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("cut5primeBases", "1"),
                        Collections.singletonList(CutReadTrimmer.class)},
                {getClpWithMottDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("cut3primeBases", "1"),
                        Arrays.asList(MottQualityTrimmer.class, CutReadTrimmer.class)},
                // providing an already defined one is not added twice
                {getClpWithMottDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("trimmer", "MottQualityTrimmer"),
                        Collections.singletonList(MottQualityTrimmer.class)},
                // disable a trimmer that is not in the defaults logs a warning
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "CutReadTrimmer"),
                        Collections.emptyList()},
                {getClpWithMottDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "CutReadTrimmer"),
                        Collections.singletonList(MottQualityTrimmer.class)},
                // providing a parameter for a default trimmer
                {getClpWithMottDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("mottQualityThreshold", "10"),
                        Collections.singletonList(MottQualityTrimmer.class)}
        };
    }

    @Test(dataProvider = "correctArguments")
    public void testArgumentsCorrectlyParsed(final ClpWithTrimmingPlugin clp,
            final ArgumentsBuilder args, final List<Class> expectedClasses) throws Exception {
        // make test output silent except if an error occurs
        args.addArgument("verbosity", "ERROR").addBooleanArgument("QUIET", true);
        // run the instance main
        final List<TrimmingFunction> parsedTrimmers =
                (List<TrimmingFunction>) clp.instanceMain(args.getArgsArray());

        Assert.assertEquals(parsedTrimmers.size(), expectedClasses.size(),
                "not equal number of classes: " + parsedTrimmers);

        Assert.assertEquals(
                parsedTrimmers.stream().map(TrimmingFunction::getClass)
                        .collect(Collectors.toList()),
                expectedClasses, "order not maintained");
    }

    @DataProvider(name = "incorrectArguments")
    public Object[][] getIncorrectArgumentsForTesting() throws Exception {
        return new Object[][] {
                // test unknown trimmer
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("trimmer", "UnknownTrimmer")},
                // test enable/disable the same trimmer
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("trimmer", "TrailingNtrimmer")
                        .addArgument("disableTrimmer", "TrailingNtrimmer")},
                // test trimmer with illegal parameters
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")},
                // only providing a parameter but not the trimmer it belongs to
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("cut5primeBases", "5")},
                // disable trimmer and providing an argument for it
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "MottQualityTrimmer")
                        .addArgument("mottQualityThreshold", "10")},
                // provide twice the same trimmer
                {getClpWithNotDefaultTrimmers(), new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("cut5primeBases", "2")
                        .addArgument("cut3primeBases", "1")},
                // TODO: enable this test if it is implemented that a parameter is provided for a disable defaul trimmer
                // {new getClpWithMottDefaultTrimmers(), new ArgumentsBuilder()
                //        .addArgument("disableTrimmer", "MottQualityTrimmer")
                //        .addArgument("mottQualityThreshold", "10")}
        };
    }

    @Test(dataProvider = "incorrectArguments", expectedExceptions = CommandLineException.class)
    public void testParsingWrongArguments(final ClpWithTrimmingPlugin clp,
            final ArgumentsBuilder args) throws Exception {
        clp.instanceMain(args.getArgsArray());
    }
}