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

package org.magicdgs.readtools.utils.iterators;

import org.magicdgs.readtools.RTBaseTest;
import org.magicdgs.readtools.utils.fastq.FastqGATKRead;

import htsjdk.samtools.fastq.FastqRecord;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RecordToReadIteratorUnitTest extends RTBaseTest {

    private final Function<FastqRecord, GATKRead> fastqEncoder = FastqGATKRead::new;

    @DataProvider(name = "iterators")
    public Object[][] recordProvider() throws Exception {
        // list to iterate
        final List<FastqRecord> records = Arrays.asList(
                new FastqRecord("read0", "ACTG", null, "FFFF"),
                new FastqRecord("read1", "TTCC", "comment", "5555"),
                new FastqRecord("read2#ACTG/2", "ACAG", "comment2", "F5II")
        );

        // expected reads
        final GATKRead read0 = ArtificialReadUtils.createArtificialUnmappedRead(
                null, new byte[] {'A', 'C', 'T', 'G'},
                new byte[] {37, 37, 37, 37});
        read0.setName("read0");
        final GATKRead read1 = ArtificialReadUtils.createArtificialUnmappedRead(
                null, new byte[] {'T', 'T', 'C', 'C'},
                new byte[] {20, 20, 20, 20});
        read1.setName("read1");
        read1.setAttribute("CO", "comment");
        final GATKRead read2 = ArtificialReadUtils.createArtificialUnmappedRead(
                null, new byte[] {'A', 'C', 'A', 'G'},
                new byte[] {37, 20, 40, 40});
        read2.setName("read2");
        read2.setIsSecondOfPair();
        read2.setMateIsUnmapped();
        read2.setAttribute("CO", "comment2");
        read2.setAttribute("BC", "ACTG");
        final List<GATKRead> expectedReads = Arrays.asList(read0, read1, read2);

        // TODO: add more iterators?
        return new Object[][] {
                {records.iterator(), fastqEncoder, expectedReads}
        };
    }

    @Test(dataProvider = "iterators")
    public <T> void testNextHasNext(final Iterator<T> recordIterator,
            final Function<T, GATKRead> encoder,
            final List<GATKRead> expectedReads) throws Exception {
        final RecordToReadIterator<T> it = new RecordToReadIterator(recordIterator, encoder);
        for (int i = 0; i < expectedReads.size(); i++) {
            Assert.assertTrue(it.hasNext());
            // testing as as SAMRecord because GATKRead.equals only considers Object equality
            Assert.assertEquals(it.next().convertToSAMRecord(null),
                    expectedReads.get(i).convertToSAMRecord(null), "failed read" + i);
        }
        Assert.assertFalse(it.hasNext());
        try {
            it.next();
            Assert.fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {
            // this means that the test pass
        }
    }

    @Test(dataProvider = "iterators")
    public <T> void testIterator(final Iterator<T> recordIterator,
            final Function<T, GATKRead> encoder,
            final List<GATKRead> expectedReads) throws Exception {
        final RecordToReadIterator<T> it = new RecordToReadIterator(recordIterator, encoder);
        final Iterator<GATKRead> expected = expectedReads.iterator();
        for (final GATKRead read : it) {
            // testing as as SAMRecord because GATKRead.equals only considers Object equality
            Assert.assertEquals(read.convertToSAMRecord(null),
                    expected.next().convertToSAMRecord(null));
        }
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void testNullNestedIterator() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new RecordToReadIterator(null, fastqEncoder));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RecordToReadIterator(Collections.emptyIterator(), null));
    }

}