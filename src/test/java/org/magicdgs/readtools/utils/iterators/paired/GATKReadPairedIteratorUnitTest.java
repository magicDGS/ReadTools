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

package org.magicdgs.readtools.utils.iterators.paired;

import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GATKReadPairedIteratorUnitTest extends RTBaseTest {

    private final static SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeader();

    private static List<List<GATKRead>> makeReadList() {
        return Arrays.asList(
                ArtificialReadUtils.createPair(header, "read1", 10, 100, 200, true, false),
                ArtificialReadUtils.createPair(header, "read2", 10, 100, 200, true, false),
                ArtificialReadUtils.createPair(header, "read3", 10, 100, 200, true, false),
                ArtificialReadUtils.createPair(header, "read4", 10, 100, 200, true, false),
                ArtificialReadUtils.createPair(header, "read5", 10, 100, 200, true, false)
        );
    }

    @DataProvider(name = "iteratorArrays")
    public Object[][] constructorDataProvider() {
        final Iterator<GATKRead> emptyIterator = Collections.emptyIterator();
        return new Object[][] {
                {new Iterator[] {emptyIterator, emptyIterator}, SplitGATKReadPairedIterator.class},
                {new Iterator[] {emptyIterator}, InterleavedGATKReadPairedIterator.class}
        };
    }

    @Test(dataProvider = "iteratorArrays")
    public void testStaticProvider(final Iterator<GATKRead>[] iterators, final Class expectedClass)
            throws Exception {
        Assert.assertEquals(GATKReadPairedIterator.of(iterators).getClass(), expectedClass);
    }

    @DataProvider(name = "brokenIteratorArrays")
    public Object[][] badConstructorDataProvider() {
        final Iterator<GATKRead> emptyIterator = Collections.emptyIterator();
        return new Object[][] {
                {null},
                {new Iterator[] {}},
                {new Iterator[] {emptyIterator, emptyIterator, emptyIterator}},
                {new Iterator[] {emptyIterator, emptyIterator, emptyIterator, emptyIterator}}
        };
    }

    @Test(dataProvider = "brokenIteratorArrays", expectedExceptions = IllegalArgumentException.class)
    public void testStaticProviderBadArgs(final Iterator<GATKRead>[] iterators) throws Exception {
        GATKReadPairedIterator.of(iterators);
    }

    @Test
    public void testSplitGATKReadPairedIterator() throws Exception {
        // expected values
        final List<List<GATKRead>> expected = makeReadList();
        final Iterator<Tuple2<GATKRead, GATKRead>> expectedIt = expected.stream()
                .map(l -> new Tuple2<>(l.get(0), l.get(1))).iterator();
        // the iterator
        final Iterator<GATKRead> reads1 = expected.stream().map(l -> l.get(0)).iterator();
        final Iterator<GATKRead> reads2 = expected.stream().map(l -> l.get(1)).iterator();
        final SplitGATKReadPairedIterator iterator =
                new SplitGATKReadPairedIterator(reads1, reads2);
        testPairIterator(iterator, expectedIt);
    }

    @Test
    public void testInterleavedGATKReadPairedIterator() throws Exception {
        final List<List<GATKRead>> expected = makeReadList();
        final Iterator<Tuple2<GATKRead, GATKRead>> expectedIt = expected.stream()
                .map(l -> new Tuple2<>(l.get(0), l.get(1))).iterator();
        final InterleavedGATKReadPairedIterator interleaved =
                new InterleavedGATKReadPairedIterator(expected.stream()
                        .flatMap(Collection::stream).iterator());

        testPairIterator(interleaved, expectedIt);
    }

    // should be exactly the same read, because no deep copy is performed
    private static void testPairIterator(final GATKReadPairedIterator iterator,
            final Iterator<Tuple2<GATKRead, GATKRead>> expectedIt) {
        for (final Tuple2<GATKRead, GATKRead> pair : iterator) {
            Assert.assertEquals(pair, expectedIt.next());
        }
        Assert.assertFalse(iterator.hasNext());
        try {
            final Tuple2<GATKRead, GATKRead> none = iterator.next();
            Assert.fail(none.toString());
        } catch (NoSuchElementException e) {
            // this is expected
        }
    }

    @Test
    public void testBrokenInterleavedGATKReadPairedIterator() throws Exception {
        final List<GATKRead> reads = new ArrayList<>(
                ArtificialReadUtils.createPair(header, "read1", 10, 100, 200, true, false));
        final GATKRead unpaired = ArtificialReadUtils.createArtificialRead("10M");
        reads.add(unpaired);
        final InterleavedGATKReadPairedIterator iterator =
                new InterleavedGATKReadPairedIterator(reads.iterator());
        // the first is correctly read
        Assert.assertEquals(iterator.next(), new Tuple2<>(reads.get(0), reads.get(1)));
        testBrokenIterator(iterator);
    }

    @Test
    public void testBrokenSplitGATKReadPairedIterator() {
        // initialize with an empty iterator
        final List<GATKRead> singleRead =
                Collections.singletonList(ArtificialReadUtils.createArtificialRead("10M"));
        // first iterator exhausted
        testBrokenIterator(new SplitGATKReadPairedIterator(singleRead.iterator(),
                Collections.emptyIterator()));
        // second iterator exhausted
        testBrokenIterator(new SplitGATKReadPairedIterator(Collections.emptyIterator(),
                singleRead.iterator()));
    }

    // test if the iterator is broken -> hasNext is true, but the pair is broken
    // this should throw an IllegalStateException
    private static void testBrokenIterator(final GATKReadPairedIterator pairIterator) {
        Assert.assertTrue(pairIterator.hasNext());
        try {
            final Tuple2<GATKRead, GATKRead> broken = pairIterator.next();
            Assert.fail(broken.toString());
        } catch (IllegalStateException e) {
            // this is expected
        }
    }
}