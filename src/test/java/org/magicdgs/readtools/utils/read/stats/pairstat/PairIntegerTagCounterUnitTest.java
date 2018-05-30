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

import org.magicdgs.readtools.RTBaseTest;
import org.magicdgs.readtools.utils.math.RelationalOperator;

import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.Tuple2;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class PairIntegerTagCounterUnitTest extends RTBaseTest {

    private static final PairIntegerTagCounter COUNT_NM_EQUAL_TEN = new PairIntegerTagCounter("NM", RelationalOperator.EQ, 10);

    @DataProvider
    public static Object[][] invalidArgs() {
        return new Object[][] {
                {"INVALID_TAG", RelationalOperator.EQ},
                {"NM", null}
        };
    }

    @Test(dataProvider = "invalidArgs", expectedExceptions = IllegalArgumentException.class)
    public void testInvalidConstructor(final String tagName, final RelationalOperator op) {
        new PairIntegerTagCounter(tagName, op, 1);
    }

    @Test
    public void testInitEmpty() {
        final PairIntegerTagCounter counter = new PairIntegerTagCounter();
        Assert.assertThrows(IllegalArgumentException.class, counter::init);
    }

    @Test
    public void testCountPair() {
        final Tuple2<GATKRead, GATKRead> reads = Tuple2.apply(
                ArtificialReadUtils.createArtificialRead("100M"),
                ArtificialReadUtils.createArtificialRead("100M")
        );
        reads._1.setAttribute("NM", 10);
        reads._2.setAttribute("NM", 10);

        Assert.assertEquals(COUNT_NM_EQUAL_TEN.compute(reads).intValue(), 1);
    }

    @Test
    public void testNotCountPair() {
        final Tuple2<GATKRead, GATKRead> reads = Tuple2.apply(
                ArtificialReadUtils.createArtificialRead("100M"),
                ArtificialReadUtils.createArtificialRead("100M")
        );
        reads._1.setAttribute("NM", 0);
        reads._2.setAttribute("NM", 10);

        Assert.assertEquals(COUNT_NM_EQUAL_TEN.compute(reads).intValue(), 0);
        Assert.assertEquals(COUNT_NM_EQUAL_TEN.compute(reads.swap()).intValue(), 0);
        Assert.assertEquals(COUNT_NM_EQUAL_TEN.compute(Tuple2.apply(reads._1, reads._2)).intValue(), 0);
    }

    @Test
    public void testNotCountReadWithoutTag() {
        final Tuple2<GATKRead, GATKRead> reads = Tuple2.apply(
                ArtificialReadUtils.createArtificialRead("100M"),
                ArtificialReadUtils.createArtificialRead("100M")
        );

        // if none of them have the tag -> do not count
        Assert.assertEquals(COUNT_NM_EQUAL_TEN.compute(reads).intValue(), 0);

        // if only one has the tag -> do not count
        reads._1.setAttribute("NM", 10);
        Assert.assertEquals(COUNT_NM_EQUAL_TEN.compute(reads).intValue(), 0);
        Assert.assertEquals(COUNT_NM_EQUAL_TEN.compute(reads.swap()).intValue(), 0);
    }

    @Test
    public void testStatName() throws Exception {
        Assert.assertEquals(COUNT_NM_EQUAL_TEN.getStatName(), "pair.NM.eq.10");
    }
}
