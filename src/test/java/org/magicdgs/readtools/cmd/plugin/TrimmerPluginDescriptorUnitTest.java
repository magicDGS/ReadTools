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

import org.broadinstitute.barclay.argparser.CommandLineArgumentParser;
import org.broadinstitute.barclay.argparser.CommandLineException;
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
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmerPluginDescriptorUnitTest extends BaseTest {

    // TODO: maybe we should find another way of testing this
    // this is the number of trimmers implemented to check if a returned value is correct
    // it should be modified every time a new trimmer is implemented
    private static final int NUMBER_OF_TRIMMERS_IMPLEMENTED = 3;

    @Test
    public void testAnonymousClassAsToolDefault() throws Exception {
        final TrimmingFunction anonymous = new TrimmingFunction() {
            @Override
            protected void fillTrimPoints(GATKRead read, int[] toFill) {
                // do nothing
            }
        };

        final TrimmerPluginDescriptor pluginDescriptor =
                new TrimmerPluginDescriptor(Collections.singletonList(anonymous));

        // test all instances is empty
        Assert.assertTrue(pluginDescriptor.getAllInstances().isEmpty());

        // test that default instances are not
        final List<TrimmingFunction> defaultInsances = pluginDescriptor.getDefaultInstances();
        Assert.assertEquals(defaultInsances.size(), 1);
        Assert.assertSame(defaultInsances.get(0), anonymous);
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
    public void testGetAllowedValuesForDisableTrimmer(final List<TrimmingFunction> defaults,
            final Set<String> expectedDefaults) throws Exception {
        final TrimmerPluginDescriptor pluginDescriptor = new TrimmerPluginDescriptor(defaults);
        // test valid trimmers -> without CMD they are not found by reflection
        final Set<String> allowedTrimmers = pluginDescriptor
                .getAllowedValuesForDescriptorArgument("trimmer");
        Assert.assertEquals(allowedTrimmers.size(), 0);

        // test default trimmers
        final Set<String> allowedDisabledTrimmers = pluginDescriptor
                .getAllowedValuesForDescriptorArgument("disableTrimmer");
        Assert.assertEquals(allowedDisabledTrimmers, expectedDefaults);

        // test invalid long name
        Assert.assertThrows(IllegalArgumentException.class,
                () -> pluginDescriptor.getAllowedValuesForDescriptorArgument("trimmingAlgorithm"));
    }

    //////////////////////////////////
    // TESTS with CommandLineArgumentParser

    private static final List<TrimmingFunction> makeDefaultTrimmerForTest() {
        return Collections.singletonList(new MottQualityTrimmer());
    }

    @DataProvider(name = "correctArguments")
    public Object[][] getArgumentsForTesting() throws Exception {
        final List<Class> expectedDefaultClass = makeDefaultTrimmerForTest().stream()
                .map(tf -> tf.getClass()).collect(Collectors.toList());
        return new Object[][] {
                // no arguments
                {false, new ArgumentsBuilder(),
                        Collections.emptyList(), Collections.emptyList()},
                {true, new ArgumentsBuilder(),
                        expectedDefaultClass, Collections.emptyList()},
                // test disabling trimmers (all or specifically)
                {true, new ArgumentsBuilder()
                        .addBooleanArgument("disableAllDefaultTrimmers", true),
                        Collections.emptyList(), Collections.emptyList()},
                {true, new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "MottQualityTrimmer"),
                        Collections.emptyList(), Collections.emptyList()},
                // test adding a trimmer
                {false, new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("cut5primeBases", "1"),
                        Collections.emptyList(), Collections.singletonList(CutReadTrimmer.class)},
                {true, new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("cut3primeBases", "1"),
                        expectedDefaultClass, Collections.singletonList(CutReadTrimmer.class)},
                // providing an already defined one is not added twice
                {true, new ArgumentsBuilder()
                        .addArgument("trimmer", "MottQualityTrimmer"),
                        expectedDefaultClass, Collections.emptyList()},
                // disable a trimmer that is not in the defaults logs a warning
                {false, new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "CutReadTrimmer"),
                        Collections.emptyList(), Collections.emptyList()},
                {true, new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "CutReadTrimmer"),
                        expectedDefaultClass, Collections.emptyList()},
                // providing a parameter for a default trimmer
                {true, new ArgumentsBuilder()
                        .addArgument("mottQualityThreshold", "10"),
                        expectedDefaultClass, Collections.emptyList()},
                // test disable all trimmers but provide the same
                {true, new ArgumentsBuilder()
                        .addBooleanArgument("disableAllDefaultTrimmers", true)
                        .addArgument("trimmer", "MottQualityTrimmer"),
                        Collections.emptyList(),
                        Collections.singletonList(MottQualityTrimmer.class)}
        };
    }

    @Test(dataProvider = "correctArguments")
    public void testArgumentsCorrectlyParsed(final boolean withDefault,
            final ArgumentsBuilder args, final List<Class> expectedDefaults,
            final List<Class> expectedClassesUser) throws Exception {

        // run the instance main and get the descriptor after parsing
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TrimmerPluginDescriptor(
                        (withDefault) ? makeDefaultTrimmerForTest() : null)));

        Assert.assertTrue(clp.parseArguments(System.out, args.getArgsArray()));
        final TrimmerPluginDescriptor tpd = clp.getPluginDescriptor(TrimmerPluginDescriptor.class);

        // test the defaults classes
        Assert.assertEquals(
                tpd.getDefaultInstances().stream().map(TrimmingFunction::getClass)
                        .collect(Collectors.toList()), expectedDefaults,
                "defaults are wrong: " + tpd.getDefaultInstances());

        // test the parsed by the user
        final List<TrimmingFunction> parsedUser = tpd.getAllInstances();
        Assert.assertEquals(parsedUser.size(), expectedClassesUser.size(),
                "not equal number of classes: " + parsedUser);
        Assert.assertEquals(
                parsedUser.stream().map(TrimmingFunction::getClass)
                        .collect(Collectors.toList()),
                expectedClassesUser, "order not maintained");
    }

    @Test
    public void testAllTrimmersHelpAfterParsed() throws Exception {
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TrimmerPluginDescriptor(null)));
        clp.parseArguments(System.out, new String[] {});
        Assert.assertEquals(clp.getPluginDescriptor(TrimmerPluginDescriptor.class)
                        .getAllowedValuesForDescriptorArgument("trimmer").size(),
                NUMBER_OF_TRIMMERS_IMPLEMENTED);
    }


    @DataProvider(name = "incorrectArguments")
    public Object[][] getIncorrectArgumentsForTesting() throws Exception {
        return new Object[][] {
                // test unknown trimmer
                {false, new ArgumentsBuilder()
                        .addArgument("trimmer", "UnknownTrimmer")},
                // test enable/disable the same trimmer
                {false, new ArgumentsBuilder()
                        .addArgument("trimmer", "TrailingNtrimmer")
                        .addArgument("disableTrimmer", "TrailingNtrimmer")},
                // test trimmer with illegal parameters
                {false, new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")},
                // only providing a parameter but not the trimmer it belongs to
                {false, new ArgumentsBuilder()
                        .addArgument("cut5primeBases", "5")},
                // disable trimmer and providing an argument for it
                {false, new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "MottQualityTrimmer")
                        .addArgument("mottQualityThreshold", "10")},
                // provide twice the same trimmer
                {false, new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addArgument("cut5primeBases", "2")
                        .addArgument("cut3primeBases", "1")},
                {true, new ArgumentsBuilder()
                        .addArgument("disableTrimmer", "MottQualityTrimmer")
                        .addBooleanArgument("disableAllDefaultTrimmers", true)},
                // TODO: enable this test if it is implemented that a parameter is provided for a disable default trimmer
                // {true, new ArgumentsBuilder()
                //        .addArgument("disableTrimmer", "MottQualityTrimmer")
                //        .addArgument("mottQualityThreshold", "10")},
                // testing mutex arguments both set
                {true, new ArgumentsBuilder()
                        .addBooleanArgument("disable5pTrim", true)
                        .addBooleanArgument("disable3pTrim", true)}
        };
    }

    @Test(dataProvider = "incorrectArguments", expectedExceptions = CommandLineException.class)
    public void testParsingWrongArguments(final boolean withDefault,
            final ArgumentsBuilder args) throws Exception {
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TrimmerPluginDescriptor(
                        (withDefault) ? makeDefaultTrimmerForTest() : null)));
        clp.parseArguments(System.out, args.getArgsArray());
    }

    // TODO: it would be nice to allow that both parameters are supplied but are complementary
    // TODO: this require that they are not mutex, but handled differently
    // TODO: see also https://github.com/broadinstitute/barclay/issues/26
    @Test(dataProvider = "mutexArgs", enabled = false)
    public void testMutexArgsParsing(final boolean disable5pTrim, final boolean disable3pTrim)
            throws Exception {
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TrimmerPluginDescriptor(null)));
        final boolean parsed = clp.parseArguments(System.out,
                new ArgumentsBuilder()
                        .addBooleanArgument("disable5pTrim", disable5pTrim)
                        .addBooleanArgument("disable3pTrim", disable3pTrim)
                        .getArgsArray());
        Assert.assertTrue(parsed);
    }
}