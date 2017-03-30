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

import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;
import org.magicdgs.readtools.utils.read.writer.SplitGATKWriter;
import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.SAMFileGATKReadWriter;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTOutputBamSplitArgumentCollectionUnitTest extends RTBaseTest {

    private static final List<SAMReadGroupRecord> READ_GROUPS = Arrays.asList(
            new SAMReadGroupRecord("1"), new SAMReadGroupRecord("2"), new SAMReadGroupRecord("3"));
    private static final SAMFileHeader HEADER = ArtificialReadUtils.createArtificialSamHeader();

    static {
        READ_GROUPS.forEach(rg -> {
            rg.setSample(rg.getId());
            rg.setLibrary(rg.getId());
        });
        HEADER.setReadGroups(READ_GROUPS);
    }

    @DataProvider
    public Iterator<Object[]> outputsData() throws Exception {
        final List<Object[]> data = new ArrayList<>();
        final boolean[] trueFalse = new boolean[] {true, false};
        for (final ReadToolsIOFormat.BamFormat format : ReadToolsIOFormat.BamFormat
                .values()) {
            // TODO: test cram format -> requires a reference
            if (!format.equals(ReadToolsIOFormat.BamFormat.CRAM)) {
                for (final boolean sample : trueFalse) {
                    for (final boolean id : trueFalse) {
                        for (final boolean library : trueFalse) {
                            data.add(new Object[] {format, sample, id, library});
                        }
                    }
                }
            }
        }
        return data.iterator();
    }

    @Test(dataProvider = "outputsData")
    public void testOutputs(final ReadToolsIOFormat.BamFormat format,
            final boolean sample, final boolean id, final boolean library) throws Exception {
        // creates output prefix
        final File outputPrefix = new File(
                createTestTempDir(this.getClass().getSimpleName()).getAbsolutePath() + "split",
                String.format("sample%s_id%s_library%s", sample, id, library)
        );

        final RTOutputBamSplitArgumentCollection args = new RTOutputBamSplitArgumentCollection();
        args.outputPrefix = outputPrefix.getAbsolutePath();
        args.splitBySample = sample;
        args.splitByReadGroup = id;
        args.splitByLibrary = library;
        args.outputFormat = format;
        final List<File> expectedFiles = READ_GROUPS.stream().map(rg -> {
            String suffix = "";
            if (sample) {
                suffix += "_" + rg.getSample();
            }
            if (id) {
                suffix += "_" + rg.getReadGroupId();
            }
            if (library) {
                suffix += "_" + rg.getLibrary();
            }
            return suffix + format.getExtension();
        }).map(s -> new File(outputPrefix.getAbsolutePath() + s)).collect(Collectors.toList());

        testOutputs(args,
                (sample || id || library) ? SplitGATKWriter.class : SAMFileGATKReadWriter.class,
                expectedFiles);
    }


    // expected files should have as prefix the test name
    private void testOutputs(final RTOutputArgumentCollection args,
            final Class expectedClass, final List<File> expectedFiles) throws Exception {
        // the files does not exists before creation
        expectedFiles.forEach(f -> Assert.assertFalse(f.exists()));
        final GATKReadWriter writer = args
                .outputWriter(HEADER, null, true, null);
        // assert that it is splitting
        Assert.assertEquals(writer.getClass(), expectedClass);
        writer.close();
        // assert that the files exists
        expectedFiles.forEach(f -> Assert.assertTrue(f.exists(), f.getName() + " does not exists"));
    }

    @DataProvider
    public Iterator<Object[]> outputWithSuffix() throws Exception {
        final List<Object[]> data = new ArrayList<>();
        for (final ReadToolsIOFormat.BamFormat format : ReadToolsIOFormat.BamFormat
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
            ReadToolsIOFormat.BamFormat format, final String suffix,
            final String expectedOutputName) throws Exception {
        final RTOutputBamSplitArgumentCollection args = new RTOutputBamSplitArgumentCollection();
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
        final RTOutputBamSplitArgumentCollection args = new RTOutputBamSplitArgumentCollection();
        args.outputPrefix = outputPrefix;
        Assert.assertEquals(args.makeMetricsFile(suffix).toFile(), expectedMetricsFile);
    }
}