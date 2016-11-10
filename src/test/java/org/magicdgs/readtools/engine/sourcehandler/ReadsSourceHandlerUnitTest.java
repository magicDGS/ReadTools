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

package org.magicdgs.readtools.engine.sourcehandler;

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.FastqQualityFormat;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadsSourceHandlerUnitTest extends BaseTest {

    protected final File sourcesFolder = getClassTestDirectory().getParentFile();

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

    @DataProvider(name = "samSources")
    public Iterator<Object[]> samDataSources() {
        final List<Object[]> data = new ArrayList<>();
        // TODO: include BAM and CRAM to check if it is working
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

    private final static SAMFileHeader getHeaderForFile(final File file) {
        try (SamReader reader = SamReaderFactory.makeDefault().open(file)) {
            return reader.getFileHeader();
        } catch (IOException e) {
            // do nothing
        }
        return new SAMFileHeader();
    }

    @Test(dataProvider = "fastqSources")
    public void testFastqSources(final String source, final FastqQualityFormat format,
            final SAMFileHeader header, final int length) throws Exception {
        final ReadsSourceHandler handler = ReadsSourceHandler.getHandler(source);
        Assert.assertEquals(handler.getClass(), FastqSourceHandler.class);
        testHandler(handler, format, header, length);
    }

    @Test(dataProvider = "samSources")
    public void testSamSources(final String source, final FastqQualityFormat format,
            final SAMFileHeader header, final int length) throws Exception {
        final ReadsSourceHandler handler = ReadsSourceHandler.getHandler(source);
        Assert.assertEquals(handler.getClass(), SamSourceHandler.class);
        testHandler(handler, format, header, length);
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