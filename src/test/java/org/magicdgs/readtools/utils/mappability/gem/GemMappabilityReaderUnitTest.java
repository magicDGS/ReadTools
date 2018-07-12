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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GemMappabilityReaderUnitTest extends RTBaseTest {

    // ordered map
    private static final TreeMap<Byte, Range<Integer>> TEST_ENCODING = new TreeMap<>();


    private static final GemMappabilityHeader getTestHeader() {
        return new GemMappabilityHeader(100, 7, 4, 4, 15, 80, 1, TEST_ENCODING);
    }

    private static String getAllBytesForContigRecord(final String contig) {
        final StringBuilder builder = new StringBuilder("~").append(contig).append("\n");
        TEST_ENCODING.keySet().forEach(b -> builder.append((char) b.byteValue()));
        return builder.toString();
    }

    private static StringBuilder createMetadataString(final GemMappabilityHeader header) {
        return new StringBuilder()
                .append("~~K-MER LENGTH\n")
                .append(header.getKmerLength()).append("\n")
                .append("~~APPROXIMATION THRESHOLD\n")
                .append(header.getApproximationThreshold()).append("\n")
                .append("~~MAX MISMATCHES\n")
                .append(header.getMaxMismatches()).append("\n")
                .append("~~MAX ERRORS\n")
                .append(header.getMaxErrors()).append("\n")
                .append("~~MAX BIG INDEL LENGTH\n")
                .append(header.getMaxBigIndelLength()).append("\n")
                .append("~~MIN MATCHED BASES\n")
                .append(header.getMinMatchedBases()).append("\n")
                .append("~~STRATA AFTER BEST\n")
                .append(header.getStrataAfterBest()).append("\n");
    }

    /**
     * Sets up the test header test encoding.
     */
    @BeforeClass
    public void setup() {
        // fill in with simple ranges
        for(byte i = 32; i <= 42; i++) {
            TEST_ENCODING.put(i, Range.is((int) i - 31));
        }
    }

    @DataProvider
    public Object[][] wrongHeaders() {
        final GemMappabilityHeader header = getTestHeader();
        return new Object[][] {
                // empty file
                {""},
                // completely different header
                {"# comment header"},
                // no header, only sequence
                {"~chr1\n!!!"},
                // empty first header (no new line)
                {"~~K-MER LENGTH"},
                // no encoding header
                {createMetadataString(header).toString()},
                // only encoding header
                {"~~ENCODING"},
                // empty encoding header
                {createMetadataString(header).append("~~ENCODING")},
                // wrong format for the encoding line
                {createMetadataString(header).append("~~ENCODING\n'hello'~[a,b]")}
        };
    }

    @Test(dataProvider = "wrongHeaders", expectedExceptions = GemMappabilityException.class)
    public void testWrongHeader(final CharSequence wrongHeader) throws Exception {
        // create temp file
        final Path path = Files.createTempFile("gem" + wrongHeader.hashCode(), ".mappability");
        Files.write(path, Collections.singletonList(wrongHeader));

        // open the reader should fail because of the wrong header
        final GemMappabilityReader reader = new GemMappabilityReader(path);
    }

    @Test
    public void testReadSimpleFile() throws Exception {
        final String contig = "chr1";
        final GemMappabilityHeader header = getTestHeader();

        // create the header string with the values from the header
        final StringBuilder headerString = createMetadataString(header);
        // add the encoding header
        headerString.append("~~ENCODING").append("\n");
        TEST_ENCODING.forEach((b, r) -> headerString
                .append(String.format("'%c'~[%s-%s]", b, r.getMinimum(), r.getMaximum()))
                .append("\n"));
        // test the read header
        headerString.append("~").append(contig).append("\n")
            .append((char) TEST_ENCODING.firstKey().byteValue()).append("\n");

        // and write it up in a test file
        final Path path = Files.createTempFile("simple", "gem.mappability");
        Files.write(path, Collections.singletonList(headerString.toString()));

        // open reader
        final GemMappabilityReader reader = new GemMappabilityReader(path);
        // check the header
        Assert.assertEquals(reader.getHeader(), header);
        // check the only entry
        final GemMappabilityRecord record = reader.next();
        testSingleRecord(record, contig, 1,
                TEST_ENCODING.firstEntry().getValue().getMinimum(),
                TEST_ENCODING.firstEntry().getValue().getMaximum());

    }

    @Test
    public void testIterationSingleContig() {
        final String contig = "chr1";
        final GemMappabilityReader reader = new GemMappabilityReader(
                BufferedLineReader.fromString(getAllBytesForContigRecord(contig)),
                getTestHeader());

        // test iteration
        testEncodingIterationSingleContig(contig, reader);

        // test end of iteration
        testExhaustedIterator(reader);

        // test close
        testClose(reader);
    }

    @Test
    public void testIterationTwoContigs() {
        final String contig1 = "chr1";
        final String contig2 = "chr2";
        final GemMappabilityReader reader = new GemMappabilityReader(
                BufferedLineReader.fromString( getAllBytesForContigRecord(contig1) + getAllBytesForContigRecord(contig2)),
                getTestHeader()
        );

        // test iteration
        testEncodingIterationSingleContig(contig1, reader);
        testEncodingIterationSingleContig(contig2, reader);

        // test end of iteration
        testExhaustedIterator(reader);

        // test close
        testClose(reader);
    }

    @Test
    public void testInvalidEncodedChar() {
        final String contig = "chr1";
        final GemMappabilityReader reader = new GemMappabilityReader(
                BufferedLineReader.fromString(getAllBytesForContigRecord(contig) + TEST_ENCODING.lastKey() + 1),
                getTestHeader()
        );

        // test iteration until the last
        testEncodingIterationSingleContig(contig, reader);

        // now it should throw because there is a problem wiht the encoding
        Assert.assertThrows(GemMappabilityException.class, reader::next);
    }

    private static void testEncodingIterationSingleContig(final String contig,
            final GemMappabilityReader reader) {
        // first, assert that the iterator is not empty
        Assert.assertTrue(reader.hasNext());

        // then assert every value of iteration
        final AtomicInteger i = new AtomicInteger(0);
        TEST_ENCODING.keySet().forEach(b -> {
            i.incrementAndGet();
            final GemMappabilityRecord next = reader.next();

            // test iteration variables
            Assert.assertEquals(reader.getCurrentSequence(), contig);
            Assert.assertEquals(reader.getCurrentSequencePosition(), i.get());

            // test the record
            testSingleRecord(next, contig, i.get(), i.get(), i.get());
        });
    }

    private static void testSingleRecord(final GemMappabilityRecord record,
            final String contig, final int start, final int min, final int max) {
        Assert.assertNotNull(record);
        Assert.assertEquals(record.getContig(), contig);
        Assert.assertEquals(record.getStart(), start);
        Assert.assertEquals(record.getRange().getMinimum().intValue(), min);
        Assert.assertEquals(record.getRange().getMaximum().intValue(), max);
    }

    private static void testExhaustedIterator(final GemMappabilityReader reader) {
        // test exhausted iteration
        Assert.assertFalse(reader.hasNext());

        // test failure of next
        Assert.assertThrows(NoSuchElementException.class, reader::next);
    }

    private static void testClose(final GemMappabilityReader reader) {
        // close iterator
        reader.close();

        // test iteration variables
        Assert.assertNull(reader.getCurrentSequence());
        Assert.assertEquals(reader.getCurrentSequencePosition(), -1);

        // test failure to call any iterator method
        Assert.assertThrows(GemMappabilityException.class, reader::hasNext);
        Assert.assertThrows(GemMappabilityException.class, reader::next);
    }
}