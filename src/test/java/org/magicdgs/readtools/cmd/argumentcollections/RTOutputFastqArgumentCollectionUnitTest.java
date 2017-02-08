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

import org.magicdgs.readtools.utils.read.writer.FastqGATKWriter;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;
import org.magicdgs.readtools.utils.read.writer.SplitGATKWriter;
import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
        args.outputFormat = ReadToolsIOFormat.FastqFormat.PLAIN;
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

    @DataProvider
    public Iterator<Object[]> outputWithSuffix() throws Exception {
        final List<Object[]> data = new ArrayList<>();
        for (final ReadToolsIOFormat.FastqFormat format : ReadToolsIOFormat.FastqFormat
                .values()) {
            data.add(new Object[] {"prefix", format, "_suffix",
                    "prefix_suffix" + format.getExtension()});
            data.add(new Object[] {"prefix.one_suffix", format, ".second",
                    "prefix.one_suffix.second" + format.getExtension()});
            data.add(new Object[] {"prefix.one_suffix", format, "_second",
                    "prefix.one_suffix_second" + format.getExtension()});
        }
        return data.iterator();
    }

    @Test(dataProvider = "outputWithSuffix")
    public void testGetOutputNameWithSuffix(final String outputPrefix,
            ReadToolsIOFormat.FastqFormat format, final String suffix,
            final String expectedOutputName) throws Exception {
        final RTOutputFastqArgumentCollection args = new RTOutputFastqArgumentCollection();
        args.outputPrefix = outputPrefix;
        args.outputFormat = format;
        Assert.assertEquals(args.getOutputNameWithSuffix(suffix), expectedOutputName);
    }

    @DataProvider
    public Object[][] getMetricsNames() {
        return new Object[][] {
                {"example", null, new File("example.metrics")},
                {"example.2", null, new File("example.2.metrics")},
                {"example", "", new File("example.metrics")},
                {"example", "_suffix", new File("example_suffix.metrics")}
        };
    }

    @Test(dataProvider = "getMetricsNames")
    public void testMakeMetricsFile(final String outputPrefix, final String suffix,
            final File expectedMetricsFile) throws Exception {
        final RTOutputFastqArgumentCollection args = new RTOutputFastqArgumentCollection();
        args.outputPrefix = outputPrefix;
        Assert.assertEquals(args.makeMetricsFile(suffix).toFile(), expectedMetricsFile);
    }
}