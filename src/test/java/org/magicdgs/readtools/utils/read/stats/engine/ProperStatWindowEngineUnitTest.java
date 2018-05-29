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

package org.magicdgs.readtools.utils.read.stats.engine;

import org.magicdgs.readtools.RTBaseTest;
import org.magicdgs.readtools.utils.math.RelationalOperator;
import org.magicdgs.readtools.utils.read.stats.pairstat.PairIntegerTagCounter;
import org.magicdgs.readtools.utils.read.stats.singlestat.ContainSoftclipCounter;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ProperStatWindowEngineUnitTest extends RTBaseTest {

    private static final SAMFileHeader TEST_HEADER = ArtificialReadUtils.createArtificialSamHeader();
    private static final SimpleInterval FIRST_CONTIG_INTERVAL_1 = new SimpleInterval(TEST_HEADER.getSequence(0).getSequenceName(), 1, 100);
    private static final SimpleInterval FIRST_CONTIG_INTERVAL_2 = new SimpleInterval(TEST_HEADER.getSequence(0).getSequenceName(), 101, 200);
    private static final SimpleInterval SECOND_CONTIG_INTERVAL = new SimpleInterval(TEST_HEADER.getSequence(1).getSequenceName(), 1, 100);
    private static final SimpleInterval OUT_OF_WINDOW_INTERVAL = new SimpleInterval(TEST_HEADER.getSequence(2).getSequenceName(), 1, 100);

    private static final ContainSoftclipCounter SOFTCLIP_COUNTER = new ContainSoftclipCounter();
    private static final PairIntegerTagCounter NM_EQ_ZERO_COUNTER = new PairIntegerTagCounter("NM", RelationalOperator.EQ, 0);

    private static final String NL = "\n";
    private static final String EXPECTED_OUTPUT_HEADER = String.join("\t",
            "window", "total", "proper", "missing",
            SOFTCLIP_COUNTER.getStatName(), NM_EQ_ZERO_COUNTER.getStatName()) + NL;

    private static final String EMPTY_PRINTALL_RESULT = EXPECTED_OUTPUT_HEADER +
            formatExpectedRow(FIRST_CONTIG_INTERVAL_1, 0, 0, 0, "NA", "NA") +
            formatExpectedRow(FIRST_CONTIG_INTERVAL_2, 0, 0, 0, "NA", "NA") +
            formatExpectedRow(SECOND_CONTIG_INTERVAL, 0, 0, 0, "NA", "NA");

    private static ProperStatWindowEngine createEngineInstance(final ByteArrayOutputStream resultStream, final boolean printAll) {
        return new ProperStatWindowEngine(
                TEST_HEADER.getSequenceDictionary(),
                Arrays.asList(FIRST_CONTIG_INTERVAL_1, FIRST_CONTIG_INTERVAL_2, SECOND_CONTIG_INTERVAL),
                Collections.singletonList(SOFTCLIP_COUNTER),
                Collections.singletonList(NM_EQ_ZERO_COUNTER),
                resultStream, printAll);
    }

    private static String formatExpectedRow(final SimpleInterval interval,
            final int total, final int proper, final int missing,
            final String softclip, final String nmEqZero) {
        return String.format("%s:%d-%d\t%s",
                interval.getContig(), interval.getStart(), interval.getEnd(),
                String.join("\t", Arrays.asList(
                        Integer.toString(total), Integer.toString(proper), Integer.toString(missing),
                        softclip, nmEqZero)
                ) + NL
        );
    }

    @DataProvider
    public static Object[][] emtpyOutputArgs() {
        return new Object[][] {
                // do not print all - only header for empty
                {false, EXPECTED_OUTPUT_HEADER},
                // print all
                {true, EMPTY_PRINTALL_RESULT}
        };
    }

    @Test(dataProvider = "emtpyOutputArgs")
    public void testEmptyOutput(final boolean printAll, final String expectedOutput) throws Exception {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final ProperStatWindowEngine engine = createEngineInstance(result, printAll);
        engine.close();
        Assert.assertEquals(result.toString(), expectedOutput);
    }


    @Test
    public void testFlushToNextContig() throws Exception {
        // result stream and engine with all
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final ProperStatWindowEngine engine = createEngineInstance(result, true);

        // create a proper pair on hte second contig and add to engine
        ArtificialReadUtils.createPair(TEST_HEADER, "overlapping.pair",
                50, // read length
                1, // on second contig (ref-index = 1)
                SECOND_CONTIG_INTERVAL.getStart(), SECOND_CONTIG_INTERVAL.getStart(), // start on the interval
                true, false) // not required
                .forEach(engine::addRead); // add the pair

        // check that before closing, the flush for the first
        final String firstFlush = EXPECTED_OUTPUT_HEADER +
                formatExpectedRow(FIRST_CONTIG_INTERVAL_1, 0, 0, 0, "NA", "NA") +
                formatExpectedRow(FIRST_CONTIG_INTERVAL_2, 0, 0, 0, "NA", "NA");

        Assert.assertEquals(result.toString(), firstFlush);

        // and when closing, flushes the second one
        engine.close();
        Assert.assertEquals(result.toString(), firstFlush +
                formatExpectedRow(SECOND_CONTIG_INTERVAL, 2, 2, 0, "0", "0")
        );
    }

    @Test
    public void testAddReadsAfterProvidedWindowsFlushEverything() throws Exception {
        // result stream and add both reads
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final ProperStatWindowEngine engine = createEngineInstance(result, true);

        final GATKRead read = ArtificialReadUtils.createArtificialRead("100M");
        read.setPosition(OUT_OF_WINDOW_INTERVAL);
        engine.addRead(read);

        // complete result is already there
        Assert.assertEquals(result.toString(), EMPTY_PRINTALL_RESULT);

        // ensure that adding after flushing all does not throw
        engine.addRead(read);

        // and after engine is closed, nothing changes
        engine.close();
        Assert.assertEquals(result.toString(), EMPTY_PRINTALL_RESULT);
    }

    @Test
    public void testDoNotFlushUnmappedReadWithAssignedPosition() throws Exception {
        // result stream and add both reads
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final ProperStatWindowEngine engine = createEngineInstance(result, false);

        final GATKRead unmapped = ArtificialReadUtils.createArtificialUnmappedReadWithAssignedPosition(
                TEST_HEADER,
                SECOND_CONTIG_INTERVAL.getContig(),
                SECOND_CONTIG_INTERVAL.getStart(),
                new byte[10], new byte[10]);

        engine.addRead(unmapped);

        // check that only the header is there
        Assert.assertEquals(result.toString(), EXPECTED_OUTPUT_HEADER);

        // create a proper pair on hte second contig and add to engine
        ArtificialReadUtils.createPair(TEST_HEADER, "overlapping.pair",
                50, // read length
                1, // on second contig (ref-index = 1)
                SECOND_CONTIG_INTERVAL.getStart(), SECOND_CONTIG_INTERVAL.getStart(), // start on the interval
                true, false) // not required
                .forEach(engine::addRead); // add the pair


        // close and check the result is there
        engine.close();
        Assert.assertEquals(result.toString(), EXPECTED_OUTPUT_HEADER +
                formatExpectedRow(SECOND_CONTIG_INTERVAL, 2, 2, 0, "0", "0"));
    }

    @Test
    public void testAddingPairInConsecutiveWindows() throws Exception {
        // result stream and add both reads
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final ProperStatWindowEngine engine = createEngineInstance(result, false);

        // create a proper pair on the first contig and add to the engine
        ArtificialReadUtils.createPair(TEST_HEADER, "consecutive.pair",
                50, // read length
                0, // on first contig (ref-index = 0)
                FIRST_CONTIG_INTERVAL_1.getStart(), FIRST_CONTIG_INTERVAL_2.getStart(), // start on the interval
                true, false) // not required
                .forEach(engine::addRead); // add the pair

        // check that only the header is there (do not flush because the consecutive reads don't trigger it)
        Assert.assertEquals(result.toString(), EXPECTED_OUTPUT_HEADER);

        // check that after closing the results are the expected ones
        engine.close();

        Assert.assertEquals(result.toString(), EXPECTED_OUTPUT_HEADER +
                formatExpectedRow(FIRST_CONTIG_INTERVAL_1, 1, 1, 0, "0", "0") +
                formatExpectedRow(FIRST_CONTIG_INTERVAL_2, 1, 1, 0, "0", "0"));
    }
}