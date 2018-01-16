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

package org.magicdgs.readtools.engine.sourcehandler;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.TestResourcesUtils;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;
import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadsSourceHandlerUnitTest extends RTBaseTest {

    private final static SimpleInterval INTERVAL_TO_QUERY = new SimpleInterval("2L");

    private final File sourcesFolder = getClassTestDirectory().getParentFile();

    // this is the factory for tests, including reference sequence for CRAM
    private final ReadReaderFactory FACTORY_FOR_TEST = new ReadReaderFactory()
            .setReferenceSequence(TestResourcesUtils.getWalkthroughDataFile("2L.fragment.fa"));

    // empty header for Walkthrough data - we do not expect headers to contain more information
    private final static SAMFileHeader EMPTY_HEADER = new SAMFileHeader();
    static  {
        EMPTY_HEADER.setSortOrder(SAMFileHeader.SortOrder.unsorted);
    }

    // empty header for standard Walkthrough data - includes also GO:query
    private final static SAMFileHeader EMPTY_PAIRED_HEADER = EMPTY_HEADER.clone();
    static {
        EMPTY_PAIRED_HEADER.setGroupOrder(SAMFileHeader.GroupOrder.query);
    }

    @Test(expectedExceptions = UserException.CouldNotReadInputFile.class)
    public void testNotFoundHandler() throws Exception {
        ReadsSourceHandler.getHandler("unknown", FACTORY_FOR_TEST);
    }

    @DataProvider(name = "fastqSources")
    public Object[][] fastqDataSources() {
        return new Object[][] {
                {TestResourcesUtils
                        .getWalkthroughDataFile("casava.single_index.SE.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("casava.single_index.paired_1.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("casava.single_index.paired_2.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.SE.fq"),
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
                        .getWalkthroughDataFile("legacy.single_index.SE.fq"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.illumina_quality.SE.fq"),
                        FastqQualityFormat.Illumina, EMPTY_HEADER, 103},
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

    @DataProvider(name = "samSourcesNoIndex")
    public Object[][] samDataSources() {
        // header for CRAM files should be the same for all
        // this is required because the @SQ lines are included in this header
        final SAMFileHeader cramHeader = getHeaderForFile(TestResourcesUtils
                .getWalkthroughDataFile("standard.dual_index.SE.cram"));
        final SAMFileHeader cramPairedHeader = cramHeader.clone();
        cramPairedHeader.setGroupOrder(SAMFileHeader.GroupOrder.query);

        // for mapped files, extract the header from the file for testing
        final SAMFileHeader singleIndexMappedHeader = getHeaderForFile(TestResourcesUtils
                .getWalkthroughDataFile("legacy.single_index.paired.mapped.sam"));
        final SAMFileHeader dualIndexMappedHeader = getHeaderForFile(TestResourcesUtils
                .getWalkthroughDataFile("legacy.dual_index.paired.mapped.sam"));
        // and for CRAM, it has a different SQ line
        final SAMFileHeader singleIndexMappedCramHeader = getHeaderForFile(TestResourcesUtils
                .getWalkthroughDataFile("legacy.single_index.paired.mapped.cram"));
        final SAMFileHeader dualIndexMappedCramHeader = getHeaderForFile(TestResourcesUtils
                .getWalkthroughDataFile("legacy.dual_index.paired.mapped.cram"));

        return new Object[][] {
                // SAM files
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("misencoded.single_index.SE.sam"),
                        FastqQualityFormat.Illumina, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_two_tags.dual_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils.getWalkthroughDataFile("bc_in_two_tags.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.single_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.single_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.dual_index.SE.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("bc_in_read_name.dual_index.paired.sam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 206},
                // BAM files
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.SE.bam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.bam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.SE.bam"),
                        FastqQualityFormat.Standard, EMPTY_HEADER, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.bam"),
                        FastqQualityFormat.Standard, EMPTY_PAIRED_HEADER, 206},
                // CRAM files
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.SE.cram"),
                        FastqQualityFormat.Standard, cramHeader, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.dual_index.paired.cram"),
                        FastqQualityFormat.Standard, cramPairedHeader, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.SE.cram"),
                        FastqQualityFormat.Standard, cramHeader, 103},
                {TestResourcesUtils
                        .getWalkthroughDataFile("standard.single_index.paired.cram"),
                        FastqQualityFormat.Standard, cramPairedHeader, 206},
                // mapped files
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.paired.mapped.sam"),
                        FastqQualityFormat.Standard, singleIndexMappedHeader, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.paired.mapped.bam"),
                        FastqQualityFormat.Standard, singleIndexMappedHeader, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.single_index.paired.mapped.cram"),
                        FastqQualityFormat.Standard, singleIndexMappedCramHeader, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.paired.mapped.sam"),
                        FastqQualityFormat.Standard, dualIndexMappedHeader, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.paired.mapped.bam"),
                        FastqQualityFormat.Standard, dualIndexMappedHeader, 206},
                {TestResourcesUtils
                        .getWalkthroughDataFile("legacy.dual_index.paired.mapped.cram"),
                        FastqQualityFormat.Standard, dualIndexMappedCramHeader, 206}

        };
    }

    @DataProvider(name = "samSourcesIndexed")
    public Iterator<Object[]> samDataSourcesIndexed() {
        final List<Object[]> data = new ArrayList<>();
        // mapped files
        final String[] fileNames =
                new String[] {"small.mapped.sort.bam", "small.mapped.sort.cram"};
        // all the files comes from the same
        for (final String files : fileNames) {
            final File file = new File(sourcesFolder, files);
            data.add(new Object[] {file.getAbsolutePath(), FastqQualityFormat.Standard,
                    getHeaderForFile(file), 206, 118});
        }
        return data.iterator();
    }

    private final SAMFileHeader getHeaderForFile(final File file) {
        try (final SamReader reader = FACTORY_FOR_TEST.openSamReader(file)) {
            return reader.getFileHeader();
        } catch (IOException e) {
            // do nothing
        }
        return new SAMFileHeader();
    }

    @Test(dataProvider = "fastqSources")
    public void testFastqSources(final File source, final FastqQualityFormat format,
            final SAMFileHeader header, final int length) throws Exception {
        final ReadsSourceHandler handler = ReadsSourceHandler.getHandler(source.getAbsolutePath(), FACTORY_FOR_TEST);
        Assert.assertEquals(handler.getClass(), FastqSourceHandler.class);
        testHandler(handler, format, header, length);
        // fastq files could not be iterated
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> handler.toIntervalIterator(Collections.singletonList(INTERVAL_TO_QUERY)));
    }

    @Test(dataProvider = "samSourcesNoIndex")
    public void testSamSourcesWithoutIndex(final File source, final FastqQualityFormat format,
            final SAMFileHeader header, final int length) throws Exception {
        final ReadsSourceHandler handler = ReadsSourceHandler.getHandler(source.getAbsolutePath(), FACTORY_FOR_TEST);
        Assert.assertEquals(handler.getClass(), SamSourceHandler.class);
        testHandler(handler, format, header, length);
        // sources without index could not be iterated with interval iteration
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> handler.toIntervalIterator(Collections.singletonList(INTERVAL_TO_QUERY)));
    }

    @Test(dataProvider = "samSourcesIndexed")
    public void testSamSourcesWithIndex(final String source, final FastqQualityFormat format,
            final SAMFileHeader header, final int length, final int length2L) throws Exception {
        final ReadsSourceHandler handler = new SamSourceHandler(source, FACTORY_FOR_TEST);
        Assert.assertEquals(handler.getClass(), SamSourceHandler.class);
        testHandler(handler, format, header, length);
        // sources with index should be tested
        final Iterator<GATKRead> itInterval =
                handler.toIntervalIterator(Collections.singletonList(INTERVAL_TO_QUERY));
        int n = 0;
        while (itInterval.hasNext()) {
            n++;
            itInterval.next();
        }
        Assert.assertEquals(n, length2L);
    }

    private static void testHandler(final ReadsSourceHandler handler,
            final FastqQualityFormat format,
            final SAMFileHeader header, final int length) throws Exception {
        Assert.assertEquals(handler.getQualityEncoding(100), format);
        final SAMFileHeader firstHeader = handler.getHeader();
        Assert.assertEquals(firstHeader, header);
        Assert.assertNotSame(handler.getHeader(), firstHeader);
        // check if two iterators could be open at the same time and start from the beginning of the file
        Assert.assertEquals(handler.toStream().count(), length);
        Assert.assertEquals(handler.toStream().count(), length);
        // test that they close correctly without throwing exceptions
        handler.close();
    }

    @Test
    public void testCorruptedFileHandler() throws Exception {
        // TODO - this file is truncated but too big (maybe Rupert can create a better one)
        final File testFile = new File(sourcesFolder, "premature_end.bam");
        Assert.assertTrue(testFile.exists(), "Test file does not exists: " + testFile);
        final ReadsSourceHandler handler = new SamSourceHandler(testFile.getAbsolutePath());
        // System.err.println(handler.getQualityEncoding(RTDefaults.MAX_RECORDS_FOR_QUALITY));
        handler.close();
        Assert.assertThrows(UserException.CouldNotReadInputFile.class,
                () -> handler.getQualityEncoding(RTDefaults.MAX_RECORDS_FOR_QUALITY));
    }
}