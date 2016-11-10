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

package org.magicdgs.readtools.utils.iterators;

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.TextCigarCodec;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class InterleaveGATKReadIteratorsUnitTest extends BaseTest {

    private Iterator<GATKRead> makeReadsIterator(final String name) {
        return Arrays.asList(
                ArtificialReadUtils.createArtificialRead(TextCigarCodec.decode("10M"), name),
                ArtificialReadUtils.createArtificialRead(TextCigarCodec.decode("10M"), name),
                ArtificialReadUtils.createArtificialRead(TextCigarCodec.decode("10M"), name),
                ArtificialReadUtils.createArtificialRead(TextCigarCodec.decode("10M"), name),
                ArtificialReadUtils.createArtificialRead(TextCigarCodec.decode("10M"), name)
        ).iterator();
    }

    @Test
    public void testIterator() throws Exception {
        final InterleaveGATKReadIterators interleaved =
                new InterleaveGATKReadIterators(makeReadsIterator("first"),
                        makeReadsIterator("second"));
        int i = 0;
        for (final GATKRead read : interleaved) {
            if (i % 2 == 0) {
                Assert.assertEquals(read.getName(), "first");
            } else {
                Assert.assertEquals(read.getName(), "second");
            }
            i++;
        }
        Assert.assertEquals(i, 10);
        Assert.assertFalse(interleaved.hasNext());
        try {
            interleaved.iterator().next();
            Assert.fail();
        } catch (NoSuchElementException e) {
            // this is expected
        }
    }

    @Test
    public void testBrokenIterator() throws Exception {
        final List<GATKRead> second = Arrays.asList(ArtificialReadUtils.createArtificialRead("10M"));
        second.forEach(r -> r.setName("second"));
        final InterleaveGATKReadIterators interleaved =
                new InterleaveGATKReadIterators(makeReadsIterator("first"), second.iterator());
        Assert.assertEquals(interleaved.next().getName(), "first");
        Assert.assertEquals(interleaved.next().getName(), "second");
        Assert.assertEquals(interleaved.next().getName(), "first");
        try {
            final GATKRead read = interleaved.next();
            Assert.fail(read.toString());
        } catch (IllegalStateException e) {
            // it should fail when retrieved, but hasNext should return true
            Assert.assertTrue(interleaved.hasNext());
        }
    }

}