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

package org.magicdgs.readtools.utils.read.stats.pairstat;

import org.magicdgs.readtools.utils.math.RelationalOperator;

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
public class PairIntegerTagListCounterUnitTest {

    // counters to test
    private static final String NM_TAG_NAME = "NM";
    private static final String AS_TAG_NAME = "AS";
    private static final PairIntegerTagCounter COUNT_NM_EQUAL_TEN = new PairIntegerTagCounter(NM_TAG_NAME, RelationalOperator.EQ, 10);
    private static final PairIntegerTagCounter COUNT_AS_IS_ONE = new PairIntegerTagCounter(AS_TAG_NAME, RelationalOperator.EQ, 1);
    private static final PairIntegerTagListCounter COUNTER_LIST = new PairIntegerTagListCounter(
            Arrays.asList(NM_TAG_NAME, AS_TAG_NAME),
            Arrays.asList(RelationalOperator.EQ, RelationalOperator.EQ),
            Arrays.asList(10, 1)
    );

    private static Tuple2<GATKRead, GATKRead> createTestPair(final int firstNM, final int secondNN,
            final int firstAS, final int secondAS) {
        final Tuple2<GATKRead, GATKRead> reads = Tuple2.apply(
                ArtificialReadUtils.createArtificialRead("100M"),
                ArtificialReadUtils.createArtificialRead("100M")
        );
        // set first tags
        reads._1.setAttribute(NM_TAG_NAME, firstNM);
        reads._1.setAttribute(AS_TAG_NAME, firstAS);
        // set second tags
        reads._2.setAttribute(NM_TAG_NAME, secondNN);
        reads._2.setAttribute(AS_TAG_NAME, secondAS);

        return reads;
    }

    @DataProvider
    public Object[][] testCountListData() {
        return new Object[][]{
                // none is counted
                {createTestPair(10, 2, 0, 1), 0, 0},
                // both are counted
                {createTestPair(10, 10, 1, 1), 1, 1},
                // first is counted
                {createTestPair(10, 10, 0, 0), 1, 0},
                // second is counted
                {createTestPair(10, 2, 1, 1), 0, 1},
        };
    }

    @Test(dataProvider = "testCountListData")
    public void testCountListResult(final Tuple2<GATKRead, GATKRead> reads, final Integer first, final Integer second) {
        Assert.assertEquals(COUNT_NM_EQUAL_TEN.compute(reads), first, "wrong provider case (first)");
        Assert.assertEquals(COUNT_AS_IS_ONE.compute(reads), second, "wrong provider case (second)");
        Assert.assertEquals(COUNTER_LIST.compute(reads), Arrays.asList(first, second));
    }

    @DataProvider
    public Object[][] invalidArgs() {
        return new Object[][] {
                // null lists
                {null, Collections.singletonList(RelationalOperator.EQ), Collections.singletonList(1)},
                {Collections.singletonList(NM_TAG_NAME), null, Collections.singletonList(1)},
                {Collections.singletonList(NM_TAG_NAME), Collections.singletonList(RelationalOperator.EQ), null},
                // empty lists
                {Collections.emptyList(), Collections.singletonList(RelationalOperator.EQ), Collections.singletonList(1)},
                {Collections.singletonList(NM_TAG_NAME), Collections.emptyList(), Collections.singletonList(1)},
                {Collections.singletonList(NM_TAG_NAME), Collections.singletonList(RelationalOperator.EQ), Collections.emptyList()},
                // different number of tags and other args
                {Collections.singletonList(NM_TAG_NAME), Arrays.asList(RelationalOperator.EQ, RelationalOperator.EQ), Collections.singletonList(1)},
                {Collections.singletonList(NM_TAG_NAME), Collections.singletonList(RelationalOperator.EQ), Arrays.asList(1, 2)},
        };
    }

    @Test(dataProvider = "invalidArgs", expectedExceptions = IllegalArgumentException.class)
    public void testInvalidConstructor(final List<String> tag, final List<RelationalOperator> op, final List<Integer> threshold) {
        new PairIntegerTagListCounter(tag, op, threshold);
    }

    @Test
    public void testInitEmpty() {
        final PairIntegerTagListCounter counter = new PairIntegerTagListCounter();
        Assert.assertThrows(IllegalArgumentException.class, counter::init);
    }

    @Test
    public void testGetStatName() {
        Assert.assertEquals(COUNTER_LIST.getStatName(),
                COUNT_NM_EQUAL_TEN.getStatName() + "\t" + COUNT_AS_IS_ONE.getStatName());
    }

    @Test
    public void testTableMissingFormatList() {
        final String twoMissing = "NA\tNA";
        Assert.assertEquals(COUNTER_LIST.tableMissingFormat(), twoMissing);
        // test also no-results to table format
        Assert.assertEquals(COUNTER_LIST.tableResultFormat(null), twoMissing);
        Assert.assertEquals(COUNTER_LIST.tableResultFormat(Arrays.asList(null, null)), twoMissing);
    }

    @DataProvider
    public Object[][] invalidArgumentsForReduce() {
        return new Object[][]{
                {null, Arrays.asList(1, 2)},
                {Collections.emptyList(), Arrays.asList(1, 2)},
                {Arrays.asList(1, 2), Collections.emptyList()}
        };
    }

    @Test(dataProvider = "invalidArgumentsForReduce", expectedExceptions = IllegalArgumentException.class)
    public void testReduceInvalidArguments(final List<Integer> current, final List<Integer> accumulator) {
        COUNTER_LIST.reduce(current, accumulator);
    }

    @Test
    public void testReduceLoop() {
        // only test that the for loop is correct
        final int acc = 0;
        final int first = 1;
        final int second = 1;
        Assert.assertEquals(
                COUNTER_LIST.reduce(Arrays.asList(first, second), Arrays.asList(acc, acc)),
                Arrays.asList(
                        COUNT_NM_EQUAL_TEN.reduce(first, acc),
                        COUNT_NM_EQUAL_TEN.reduce(second, acc)
                )
        );
    }

    @Test
    public void testReduceForNullAccumulator() {
        final List<Integer> results = Arrays.asList(10, 30);
        Assert.assertEquals(COUNTER_LIST.reduce(results, null), results);
        Assert.assertSame(COUNTER_LIST.reduce(results, null), results);
    }
}