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

package org.magicdgs.readtools.utils.read.writer;

import org.magicdgs.readtools.utils.iterators.FastqToReadIterator;
import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqWriterFactory;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqGATKWriterUnitTest extends RTBaseTest {

    @DataProvider(name = "readsToWrite")
    public Object[][] getReadList() throws Exception {
        // expected reads
        final GATKRead read0 = ArtificialReadUtils.createArtificialUnmappedRead(
                null, new byte[] {'A', 'C', 'T', 'G'},
                new byte[] {37, 37, 37, 37});
        read0.setName("read0");
        final GATKRead read1 = ArtificialReadUtils.createArtificialUnmappedRead(
                null, new byte[] {'T', 'T', 'C', 'C'},
                new byte[] {20, 20, 20, 20});
        read1.setName("read1");
        read1.setIsFirstOfPair();
        read1.setMateIsUnmapped();
        read1.setAttribute("CO", "comment");
        final GATKRead read2 = ArtificialReadUtils.createArtificialUnmappedRead(
                null, new byte[] {'A', 'C', 'A', 'G'},
                new byte[] {37, 20, 40, 40});
        read2.setName("read2");
        read2.setIsSecondOfPair();
        read2.setMateIsUnmapped();
        read2.setAttribute("CO", "comment2");
        read2.setAttribute("BC", "ACTG");

        // TODO: add more lists?
        return new Object[][] {
                {Arrays.asList(read0, read1, read2)}
        };
    }

    @Test(dataProvider = "readsToWrite")
    public void testWritingReading(final List<GATKRead> readsToWrite) throws Exception {
        final File tempFile = IOUtils.createTempFile("testWriting", "fastq");
        final FastqGATKWriter writer = new FastqGATKWriter(
                new FastqWriterFactory().newWriter(tempFile));
        readsToWrite.forEach(writer::addRead);
        writer.close();
        // now check if reading is the same
        final FastqReader reader = new FastqReader(tempFile);
        final Iterator<GATKRead> iterator = new FastqToReadIterator(reader.iterator());
        readsToWrite.forEach(r -> Assert.assertEquals(iterator.next().convertToSAMRecord(null),
                r.convertToSAMRecord(null)));
        Assert.assertFalse(iterator.hasNext());
        reader.close();
    }

}