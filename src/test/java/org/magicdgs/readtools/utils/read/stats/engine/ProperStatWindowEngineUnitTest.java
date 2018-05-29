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
import htsjdk.samtools.util.Log;
import org.broadinstitute.hellbender.utils.LoggingUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ProperStatWindowEngineUnitTest extends RTBaseTest {

    private static final SAMFileHeader TEST_HEADER = ArtificialReadUtils.createArtificialSamHeader();
    private static final SimpleInterval FIRST_CONTIG_INTERVAL_1 = new SimpleInterval(TEST_HEADER.getSequence(0).getSequenceName(), 1, 100);
    private static final SimpleInterval FIRST_CONTIG_INTERVAL_2 = new SimpleInterval(TEST_HEADER.getSequence(0).getSequenceName(), 101, 200);
    private static final SimpleInterval SECOND_CONTIG_INTERVAL = new SimpleInterval(TEST_HEADER.getSequence(1).getSequenceName(), 1, 100);

    private static final ContainSoftclipCounter SOFTCLIP_COUNTER = new ContainSoftclipCounter();
    private static final PairIntegerTagCounter NM_EQ_ZERO_COUNTER = new PairIntegerTagCounter("NM", RelationalOperator.EQ, 0);

    private static final List<String> EXPECTED_COLUMN_NAMES = Arrays.asList("total", "proper", "missing",
            SOFTCLIP_COUNTER.getStatName(), NM_EQ_ZERO_COUNTER.getStatName());

    private static String formatExpectedRow(final SimpleInterval interval,
            final int total, final int proper, final int missing,
            final String softclip, final String nmEqZero) {
        return String.format("%s:%d-%d\t%s",
                interval.getContig(), interval.getStart(), interval.getEnd(),
                String.join("\t", Arrays.asList(
                        Integer.toString(total), Integer.toString(proper), Integer.toString(missing),
                        softclip, nmEqZero)
                )
        );
    }

    @DataProvider
    public static Object[][] emtpyOutputArgs() {
        final String expectedHeader = "window\t" + String.join("\t", EXPECTED_COLUMN_NAMES) + "\n";
        return new Object[][] {
                // do not print all - only header for empty
                {false, expectedHeader},
                // print all
                {true, expectedHeader +
                        formatExpectedRow(FIRST_CONTIG_INTERVAL_1, 0, 0, 0, "NA", "NA") + "\n" +
                        formatExpectedRow(FIRST_CONTIG_INTERVAL_2, 0, 0, 0, "NA", "NA") + "\n" +
                        formatExpectedRow(SECOND_CONTIG_INTERVAL, 0, 0, 0, "NA", "NA") + "\n"
                }
        };
    }

    @Test(dataProvider = "emtpyOutputArgs")
    public void testEmptyOutput(final boolean printAll, final String expectedOutput) throws Exception {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final ProperStatWindowEngine engine = new ProperStatWindowEngine(
                TEST_HEADER.getSequenceDictionary(),
                Arrays.asList(FIRST_CONTIG_INTERVAL_1, FIRST_CONTIG_INTERVAL_2, SECOND_CONTIG_INTERVAL),
                Collections.singletonList(SOFTCLIP_COUNTER),
                Collections.singletonList(NM_EQ_ZERO_COUNTER),
                result, printAll);
        engine.close();
        Assert.assertEquals(result.toString(), expectedOutput);
    }
}