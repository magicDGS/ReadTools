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

import org.magicdgs.readtools.utils.read.ReadReaderFactory;
import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.FastqQualityFormat;
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

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadsSourceHandlerUnitTest extends BaseTest {

    private final static SimpleInterval INTERVAL_TO_QUERY = new SimpleInterval("2L");

    private final File sourcesFolder = getClassTestDirectory().getParentFile();

    // this is the factory for tests, including reference sequence for CRAM
    private final ReadReaderFactory FACTORY_FOR_TEST = new ReadReaderFactory()
            .setReferenceSequence(new File(sourcesFolder, "2L.fragment.fa"));

    @DataProvider(name = "fastqSources")
    public Object[][] fastqDataSources() {
        final SAMFileHeader emptyHeader = new SAMFileHeader();
        return new Object[][] {
                {new File(sourcesFolder, "small.illumina.fq").getAbsolutePath(),
                        FastqQualityFormat.Illumina, emptyHeader, 25},
                {new File(sourcesFolder, "small.sanger.fq").getAbsolutePath(),
                        FastqQualityFormat.Standard, emptyHeader, 14}
        };
    }

    @DataProvider(name = "samSourcesNoIndex")
    public Iterator<Object[]> samDataSources() {
        final List<Object[]> data = new ArrayList<>();
        // mapped files
        final String[] mapped =
                new String[] {"small.mapped.sam", "small.mapped.bam", "small.mapped.cram"};
        final String[] unmapped =
                new String[] {"small.unmapped.sam", "small.unmapped.bam", "small.unmapped.cram"};
        // all the files comes from the same
        for (final String files : unmapped) {
            final File file = new File(sourcesFolder, files);
            data.add(new Object[] {file.getAbsolutePath(), FastqQualityFormat.Standard,
                    getHeaderForFile(file), 206});
        }
        for (final String files : mapped) {
            final File file = new File(sourcesFolder, files);
            data.add(new Object[] {file.getAbsolutePath(), FastqQualityFormat.Standard,
                    getHeaderForFile(file), 206});
        }
        return data.iterator();
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
    public void testFastqSources(final String source, final FastqQualityFormat format,
            final SAMFileHeader header, final int length) throws Exception {
        final ReadsSourceHandler handler = ReadsSourceHandler.getHandler(source, FACTORY_FOR_TEST);
        Assert.assertEquals(handler.getClass(), FastqSourceHandler.class);
        testHandler(handler, format, header, length);
        // fastq files could not be iterated
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> handler.toIntervalIterator(Collections.singletonList(INTERVAL_TO_QUERY)));
    }

    @Test(dataProvider = "samSourcesNoIndex")
    public void testSamSourcesWithoutIndex(final String source, final FastqQualityFormat format,
            final SAMFileHeader header, final int length) throws Exception {
        final ReadsSourceHandler handler = ReadsSourceHandler.getHandler(source, FACTORY_FOR_TEST);
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
}