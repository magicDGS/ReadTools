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

package org.magicdgs.readtools.cmd.plugin;

import org.magicdgs.readtools.tools.trimming.TrimReadsTrimmerPluginArgumentCollection;
import org.magicdgs.readtools.utils.read.transformer.trimming.CutReadTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.MottQualityTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;
import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.barclay.argparser.CommandLineArgumentParser;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmerPluginDescriptorUnitTest extends RTBaseTest {

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
                new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(), Collections.singletonList(anonymous));

        // test all instances is empty
        Assert.assertTrue(pluginDescriptor.getUserEnabledTrimmers().isEmpty());

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
        new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(), Collections.singletonList(trimmer));
    }

    @Test
    public void testGetInstanceThrowByCollision() throws Exception {
        final TrimmerPluginDescriptor pluginDescriptor =
                new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(), Collections.emptyList());
        // pass twice the class to exercise IllegalArgumentException path
        // this will only happen if there are duplicated packages
        pluginDescriptor.createInstanceForPlugin(MottQualityTrimmer.class);
        Assert.assertThrows(IllegalArgumentException.class,
                () -> pluginDescriptor.createInstanceForPlugin(MottQualityTrimmer.class));
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
        final TrimmerPluginDescriptor pluginDescriptor = new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(), defaults);
        // test valid trimmers -> without CMD they are not found by reflection
        final Set<String> allowedTrimmers = pluginDescriptor
                .getAllowedValuesForDescriptorHelp("trimmer");
        Assert.assertEquals(allowedTrimmers.size(), 0);

        // test default trimmers
        final Set<String> allowedDisabledTrimmers = pluginDescriptor
                .getAllowedValuesForDescriptorHelp("disableTrimmer");
        Assert.assertEquals(allowedDisabledTrimmers, expectedDefaults);

        // test invalid long name returns null
        Assert.assertNull(pluginDescriptor.getAllowedValuesForDescriptorHelp("trimmingAlgorithm"));
    }

    //////////////////////////////////
    // TESTS with CommandLineArgumentParser

    private static final List<TrimmingFunction> makeDefaultTrimmerForTest() {
        return Collections.singletonList(new MottQualityTrimmer());
    }

    @DataProvider(name = "correctArguments")
    public Iterator<Object[]> getArgumentsForTesting() throws Exception {
        final List<Class> expectedDefaultClass = makeDefaultTrimmerForTest().stream()
                .map(tf -> tf.getClass()).collect(Collectors.toList());

        final List<Object[]> data = new ArrayList<>();
        for (boolean disable5prime : new boolean[] {true, false}) {
            // no arguments
            data.add(new Object[] {false, new ArgumentsBuilder(),
                    Collections.emptyList(), Collections.emptyList(),
                    disable5prime});
            data.add(new Object[] {true, new ArgumentsBuilder(),
                    expectedDefaultClass, Collections.emptyList(),
                    disable5prime});
            // test disabling trimmers (all or specifically)
            data.add(new Object[] {true, new ArgumentsBuilder()
                    .addBooleanArgument("disableAllDefaultTrimmers", true),
                    Collections.emptyList(), Collections.emptyList(),
                    disable5prime});
            data.add(new Object[] {true, new ArgumentsBuilder()
                    .addArgument("disableTrimmer", "MottQualityTrimmer"),
                    Collections.emptyList(), Collections.emptyList(),
                    disable5prime});
            // test adding a trimmer
            final String cutBasesArg = (disable5prime) ? "cut3primeBases" : "cut3primeBases";
            data.add(new Object[] {false, new ArgumentsBuilder()
                    .addArgument("trimmer", "CutReadTrimmer")
                    .addArgument(cutBasesArg, "1"),
                    Collections.emptyList(),
                    Collections.singletonList(CutReadTrimmer.class),
                    disable5prime});
            // providing an already defined one is not added twice
            data.add(new Object[] {true, new ArgumentsBuilder()
                    .addArgument("trimmer", "MottQualityTrimmer"),
                    expectedDefaultClass, Collections.emptyList(),
                    disable5prime});
            // disable a trimmer that is not in the defaults logs a warning
            data.add(new Object[] {false, new ArgumentsBuilder()
                    .addArgument("disableTrimmer", "CutReadTrimmer"),
                    Collections.emptyList(), Collections.emptyList(),
                    disable5prime});
            data.add(new Object[] {true, new ArgumentsBuilder()
                    .addArgument("disableTrimmer", "CutReadTrimmer"),
                    expectedDefaultClass, Collections.emptyList(),
                    disable5prime});
            // providing a parameter for a default trimmer
            data.add(new Object[] {true, new ArgumentsBuilder()
                    .addArgument("mottQualityThreshold", "10"),
                    expectedDefaultClass, Collections.emptyList(),
                    disable5prime});
            // test disable all trimmers but provide the same
            data.add(new Object[] {true, new ArgumentsBuilder()
                    .addBooleanArgument("disableAllDefaultTrimmers", true)
                    .addArgument("trimmer", "MottQualityTrimmer"),
                    Collections.emptyList(),
                    Collections.singletonList(MottQualityTrimmer.class),
                    disable5prime});
        }
        return data.iterator();
    }

    @Test(dataProvider = "correctArguments")
    public void testArgumentsCorrectlyParsed(final boolean withDefault,
            final ArgumentsBuilder args, final List<Class> expectedDefaults,
            final List<Class> expectedClassesUser,
            final boolean disable5prime) throws Exception {

        // the 5/3 prime are set in the same call, so we require only one for testing
        // we can't provide the two of them because they are mutex
        // in addition, the method for set disabling is alreay tested in the TrimmingFunction classes
        args.addBooleanArgument("disable5pTrim", disable5prime);

        // run the instance main and get the descriptor after parsing
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(),
                        (withDefault) ? makeDefaultTrimmerForTest() : null)),
                Collections.emptySet());

        Assert.assertTrue(clp.parseArguments(NULL_PRINT_STREAM, args.getArgsArray()));
        final TrimmerPluginDescriptor tpd = clp.getPluginDescriptor(TrimmerPluginDescriptor.class);

        // test the defaults classes
        Assert.assertEquals(
                tpd.getDefaultInstances().stream().map(Object::getClass)
                        .collect(Collectors.toList()), expectedDefaults,
                "defaults are wrong: " + tpd.getDefaultInstances());

        // test the parsed by the user
        final List<TrimmingFunction> parsedUser = tpd.getUserEnabledTrimmers();
        Assert.assertEquals(parsedUser.size(), expectedClassesUser.size(),
                "not equal number of classes: " + parsedUser);

        for (int i = 0; i < parsedUser.size(); i++) {
            final TrimmingFunction tf = parsedUser.get(i);
            // check if it is the same class
            Assert.assertEquals(tf.getClass(), expectedClassesUser.get(i));

            // check that the disable 3' is always false
            Assert.assertFalse(tf.isDisable3prime());
            // check that the disable 5 prime is the one provided
            Assert.assertEquals(tf.isDisable5prime(), disable5prime);
        }
    }

    @Test
    public void testAllTrimmersHelpAfterParsed() throws Exception {
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(), null)),
                Collections.emptySet());
        clp.parseArguments(NULL_PRINT_STREAM, new String[] {});
        Assert.assertEquals(clp.getPluginDescriptor(TrimmerPluginDescriptor.class)
                        .getAllowedValuesForDescriptorHelp("trimmer").size(),
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
                        .addBooleanArgument("disable3pTrim", true)},
                // set disable 5 prime incompatible with one trimmer parameter
                {true, new ArgumentsBuilder()
                        .addArgument("trimmer", "CutReadTrimmer")
                        .addBooleanArgument("disable5pTrim", true)
                        .addArgument("cut5primeBases", "1")}
        };
    }

    @Test(dataProvider = "incorrectArguments", expectedExceptions = CommandLineException.class)
    public void testParsingWrongArguments(final boolean withDefault,
            final ArgumentsBuilder args) throws Exception {
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(),
                        (withDefault) ? makeDefaultTrimmerForTest() : null)),
                Collections.emptySet());
        clp.parseArguments(NULL_PRINT_STREAM, args.getArgsArray());
    }

    @DataProvider(name = "mutexArgs")
    public Object[][] mutexArgs() {
        return new Object[][] {
                {true, false},
                {false, true},
                {false, false}
        };
    }

    @Test(dataProvider = "mutexArgs")
    public void testMutexArgsParsing(final boolean disable5pTrim, final boolean disable3pTrim)
            throws Exception {
        final CommandLineArgumentParser clp = new CommandLineArgumentParser(new Object(),
                Collections.singletonList(new TrimmerPluginDescriptor(new TrimReadsTrimmerPluginArgumentCollection(), null)),
                Collections.emptySet());
        final boolean parsed = clp.parseArguments(NULL_PRINT_STREAM,
                new ArgumentsBuilder()
                        .addBooleanArgument("disable5pTrim", disable5pTrim)
                        .addBooleanArgument("disable3pTrim", disable3pTrim)
                        .getArgsArray());
        Assert.assertTrue(parsed);
    }
}