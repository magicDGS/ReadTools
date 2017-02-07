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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.utils.read.transformer.barcodes.FixRawBarcodeTagsReadTransformer;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixReadNameBarcodesReadTransformer;
import org.magicdgs.readtools.utils.tests.BaseTest;

import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;
import org.broadinstitute.hellbender.cmdline.TestProgramGroup;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FixBarcodeAbstractArgumentCollectionTest extends BaseTest {

    @CommandLineProgramProperties(
            oneLineSummary = "Fix barcodes CLP test",
            summary = "Fix barcodes CLP test",
            programGroup = TestProgramGroup.class)
    private static class FixBarcodesCLP extends CommandLineProgram {
        @ArgumentCollection
        public FixBarcodeAbstractArgumentCollection args;

        public FixBarcodesCLP(final boolean fixQuals) {
            this.args = FixBarcodeAbstractArgumentCollection.getArgumentCollection(fixQuals);
        }

        @Override
        public String[] customCommandLineValidation() {
            args.validateArguments();
            return null;
        }

        @Override
        protected Object doWork() {
            return args.getFixBarcodeReadTransformer().getClass();
        }
    }

    @DataProvider(name = "badArgs")
    public Object[][] getBadArguments() {
        return new Object[][] {
                // Without fixing quality
                // incompatible arguments
                {new ArgumentsBuilder()
                        .addBooleanArgument("barcodeInReadName", true)
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                        false},
                // repeating barcodes should fail
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                        false},
                // invalid tag names
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "1A"),
                        false},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1A"),
                        false},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "A+"),
                        false},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "+A"),
                        false},

                // With fixing quality bad arguments
                // incompatible arguments (in read name and sequence tag)
                {new ArgumentsBuilder()
                        .addBooleanArgument("barcodeInReadName", true)
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                        true},
                // incompatible arguments (in read name and quality tag)
                {new ArgumentsBuilder()
                        .addBooleanArgument("barcodeInReadName", true)
                        .addArgument("rawBarcodeQualityTag", "Q2"),
                        true},
                // repeating barcodes should fail
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                        true},
                // repeating qualities should fail
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeSequenceTags", "B2")
                        .addArgument("rawBarcodeQualityTag", "Q1")
                        .addArgument("rawBarcodeQualityTag", "Q1"),
                        true},
                // only quality
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "null")
                        .addArgument("rawBarcodeQualityTag", "Q2"),
                        true},
                // two quality tags and only one barcode quality (default)
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "Q1")
                        .addArgument("rawBarcodeQualityTag", "Q2"),
                        true},
                // two quality tags and only one barcode quality (overridden)
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "Q1")
                        .addArgument("rawBarcodeQualityTag", "Q2")
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                        true},
                // two barcode tags and only one quality (overridden)
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeSequenceTags", "B2")
                        .addArgument("rawBarcodeQualityTag", "Q1"),
                        true},
                // incompatible arguments
                {new ArgumentsBuilder()
                        .addBooleanArgument("barcodeInReadName", true)
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                        true},
                // invalid qual tag names
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "1A"),
                        true},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "A1A"),
                        true},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "A+"),
                        true},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "+A"),
                        true},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "1A"),
                        true},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "A1A"),
                        true},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "A+"),
                        true},
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "+A"),
                        true}
        };
    }

    @Test(dataProvider = "badArgs", expectedExceptions = CommandLineException.class)
    public void testBadArguments(final ArgumentsBuilder args, final boolean fixQuals)
            throws Exception {
        args.addArgument("verbosity", "ERROR").addBooleanArgument("QUIET", true);
        final CommandLineProgram clp = new FixBarcodesCLP(fixQuals);
        clp.instanceMain(args.getArgsArray());
    }

    @DataProvider(name = "goodArgs")
    public Object[][] getGoodArguments() {
        return new Object[][] {
                // Without fixing quality
                // no args
                {new ArgumentsBuilder(),
                        false, ReadTransformer.identity().getClass()},
                // barcodes in read name
                {new ArgumentsBuilder()
                        .addBooleanArgument("barcodeInReadName", true),
                        false, FixReadNameBarcodesReadTransformer.class},
                // barcodes in tag
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                        false, FixRawBarcodeTagsReadTransformer.class},
                // cleaning BC tag
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "null"),
                        false, ReadTransformer.identity().getClass()},

                // With fixing quality bad arguments
                // barcodes in read name
                {new ArgumentsBuilder()
                        .addBooleanArgument("barcodeInReadName", true),
                        true, FixReadNameBarcodesReadTransformer.class},
                // barcodes in tag
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                        true, FixRawBarcodeTagsReadTransformer.class},
                // no args
                {new ArgumentsBuilder(),
                        true, ReadTransformer.identity().getClass()},
                // cleaning BC tag
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "null"),
                        true, ReadTransformer.identity().getClass()},
                // just quality tag
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "Q1"),
                        true, FixRawBarcodeTagsReadTransformer.class},
                // 1 barcode and quality tag
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeQualityTag", "Q1"),
                        true, FixRawBarcodeTagsReadTransformer.class},
                // 2 barcode and quality tags
                {new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeQualityTag", "Q1")
                        .addArgument("rawBarcodeSequenceTags", "B2")
                        .addArgument("rawBarcodeQualityTag", "Q2"),
                        true, FixRawBarcodeTagsReadTransformer.class}
        };
    }

    @Test(dataProvider = "goodArgs")
    public void testGoodArguments(final ArgumentsBuilder args, final boolean fixQuals,
            final Class<ReadTransformer> transformerClass)
            throws Exception {
        // set verbosity to the minimal
        args.addArgument("verbosity", "ERROR").addBooleanArgument("QUIET", true);
        final CommandLineProgram clp = new FixBarcodesCLP(fixQuals);
        // assert that the transformer is not null
        Assert.assertEquals(clp.instanceMain(args.getArgsArray()), transformerClass);
    }

}