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

import org.magicdgs.readtools.utils.function.BinaryPredicate;

import java.util.Comparator;
import java.util.function.IntPredicate;

/**
 * Representation of relational operators as an enum.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public enum RelationalOperator implements BinaryPredicate<Comparable> {
    EQ(i -> i == 0, "="),
    NE(i -> i != 0, "!="),
    GT(i -> i > 0, ">"),
    GE(i -> i >= 0, ">="),
    LT(i -> i < 0, "<"),
    LE(i -> i <= 0, "<=");

    private final IntPredicate eval;
    private final String string;

    private RelationalOperator(final IntPredicate eval, final String string) {
        this.eval = eval;
        this.string = string;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean test(final Comparable first, final Comparable second) {
        return eval.test(first.compareTo(second));
    }

    /**
     * Applies the relational operator to an object with an associated comparator.
     *
     * @param first the first input argument
     * @param second the second input argument
     * @param comparator comparator to compare the objects.
     * @param <T> object type.
     * @return {@code true} if the condition is met; {@code false} otherwise.
     */
    public <T> boolean test(final T first, final T second, final Comparator<T> comparator) {
        return eval.test(comparator.compare(first, second));
    }

    /**
     * Returns the String formatted as a symbol.
     *
     * @return the symbol for the operator.
     */
    // TODO: change for toString() after https://github.com/broadinstitute/barclay/issues/139
    public String symbol() {
        return string;
    }

}
