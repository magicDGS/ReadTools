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
import htsjdk.samtools.util.Locatable;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.codecs.table.TableFeature;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ProperStatWindowCalculatorUnitTest extends RTBaseTest {

    private static final SAMFileHeader TEST_HEADER = ArtificialReadUtils.createArtificialSamHeader();
    private static final SimpleInterval TEST_INTERVAL = new SimpleInterval(TEST_HEADER.getSequence(0).getSequenceName(), 1, 100);
    private static final SimpleInterval TEST_OUTSIDE_INTERVAL = new SimpleInterval(TEST_INTERVAL.getContig(), TEST_INTERVAL.getEnd() + 100, TEST_INTERVAL.getEnd() + 100);
    private static final SimpleInterval TEST_OTHER_CHROMOSOME = new SimpleInterval(TEST_HEADER.getSequence(1).getSequenceName(), 1, 100);
    private static final ContainSoftclipCounter SOFTCLIP_COUNTER = new ContainSoftclipCounter();
    private static final PairIntegerTagCounter NM_EQ_ZERO_COUNTER = new PairIntegerTagCounter("NM", RelationalOperator.EQ, 0);
    private static final List<String> EXPECTED_COLUMN_NAMES = Arrays.asList("total", "proper", "missing",
            SOFTCLIP_COUNTER.getStatName(), NM_EQ_ZERO_COUNTER.getStatName());


    private static final ProperStatWindowCalculator createTestWindow() {
        return new ProperStatWindowCalculator(
                TEST_INTERVAL,
                Collections.singletonList(SOFTCLIP_COUNTER),
                Collections.singletonList(NM_EQ_ZERO_COUNTER));
    }

    private static GATKRead createSingleRead(final String cigar, final int nm, final Locatable positon) {
        final GATKRead read = ArtificialReadUtils.createArtificialRead(cigar);
        read.setAttribute("NM", nm);
        read.setPosition(positon);
        return read;
    }

    private static GATKRead createPair(final String cigar, final GATKRead read, final int nm, final Locatable positon) {
        final GATKRead pair = createSingleRead(cigar, nm, positon);
        read.setMatePosition(pair);
        pair.setMatePosition(read);
        return pair;
    }

    private static Tuple2<GATKRead, GATKRead> createPair(final String cigar1, final String cigar2, final int nm1, final int nm2, final Locatable pos1, final Locatable pos2) {
        final GATKRead first = createSingleRead(cigar1, nm1, pos1);
        return Tuple2.apply(first, createPair(cigar2, first, nm2, pos2));
    }

    private static GATKRead createUnmappedRead(final Locatable position) {
        final byte[] seqs = new byte[10];
        return (position != null) ?
                ArtificialReadUtils.createArtificialUnmappedReadWithAssignedPosition(TEST_HEADER, position.getContig(), position.getStart(), seqs, seqs) :
                ArtificialReadUtils.createArtificialUnmappedRead(TEST_HEADER, seqs, seqs);
    }

    @Test
    public void testDefaultConstructorGetColumnNames() throws Exception {
        Assert.assertEquals(createTestWindow().getColumnNames(), EXPECTED_COLUMN_NAMES);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDifferentColumnName() throws Exception {
        new ProperStatWindowCalculator(TEST_INTERVAL,
                Collections.singletonList(SOFTCLIP_COUNTER),
                Collections.singletonList(NM_EQ_ZERO_COUNTER),
                Collections.emptyList());
    }

    @DataProvider
    public Object[][] cacheData() throws Exception {
        return new Object[][] {
                {"unmapped (unassigned location)", createUnmappedRead(null), false},
                {"unmapped (assigned location)", createUnmappedRead(TEST_INTERVAL), false},
                {"single-read", createSingleRead("100M", 0, TEST_INTERVAL), false},
                {"pair-read (overlapping)", createPair("100M", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, TEST_INTERVAL), true},
                {"pair-read (mate overlapping)", createPair("100M", createSingleRead("100M", 0, TEST_INTERVAL), 0, TEST_OUTSIDE_INTERVAL), true},
                {"pair-read (none overlapping)", createPair("100M", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, TEST_OUTSIDE_INTERVAL), false},
                {"pair-read (no proper)", createPair("100M", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, TEST_OTHER_CHROMOSOME), false}
        };
    }

    @Test(dataProvider = "cacheData")
    public void testAddAndHasCachePairs(final String desc, final GATKRead read, final boolean expectedCached) throws Exception {
        final ProperStatWindowCalculator calculator = createTestWindow();
        Assert.assertFalse(calculator.hasCachedPairs(), "new window has cached pairs");
        calculator.addRead(read);
        Assert.assertEquals(calculator.hasCachedPairs(), expectedCached, desc);
    }

    @DataProvider
    public Object[][] fragmentOverlapsData() throws Exception {
        // create mate unmmaped
        final GATKRead mateUnmapped = createPair("100M", createSingleRead("100M", 0, TEST_INTERVAL), 0, TEST_OTHER_CHROMOSOME);
        mateUnmapped.setMateIsUnmapped();
        return new Object[][] {
                {"unmapped (unassigned location)", createUnmappedRead(null), false},
                {"unmapped (assigned location)", createUnmappedRead(TEST_INTERVAL), false},
                {"single-read (overlapping)", createSingleRead("100M", 0, TEST_INTERVAL), true},
                {"single-read (not overlapping)", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), false},
                {"single-read (other contig)", createSingleRead("100M", 0, TEST_OTHER_CHROMOSOME), false},
                {"pair-read (mate overlapping)", createPair("100M", createSingleRead("100M", 0, TEST_INTERVAL), 0, TEST_OUTSIDE_INTERVAL), true},
                {"pair-read (none overlapping)", createPair("100M", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, TEST_OUTSIDE_INTERVAL), false},
                {"pair-read (no proper)", createPair("100M", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, TEST_OTHER_CHROMOSOME), false},
                {"pair-read (mate unmapped with assigned position)", mateUnmapped, false}
        };
    }

    @Test(dataProvider = "fragmentOverlapsData")
    public void testFragmentOverlaps(final String desc, final GATKRead read, final boolean expected) throws Exception {
        Assert.assertEquals(createTestWindow().fragmentOverlaps(read), expected, desc);
    }

    @DataProvider
    public Object[][] oneReadToAdd() throws Exception {
        // order:
        // 1. description
        // 2. read
        // 3. total
        // 4. proper
        // 5. missing
        return new Object[][] {
                {"unmapped (unassigned location)", createUnmappedRead(null), 0, 0, 0},
                {"unmapped (assigned location)", createUnmappedRead(TEST_INTERVAL), 0, 0, 0},
                {"single-read (overlapping)", createSingleRead("100M", 0, TEST_INTERVAL), 1, 0, 0},
                {"single-read (not overlapping)", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, 0, 0},
                {"single-read (other contig)", createSingleRead("100M", 0, TEST_OTHER_CHROMOSOME), 0, 0, 0},
                {"pair-read (overlapping)", createPair("100M", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, TEST_INTERVAL), 1, 1, 1},
                // note: this read is cached but not added - thus, total/proper/missing are all 0
                {"pair-read (mate overlapping)", createPair("100M", createSingleRead("100M", 0, TEST_INTERVAL), 0, TEST_OUTSIDE_INTERVAL), 0, 0, 0},
                {"pair-read (none overlapping)", createPair("100M", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, TEST_OUTSIDE_INTERVAL), 0, 0, 0},
                {"pair-read (no proper)", createPair("100M", createSingleRead("100M", 0, TEST_OUTSIDE_INTERVAL), 0, TEST_OTHER_CHROMOSOME), 0, 0, 0}
        };
    }

    @Test(dataProvider = "oneReadToAdd")
    public void testAddOneReadRunningStats(final String desc, final GATKRead read, final int total, final int proper, final int missing) {
        final ProperStatWindowCalculator calculator = createTestWindow();
        calculator.addRead(read);
        final TableFeature feature = calculator.toTableFeature();
        Assert.assertEquals(feature.get("total"), String.valueOf(total), "total " + desc);
        Assert.assertEquals(feature.get("proper"), String.valueOf(proper), "proper " + desc);
        Assert.assertEquals(feature.get("missing"), String.valueOf(missing), "missing " + desc);
    }


    @DataProvider
    public Object[][] twoReadsToAdd() throws Exception {
        // order:
        // 1. description
        // 2. reads
        // 3. total
        // 4. proper
        // 5. missing
        return new Object[][] {
                // both are added
                {"proper (both overlapping)", createPair("100M", "100M", 0, 0, TEST_INTERVAL, TEST_INTERVAL), 2, 2, 0},
                // only one is added
                {"proper (first overlapping)", createPair("100M", "100M", 0, 0, TEST_INTERVAL, TEST_OUTSIDE_INTERVAL), 1, 1, 0},
                {"proper (second overlapping)", createPair("100M", "100M", 0, 0, TEST_OUTSIDE_INTERVAL, TEST_INTERVAL), 1, 1, 0},
                // non-proper (but overlapping)
                {"non-proper (first overlapping)",createPair("100M", "100M", 0, 0, TEST_INTERVAL, TEST_OTHER_CHROMOSOME), 1, 0, 0},
                {"non-proper (second overlapping)",createPair("100M", "100M", 0, 0, TEST_OTHER_CHROMOSOME, TEST_INTERVAL), 1, 0, 0},
                // both non-overlapping
                {"proper (none overlapping)", createPair("100M", "100M", 0, 0, TEST_OUTSIDE_INTERVAL, TEST_OUTSIDE_INTERVAL), 0, 0, 0},
                {"non-proper (none overlapping)", createPair("100M", "100M", 0, 0, TEST_OTHER_CHROMOSOME, TEST_OTHER_CHROMOSOME), 0, 0, 0}
        };
    }

    @Test(dataProvider = "twoReadsToAdd")
    public void testAddTwoReadsRunningStats(final String desc, final Tuple2<GATKRead, GATKRead> pair, final int total, final int proper, final int missing) {
        final ProperStatWindowCalculator calculator = createTestWindow();
        calculator.addRead(pair._1);
        calculator.addRead(pair._2);
        final TableFeature feature = calculator.toTableFeature();
        Assert.assertEquals(feature.get("total"), String.valueOf(total), "total " + desc);
        Assert.assertEquals(feature.get("proper"), String.valueOf(proper), "proper " + desc);
        Assert.assertEquals(feature.get("missing"), String.valueOf(missing), "missing " + desc);
    }

    @DataProvider
    public static Object[][] computeStats() {
        final String[] cigars = new String[] {"100M", "10S100M"};
        final int[] nm = new int[]{0, 1};
        return new Object[][] {
                // both included
                {"100M", "100M", 0, 0, true, true},
                {"10S100M", "100M", 0, 0, true, true},
                {"10S100M", "10S100M", 0, 0,  true, true},
                {"100M", "100M", 1, 0, true, true},
                {"10S100M", "100M", 1, 0, true, true},
                {"10S100M", "10S100M", 1, 0, true, true},
                {"100M", "100M", 1, 1, true, true},
                {"10S100M", "100M", 1, 1, true, true},
                {"10S100M", "10S100M", 1, 1, true, true},
                // only first included
                {"100M", "100M", 0, 0, true, false},
                {"10S100M", "100M", 0, 0, true, false},
                {"10S100M", "10S100M", 0, 0, true, false},
                {"100M", "100M", 1, 0, true, false},
                {"10S100M", "100M", 1, 0, true, false},
                {"10S100M", "10S100M", 1, 0, true, false},
                {"100M", "100M", 1, 1, true, false},
                {"10S100M", "100M", 1, 1, true, false},
                {"10S100M", "10S100M", 1, 1, true, false},
                // only second included
                {"100M", "100M", 0, 0, false, true},
                {"10S100M", "100M", 0, 0, false, true},
                {"10S100M", "10S100M", 0, 0, false, true},
                {"100M", "100M", 1, 0, false, true},
                {"10S100M", "100M", 1, 0, false, true},
                {"10S100M", "10S100M", 1, 0, false, true},
                {"100M", "100M", 1, 1, false, true},
                {"10S100M", "100M", 1, 1, false, true},
                {"10S100M", "10S100M", 1, 1, false, true}
        };
    }

    @Test(dataProvider = "computeStats")
    public void testComputeStats(final String firstCigar, final String secondCigar,
            final int firstNM, final int secondNM,
            final boolean first, final boolean second) {
        final Tuple2<GATKRead, GATKRead> pair = createPair(firstCigar, secondCigar,
                firstNM, secondNM,
                (first) ? TEST_INTERVAL : TEST_OUTSIDE_INTERVAL,
                (second) ? TEST_INTERVAL : TEST_OUTSIDE_INTERVAL);

        final ProperStatWindowCalculator calculator = createTestWindow();
        calculator.addRead(pair._1);
        calculator.addRead(pair._2);
        final TableFeature feature = calculator.toTableFeature();
        // check the single-read stat (depends on how many were added)
        final int singleStat;
        if (first && second) {
            singleStat = SOFTCLIP_COUNTER.reduce(SOFTCLIP_COUNTER.compute(pair._2), SOFTCLIP_COUNTER.compute(pair._1));
        } else if (first) {
            singleStat = SOFTCLIP_COUNTER.compute(pair._1);
        } else {
            singleStat = SOFTCLIP_COUNTER.compute(pair._2);
        }
        Assert.assertEquals(feature.get(SOFTCLIP_COUNTER.getStatName()), SOFTCLIP_COUNTER.tableResultFormat(singleStat), SOFTCLIP_COUNTER.getStatName());
        // check that is the same as adding the pair
        Assert.assertEquals(feature.get(NM_EQ_ZERO_COUNTER.getStatName()), NM_EQ_ZERO_COUNTER.tableResultFormat(NM_EQ_ZERO_COUNTER.compute(pair)), NM_EQ_ZERO_COUNTER.getStatName());
    }


    @Test
    public void testToTableFeatureEmptyWindow() throws Exception {
        final ProperStatWindowCalculator calculator = createTestWindow();
        final TableFeature feature = calculator.toTableFeature();
        Assert.assertEquals(feature.columnCount(), EXPECTED_COLUMN_NAMES.size());
        Assert.assertEquals(feature.getHeader(), EXPECTED_COLUMN_NAMES);
        Assert.assertEquals(feature.getLocation(), TEST_INTERVAL);
        Assert.assertEquals(feature.getAllValues(), Arrays.asList(
                        // total/proper/missing
                        "0", "0", "0",
                        // statistics
                        SOFTCLIP_COUNTER.tableMissingFormat(), NM_EQ_ZERO_COUNTER.tableMissingFormat()));
    }
}
