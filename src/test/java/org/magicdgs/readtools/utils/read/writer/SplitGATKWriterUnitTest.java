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

package org.magicdgs.readtools.utils.read.writer;

import org.magicdgs.readtools.engine.sourcehandler.ReadsSourceHandler;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;
import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.ValidationStringency;
import junit.framework.Assert;
import org.broadinstitute.hellbender.tools.readersplitters.ReaderSplitter;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class SplitGATKWriterUnitTest extends BaseTest {

    // test directory for outputs
    private final static File TEMP_TEST_DIR =
            createTestTempDir(SplitGATKWriterUnitTest.class.getSimpleName());

    // default factories for the test
    private static final ReadWriterFactory WRITER_FACTORY = new ReadWriterFactory();
    private static final ReadReaderFactory READER_FACTORY = new ReadReaderFactory();

    @DataProvider(name = "writers")
    public Object[][] getWriterData() {
        final List<ReaderSplitter<?>> pairEndSplitter =
                Collections.singletonList(new PairEndSplitter());
        final List<String> expectedSuffixesPairSplitter = Arrays.asList("_1.fq", "_2.fq", "_SE.fq");
        return new Object[][] {
                {"singleEnd", ReadToolsOutputFormat.FastqFormat.PLAIN, pairEndSplitter, true,
                        expectedSuffixesPairSplitter},
                {"singleEnd", ReadToolsOutputFormat.FastqFormat.PLAIN, pairEndSplitter, false,
                        expectedSuffixesPairSplitter},
                {"pairEnd", ReadToolsOutputFormat.FastqFormat.PLAIN, pairEndSplitter, true,
                        expectedSuffixesPairSplitter},
                {"pairEnd", ReadToolsOutputFormat.FastqFormat.PLAIN, pairEndSplitter, false,
                        expectedSuffixesPairSplitter}
        };
    }


    @Test(dataProvider = "writers")
    public void testSplitGATKWriter(final String outputPrefix, final ReadToolsOutputFormat format,
            final List<ReaderSplitter<?>> splitter, final boolean onDemand,
            final List<String> expectedSuffixes) throws Exception {
        final List<File> expectedFiles =
                getFilesFromSuffixes(expectedSuffixes, s -> getTestFile(outputPrefix + s));
        final String tempOutputPrefix =
                new File(TEMP_TEST_DIR, outputPrefix).getAbsolutePath() + "onDemand_" + onDemand;
        final List<File> actualFiles =
                getFilesFromSuffixes(expectedSuffixes, s -> new File(tempOutputPrefix + s));
        actualFiles.forEach(f -> Assert.assertFalse(f.exists()));
        // open the writer and write the reads
        final SplitGATKWriter writer =
                new SplitGATKWriter(tempOutputPrefix, format, splitter, new SAMFileHeader(), true,
                        WRITER_FACTORY, onDemand);
        // add all the reads from the file
        expectedFiles.stream()
                .map(f -> ReadsSourceHandler.getHandler(f.getAbsolutePath(), READER_FACTORY))
                .flatMap(ReadsSourceHandler::toStream)
                .forEach(writer::addRead);
        writer.close();
        final List<String> expectedFilePaths;
        if (onDemand) {
            expectedFilePaths = new ArrayList<>(expectedFiles.size());
            // test if the files exists
            int i = 0;
            for (final File expected : expectedFiles) {
                if (fileIsEmpty(expected)) {
                    Assert.assertFalse(actualFiles.remove(i).exists());
                } else {
                    expectedFilePaths.add(expected.getAbsolutePath());
                    i++;
                }
            }
        } else {
            expectedFilePaths =
                    expectedFiles.stream().map(File::getAbsolutePath).collect(Collectors.toList());
        }
        IntegrationTestSpec.assertMatchingFiles(actualFiles, expectedFilePaths, false,
                ValidationStringency.DEFAULT_STRINGENCY);

    }

    private static List<File> getFilesFromSuffixes(final List<String> expectedSuffixes,
            final Function<String, File> suffixToFile) {
        return expectedSuffixes.stream().map(suffixToFile).collect(Collectors.toList());
    }

    private static boolean fileIsEmpty(final File file) throws Exception {
        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine() == null;
        }
    }

}