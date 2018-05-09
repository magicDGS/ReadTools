/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.utils.mappability.gem;

import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.util.BufferedLineReader;
import org.apache.commons.lang3.Range;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GemMappabilityReaderUnitTest extends RTBaseTest {

    private static final Map<Byte, Range<Integer>> TEST_ENCODING = new HashMap<>(10);

    private static final GemMappabilityHeader getTestHeader() {
        return new GemMappabilityHeader(100, 7, 4, 4, 15, 80, 1, TEST_ENCODING);
    }

    private static String getAllBytesForContigRecord(final String contig) {
        final StringBuilder builder = new StringBuilder("~").append(contig).append("\n");
        TEST_ENCODING.keySet().forEach(b -> builder.append((char) b.byteValue()));
        return builder.toString();
    }

    /**
     * Sets up the test header test encoding.
     */
    @BeforeClass
    private void setup() {
        // fill in with simple ranges
        for(byte i = 32; i <= 42; i++) {
            TEST_ENCODING.put(i, Range.is((int) i - 31));
        }
        System.err.println(TEST_ENCODING);
    }

    @Test
    private void testIterationSingleContig() {
        final String contig = "chr1";
        final GemMappabilityReader reader = new GemMappabilityReader(
                BufferedLineReader.fromString(getAllBytesForContigRecord(contig)),
                getTestHeader());

        testEncodingIterationSingleContig(contig, reader);

        // test exhausted iteration
        Assert.assertFalse(reader.hasNext());

        // test failure of next
        Assert.assertThrows(NoSuchElementException.class, reader::next);
    }

    private static void testEncodingIterationSingleContig(final String contig,
            final GemMappabilityReader reader) {
        // first, assert that the iterator is not empty
        Assert.assertTrue(reader.hasNext());

        // then assert every value of iteration
        final AtomicInteger i = new AtomicInteger(0);
        TEST_ENCODING.keySet().forEach(b -> {
            final GemMappabilityRecord next = reader.next();
            Assert.assertNotNull(next);
            Assert.assertEquals(next.getContig(), contig);
            Assert.assertEquals(next.getStart(), i.incrementAndGet());
            Assert.assertTrue(next.getRange().contains((i.get())));
        });
    }
}