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

package org.magicdgs.readtools.utils.math;

import org.magicdgs.readtools.RTBaseTest;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RelationalOperatorUnitTest extends RTBaseTest {

    @DataProvider
    public static Object[][] integerOperations() {
        return new Object[][] {
                // equal
                {RelationalOperator.EQ, 1, 1, true},
                {RelationalOperator.EQ, 1, 3, false},
                {RelationalOperator.EQ, 3, 1, false},
                // non-equal
                {RelationalOperator.NE, 1, 1, false},
                {RelationalOperator.NE, 1, 3, true},
                {RelationalOperator.NE, 3, 1, true},
                // greater-than
                {RelationalOperator.GT, 1, 1, false},
                {RelationalOperator.GT, 1, 3, false},
                {RelationalOperator.GT, 3, 1, true},
                // greater-than or equal
                {RelationalOperator.GE, 1, 1, true},
                {RelationalOperator.GE, 1, 3, false},
                {RelationalOperator.GE, 3, 1, true},
                // lower-than
                {RelationalOperator.LT, 1, 1, false},
                {RelationalOperator.LT, 1, 3, true},
                {RelationalOperator.LT, 3, 1, false},
                // lower-than or equal
                {RelationalOperator.LE, 1, 1, true},
                {RelationalOperator.LE, 1, 3, true},
                {RelationalOperator.LE, 3, 1, false},
        };
    }

    @Test(dataProvider = "integerOperations")
    public void testIntegers(final RelationalOperator op, final int i, final int j,
            final boolean expected) {
        Assert.assertEquals(op.test(i, j), expected);
    }

    @DataProvider
    public Iterator<Object[]> allOperators() {
        return Stream.of(RelationalOperator.values()).map(o -> new Object[]{o}).iterator();
    }

    @Test(dataProvider = "allOperators")
    public void testCustomComparable(final RelationalOperator op) {
        // with custom comparator returning always equals
        Assert.assertEquals(op.test(10, 90, (i, j) -> 0), op.test(1, 1));
        // with custom comparator returning always lower first
        Assert.assertEquals(op.test(0, 0, (i, j) -> -1), op.test(0, 1));
        // with custom comparator returning always lower second
        Assert.assertEquals(op.test(0, 0, (i, j) -> 1), op.test(1, 0));
    }
}
