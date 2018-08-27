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

package org.magicdgs.readtools.engine;

import org.magicdgs.readtools.RTBaseTest;
import org.magicdgs.readtools.TestResourcesUtils;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.BufferedLineReader;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTDataSourceUnitTest extends RTBaseTest {

    // minimal header information (at least unsorted order)
    private final static SAMFileHeader EMPTY_HEADER = new SAMFileHeader();
    static {
        EMPTY_HEADER.setSortOrder(SAMFileHeader.SortOrder.unsorted);
    }

    // minimal header for paired-data should be unsorted and grouped by query
    // other possibilities are sorted by queryname
    private final static SAMFileHeader EMPTY_PAIRED_HEADER = new SAMFileHeader();
    static {
        EMPTY_PAIRED_HEADER.setSortOrder(SAMFileHeader.SortOrder.unsorted);
        EMPTY_PAIRED_HEADER.setGroupOrder(SAMFileHeader.GroupOrder.query);
    }

    // the CRAM file requires the header with @SQ lines from reference and MD5
    // thus, read the header here to pass to the test for one file
    // should be the same for all of them...
    private final static SAMFileHeader CRAM_HEADER = SamReaderFactory.makeDefault()
            .referenceSequence(TestResourcesUtils.getWalkthroughDataFile("2L.fragment.fa"))
            .getFileHeader(TestResourcesUtils.getWalkthroughDataFile("standard.dual_index.SE.cram"));

    private final static SAMFileHeader CRAM_HEADER_PAIRED = CRAM_HEADER.clone();
    static {
        CRAM_HEADER_PAIRED.setGroupOrder(SAMFileHeader.GroupOrder.query);
    }

    @BeforeClass
    public void setRTDataSourceReaderFactoryForTests() {
        // this allows CRAM tests by providing a FASTQ file to the data source
        RTDataSource.setReadReaderFactory(new ReadReaderFactory()
                .setReferenceSequence(TestResourcesUtils
                        .getWalkthroughDataFile("2L.fragment.fa").toPath()));
    }

    @AfterClass
    public void revertRTDataSourceReaderFactoryToDefault() {
        // this removes the reference to the RTDataSource to avoid re-usage in following tests
        RTDataSource.setReadReaderFactory(new ReadReaderFactory());
    }

    @Test(expectedExceptions = UserException.class)
    public void testDifferentEncodingForPairEnd() throws Exception {
        final RTDataSource dataSource = new RTDataSource(
                TestResourcesUtils.getWalkthroughDataFile(
                        "legacy.single_index.illumina_quality_1.fq").getAbsolutePath(),
                TestResourcesUtils.getWalkthroughDataFile(
                        "legacy.single_index.paired_1.fq").getAbsolutePath());
        dataSource.getOriginalQualityEncoding();
    }

    @DataProvider(name = "indexed")
    public Object[][] indexedDataSource() {
        final SAMFileHeader header = new SAMTextHeaderCodec().decode(
                BufferedLineReader.fromString("@HD\tVN:1.4\tSO:coordinate\n"
                        + "@SQ\tSN:2L\tLN:59940\n"
                        // TODO: this header should be changed by using 2L.fragment.fa
                        + "@PG\tID:bwa\tPN:bwa\tVN:0.7.12-r1039\tCL:bwa mem fragment.fa SRR1931701_1.fq SRR1931701_2.fq"
                ), "testIndexed");
        return new Object[][] {
                {getTestFile("small.mapped.sort.bam").getAbsolutePath(), FastqQualityFormat.Standard, header, 206, 118}
        };
    }

    @Test(dataProvider = "indexed")
    public void testIntervalQuery(final String source, final FastqQualityFormat format,
            final SAMFileHeader header, final int totalReads, final int readsOn2L)
            throws Exception {
        final RTDataSource dataSource = new RTDataSource(source, false);
        testFormatAndHeader(dataSource, format, header);
        testSingleProvider(dataSource, totalReads);
        int n = 0;
        final Iterator<GATKRead> query = dataSource.query(new SimpleInterval("2L"));
        while (query.hasNext()) {
            n++;
            query.next();
        }
        Assert.assertEquals(n, readsOn2L);
    }

    @DataProvider(name = "paired")
    public Object[][] pairedFilesDataSource() {
        return new Object[][] {
                // pair-end data from the walkthrough in split files
                {TestResourcesUtils
                        .getWalkthroughDataFile("casava.single_index.paired_1.fq"),
                        TestResourcesUtils
                                .getWalkthroughDataFile("casava.single_index.paired_2.fq"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.paired_1.fq"),
                        TestResourcesUtils
                                .getWalkthroughDataFile("legacy.dual_index.paired_2.fq"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.illumina_quality_1.fq"),
                        TestResourcesUtils
                                .getWalkthroughDataFile("legacy.single_index.illumina_quality_2.fq"),
                        FastqQualityFormat.Illumina, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.paired_1.fq"),
                        TestResourcesUtils
                                .getWalkthroughDataFile("legacy.single_index.paired_2.fq"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
        };
    }

    @DataProvider(name = "interleaved")
    public Object[][] interleavedDataSource() {
        return new Object[][] {
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.interleaved.fq"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.interleaved.fq"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.single_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.bam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.bam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.cram"),
                        FastqQualityFormat.Standard, CRAM_HEADER_PAIRED, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.cram"),
                        FastqQualityFormat.Standard, CRAM_HEADER_PAIRED, 103}
        };
    }

    @DataProvider(name = "single")
    public Object[][] singleDataSource() {
        return new Object[][] {
                // real single-end files
                // SAM/BAM/CRAM
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("misencoded.single_index.SE.sam"),
                        FastqQualityFormat.Illumina, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_two_tags.dual_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.single_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.dual_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.SE.bam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.SE.bam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.SE.cram"),
                        FastqQualityFormat.Standard, CRAM_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.SE.cram"),
                        FastqQualityFormat.Standard, CRAM_HEADER, 103},
                // FASTQ files
                {TestResourcesUtils
                        .getWalkthroughDataFile("casava.single_index.SE.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.SE.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.SE.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.illumina_quality.SE.fq"),
                        FastqQualityFormat.Illumina, EMPTY_HEADER, 103},
                // paired files can be always treated as single-end
                // standard BAM/SAM/CRAM - note that the header is the same as the input file (GO)
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.bam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.bam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.cram"),
                        FastqQualityFormat.Standard, CRAM_HEADER_PAIRED, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.cram"),
                        FastqQualityFormat.Standard, CRAM_HEADER_PAIRED, 206},
                // SAM file without GO:query (non-standard)
                {TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.single_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 206},
                // FASTQ files
                {TestResourcesUtils
                        .getWalkthroughDataFile("casava.single_index.paired_1.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("casava.single_index.paired_2.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.interleaved.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.paired_1.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.paired_2.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.illumina_quality_1.fq"),
                        FastqQualityFormat.Illumina, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.illumina_quality_2.fq"),
                        FastqQualityFormat.Illumina, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.interleaved.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.paired_1.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.paired_2.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103}
        };
    }

    @Test(dataProvider = "paired")
    public void testPairedFiles(final File first, final File second,
            final FastqQualityFormat format, final SAMFileHeader expectedHeader,
            final int numberOfPairs) throws Exception {
        final RTDataSource source = new RTDataSource(first.getAbsolutePath(), second.getAbsolutePath());
        testFormatAndHeader(source, format, expectedHeader);
        testPairedProvider(source, numberOfPairs);
        testSingleProvider(source, numberOfPairs * 2);
    }

    @Test(dataProvider = "interleaved")
    public void testInterleavedFiles(final File file, final FastqQualityFormat format,
            final SAMFileHeader expectedHeader, final int numberOfPairs) throws Exception {
        final RTDataSource source = new RTDataSource(file.getAbsolutePath(), true);
        testFormatAndHeader(source, format, expectedHeader);
        testPairedProvider(source, numberOfPairs);
        testSingleProvider(source, numberOfPairs * 2);
    }

    @Test(dataProvider = "single")
    public void testSingleFile(final File file, final FastqQualityFormat format,
            final SAMFileHeader expectedHeader, final int numberOfReads) throws Exception {
        final RTDataSource source = new RTDataSource(file.getAbsolutePath(), false);
        Assert.assertFalse(source.isPaired());
        testFormatAndHeader(source, format, expectedHeader);
        testSingleProvider(source, numberOfReads);
    }

    @Test(dataProvider = "single", expectedExceptions = IllegalArgumentException.class)
    public void testExceptionWhenSingle(final File file, final FastqQualityFormat format,
            final SAMFileHeader expectedHeader, final int numberOfReads) throws Exception {
        final RTDataSource source = new RTDataSource(file.getAbsolutePath(), false);
        source.pairedIterator();
    }

    // test that the format and the header are the same
    // in addition, test if forcing a different format will provide the correct "original" quality
    private static void testFormatAndHeader(final RTDataSource source, final FastqQualityFormat format,
            final SAMFileHeader expectedHeader) throws Exception {
        Assert.assertEquals(source.getOriginalQualityEncoding(), format);
        final SAMFileHeader header = source.getHeader();
        Assert.assertEquals(header, expectedHeader, String.format
                ("Headers (actual vs. expected): \n%s\n\nvs.\n\n%s\n",
                header.getSAMString(), expectedHeader.getSAMString()));
        final FastqQualityFormat incorrectForce = Stream.of(FastqQualityFormat.values())
                .filter(p -> !p.equals(format)).findFirst().orElse(null);
        final RTDataSource forced = new RTDataSource(source, incorrectForce);
        Assert.assertNotEquals(forced.getOriginalQualityEncoding(), format);
        Assert.assertEquals(forced.getOriginalQualityEncoding(), incorrectForce);
        forced.close();
    }

    // test a paired data source
    private static void testPairedProvider(final RTDataSource source, final int numberOfPairs) {
        Assert.assertTrue(source.isPaired());
        int n = 0;
        for (final Tuple2<GATKRead, GATKRead> pair : source.pairedIterator()) {
            // checks that the two reads have equal names
            Assert.assertEquals(pair._1.getName(), pair._2.getName());
            // checks that the first pair have correct paired flags
            Assert.assertTrue(pair._1.isPaired());
            Assert.assertTrue(pair._1.isFirstOfPair());
            Assert.assertFalse(pair._1.isSecondOfPair());
            // checks that the second pair have correct paired flags
            Assert.assertTrue(pair._2.isPaired());
            Assert.assertFalse(pair._2.isFirstOfPair());
            Assert.assertTrue(pair._2.isSecondOfPair());
            n++;
        }
        Assert.assertEquals(n, numberOfPairs, "less pairs than expected");
    }

    // test a single-end provider
    private static void testSingleProvider(final RTDataSource source, final int numberOfReads) {
        int n = 0;
        for (final GATKRead read : source) {
            Assert.assertNotNull(read, "null read from source");
            n++;
        }
        Assert.assertEquals(n, numberOfReads, "less reads than expected");
    }


    @DataProvider(name = "invalidSortOrderPairedData")
    public Object[][] getInvalidSortOrderPairedData() throws IOException {
        return new Object[][] {
                // this BAM file is sorted by coordinate (cannot be used as pair-end)
                {getTestFile("small.mapped.sort.bam")},
                // this SAM file is set to be sorted as duplicate (it is invalid by itself)
                {getTestFile("invalid.duplicate.sorted.sam")}
        };
    }

    @Test(dataProvider = "invalidSortOrderPairedData", expectedExceptions = UserException.BadInput.class)
    public void testInvalidSortOrderForPairedDataSourceGetHeader(final File invalidPairedSortedSource) {
        // TODO: this should throw before getting the header - see https://github.com/magicDGS/ReadTools/issues/329
        final RTDataSource source = new RTDataSource(invalidPairedSortedSource.getAbsolutePath(), true);
        source.getHeader();
    }

    @Test
    public void testQueryNameSortOrderForPairEndWarningsGetHeader() {
        final RTDataSource source = new RTDataSource(getTestFile("queryname.sorted.sam").getAbsolutePath(), true);
        // assert that the queryname sort order isn't changed
        Assert.assertEquals(source.getHeader().getSortOrder(), SAMFileHeader.SortOrder.queryname);
    }

}