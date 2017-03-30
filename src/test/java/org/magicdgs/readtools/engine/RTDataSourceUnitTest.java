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

import org.magicdgs.readtools.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.StringLineReader;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.Tuple2;

import java.io.File;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTDataSourceUnitTest extends BaseTest {

    private final static SAMFileHeader minimalPairedHeader = new SAMFileHeader();

    static {
        // TODO: header for paired-data will be always sorted by queryname
        // TODO: now we have some limitations
        minimalPairedHeader.setSortOrder(SAMFileHeader.SortOrder.unsorted);
    }

    private final static SAMFileHeader samHeader = new SAMTextHeaderCodec().decode(
            new StringLineReader(
                    "@HD\tVN:1.4\tSO:unsorted\n"
                            + "@RG\tID:SRR1931701\tPU:SRR1931701\tLB:1\tSM:SRR1931701\tPL:ILLUMINA\n"
                            + "@PG\tID:SCS\tVN:2.2.58\tPN:HiSeq Control Software\tDS:Controlling software on instrument\n"
                            + "@PG\tID:basecalling\tPP:SCS\tVN:1.18.64.0\tPN:RTA\tDS:Basecalling Package\n"
                            + "@PG\tID:bcl2fastq\tVN:2.16.0\tPN:bcl2fastq\n"
                            + "@PG\tID:fastq2bam\tVN:0.3\tPN:fastq2bam"
            ), "test");

    private String getSource(final String fileName) {
        return new File(getClassTestDirectory(), fileName).getAbsolutePath();
    }

    @Test(expectedExceptions = UserException.class)
    public void testDifferentEncodingForPairEnd() throws Exception {
        final RTDataSource dataSource =
                new RTDataSource(getSource("small_1.illumina.fq"), getSource("small.paired.sam"));
        dataSource.getOriginalQualityEncoding();
    }

    @DataProvider(name = "indexed")
    public Object[][] indexedDataSource() {
        final SAMFileHeader header = new SAMTextHeaderCodec().decode(
                new StringLineReader("@HD\tVN:1.4\tSO:coordinate\n"
                        + "@SQ\tSN:2L\tLN:59940\n"
                        + "@PG\tID:bwa\tPN:bwa\tVN:0.7.12-r1039\tCL:bwa mem fragment.fa SRR1931701_1.fq SRR1931701_2.fq"
                ), "testIndexed");
        return new Object[][] {
                {getSource("small.mapped.sort.bam"), FastqQualityFormat.Standard, header, 206, 118}
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
                // pair-end illumina
                {getSource("small_1.illumina.fq"), getSource("small_2.illumina.fq"),
                        FastqQualityFormat.Illumina, minimalPairedHeader, 5}
        };
    }

    @DataProvider(name = "interleaved")
    public Object[][] interleavedDataSource() {
        final SAMFileHeader header = samHeader.clone();
        // clone is broken and change the version, so change it here too
        header.setAttribute(SAMFileHeader.VERSION_TAG, samHeader.getVersion());
        // TODO: paired data should be always sorted by queryname at some point
        // currently we have the limitation that the queryname sorting by other tools
        // cannot be checked, so we assume unsorted
        header.setSortOrder(SAMFileHeader.SortOrder.unsorted);
        return new Object[][] {
                // this is really paired
                {getSource("small.paired.sam"), FastqQualityFormat.Standard, header, 4},
                {getSource("small.interleaved.illumina.fq"), FastqQualityFormat.Illumina,
                        minimalPairedHeader, 5}
        };
    }

    @DataProvider(name = "single")
    public Object[][] singleDataSource() {
        final SAMFileHeader emptyHeader = new SAMFileHeader();
        return new Object[][] {
                // this is really single end
                {getSource("small.single.sam"), FastqQualityFormat.Standard, samHeader, 4},
                // this are not single end, but should be treated as is
                {getSource("small_1.illumina.fq"), FastqQualityFormat.Illumina, emptyHeader, 5},
                {getSource("small_2.illumina.fq"), FastqQualityFormat.Illumina, emptyHeader, 5},
                {getSource("small.paired.sam"), FastqQualityFormat.Standard, samHeader, 8}
        };
    }

    @Test(dataProvider = "paired")
    public void testPairedFiles(final String first, final String second,
            final FastqQualityFormat format, final SAMFileHeader expectedHeader,
            final int numberOfPairs) throws Exception {
        final RTDataSource source = new RTDataSource(first, second);
        testFormatAndHeader(source, format, expectedHeader);
        testPairedProvider(source, numberOfPairs);
        testSingleProvider(source, numberOfPairs * 2);
    }

    @Test(dataProvider = "interleaved")
    public void testInterleavedFiles(final String file, final FastqQualityFormat format,
            final SAMFileHeader expectedHeader, final int numberOfPairs) throws Exception {
        final RTDataSource source = new RTDataSource(file, true);
        testFormatAndHeader(source, format, expectedHeader);
        testPairedProvider(source, numberOfPairs);
        testSingleProvider(source, numberOfPairs * 2);
    }

    @Test(dataProvider = "single")
    public void testSingleFile(final String file, final FastqQualityFormat format,
            final SAMFileHeader expectedHeader, final int numberOfReads) throws Exception {
        final RTDataSource source = new RTDataSource(file, false);
        Assert.assertFalse(source.isPaired());
        testFormatAndHeader(source, format, expectedHeader);
        testSingleProvider(source, numberOfReads);
    }

    @Test(dataProvider = "single", expectedExceptions = IllegalArgumentException.class)
    public void testExceptionWhenSingle(final String file, final FastqQualityFormat format,
            final SAMFileHeader expectedHeader, final int numberOfReads) throws Exception {
        final RTDataSource source = new RTDataSource(file, false);
        source.pairedIterator();
    }

    // test that the format and the header are the same
    // in addition, test if forcing a different format will provide the correct "original" quality
    private static void testFormatAndHeader(final RTDataSource source, final FastqQualityFormat format,
            final SAMFileHeader expectedHeader) throws Exception {
        Assert.assertEquals(source.getOriginalQualityEncoding(), format);
        Assert.assertEquals(source.getHeader(), expectedHeader);
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
            // checks that the seconf pair have correct paired flags
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
            n++;
        }
        Assert.assertEquals(n, numberOfReads, "less reads than expected");
    }

}