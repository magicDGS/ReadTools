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
import org.magicdgs.readtools.BaseTest;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
    public Iterator<Object[]> getBadArguments() {
        final List<Object[]> data = new ArrayList<>(20);

        final List<String> invalidTagNames = Arrays.asList("1A", "B1A", "A+", "+A");

        // common failing arguments
        for (boolean fixQuals : new boolean[] {true, false}) {
            // incompatible arguments (read name and barcode sequence)
            data.add(new Object[] {
                    new ArgumentsBuilder()
                            .addBooleanArgument("barcodeInReadName", true)
                            .addArgument("rawBarcodeSequenceTags", "B1"),
                    fixQuals});
            // repeat barcode sequences
            data.add(new Object[] {
                    new ArgumentsBuilder()
                            .addArgument("rawBarcodeSequenceTags", "B1")
                            .addArgument("rawBarcodeSequenceTags", "B1"),
                    fixQuals});
            // invalid tag names in barcode sequence
            invalidTagNames.forEach(tag -> data.add(new Object[] {
                    new ArgumentsBuilder()
                            .addArgument("rawBarcodeSequenceTags", tag),
                    fixQuals}));
        }

        // invalid tag names in quality tag
        invalidTagNames.forEach(tag -> data.add(new Object[] {
                new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", tag),
                true}));

        // incompatible arguments (read name and quality tag)
        data.add(new Object[] {
                new ArgumentsBuilder()
                        .addBooleanArgument("barcodeInReadName", true)
                        .addArgument("rawBarcodeQualityTag", "Q2"),
                true});

        // repeat quality tag
        data.add(new Object[] {
                new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeSequenceTags", "B2")
                        .addArgument("rawBarcodeQualityTag", "Q1")
                        .addArgument("rawBarcodeQualityTag", "Q1"),
                true});

        // only quality (setting to null barcode sequence tags
        data.add(new Object[] {
                new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "null")
                        .addArgument("rawBarcodeQualityTag", "Q2"),
                true});

        // different length of sequence/quality tag (default sequence)
        data.add(new Object[] {
                new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "Q1")
                        .addArgument("rawBarcodeQualityTag", "Q2"),
                true});

        // different length of sequence/quality tag (overridden sequence)
        data.add(new Object[] {
                new ArgumentsBuilder()
                        .addArgument("rawBarcodeQualityTag", "Q1")
                        .addArgument("rawBarcodeQualityTag", "Q2")
                        .addArgument("rawBarcodeSequenceTags", "B1"),
                true});
        // and in the other way around
        data.add(new Object[] {
                new ArgumentsBuilder()
                        .addArgument("rawBarcodeSequenceTags", "B1")
                        .addArgument("rawBarcodeSequenceTags", "B2")
                        .addArgument("rawBarcodeQualityTag", "Q1"),
                true});

        return data.iterator();
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