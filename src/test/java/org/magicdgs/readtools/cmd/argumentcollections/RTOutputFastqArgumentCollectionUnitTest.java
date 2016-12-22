/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.utils.read.writer.FastqGATKWriter;
import org.magicdgs.readtools.utils.read.writer.ReadToolsOutputFormat;
import org.magicdgs.readtools.utils.read.writer.SplitGATKWriter;
import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTOutputFastqArgumentCollectionUnitTest extends BaseTest {

    @Test
    public void testSplitOutput() throws Exception {
        // this are the expected files with the output prefix
        final String outputPrefix = createTestTempDir(this.getClass().getSimpleName())
                .getAbsolutePath() + "splitOutput";
        final List<File> expectedFiles = Stream.of("_1", "_2", "_SE")
                .map(e -> new File(outputPrefix + e + ".fq.gz")).collect(Collectors.toList());
        // set the arguments
        final RTOutputFastqArgumentCollection args = new RTOutputFastqArgumentCollection();
        args.outputPrefix = outputPrefix;
        testOutputs(args, SplitGATKWriter.class, expectedFiles);
    }

    @Test
    public void testInterleavedOutput() throws Exception {
        // this are the expected files with the output prefix
        final String outputPrefix = createTestTempDir(this.getClass().getSimpleName())
                .getAbsolutePath() + "interleaved";
        final List<File> expectedFiles =
                Collections.singletonList(new File(outputPrefix + ".fq.gz"));
        // set the arguments
        final RTOutputFastqArgumentCollection args = new RTOutputFastqArgumentCollection();
        args.outputPrefix = outputPrefix;
        args.interleaved = true;
        testOutputs(args, FastqGATKWriter.class, expectedFiles);
    }

    @Test
    public void testPlainFormat() throws Exception {
        // this are the expected files with the output prefix
        final String outputPrefix = createTestTempDir(this.getClass().getSimpleName())
                .getAbsolutePath() + "interleaved";
        final List<File> expectedFiles =
                Collections.singletonList(new File(outputPrefix + ".fq"));
        // set the arguments
        final RTOutputFastqArgumentCollection args = new RTOutputFastqArgumentCollection();
        args.outputPrefix = outputPrefix;
        args.interleaved = true;
        args.outputFormat = ReadToolsOutputFormat.FastqFormat.PLAIN;
        testOutputs(args, FastqGATKWriter.class, expectedFiles);
    }

    // expected files should have as prefix the test name
    private void testOutputs(final RTOutputArgumentCollection args,
            final Class expectedClass, final List<File> expectedFiles) throws Exception {
        // the files does not exists before creation
        expectedFiles.forEach(f -> Assert.assertFalse(f.exists()));
        final GATKReadWriter writer = args
                .outputWriter(new SAMFileHeader(), null, true, null);
        // assert that it is splitting
        Assert.assertEquals(writer.getClass(), expectedClass);
        writer.close();
        // assert that the files exists
        expectedFiles.forEach(f -> Assert.assertTrue(f.exists()));
    }

}