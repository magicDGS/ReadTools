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

package org.magicdgs.readtools.utils.read.stats;

/**
 * Common interface for statistic computation for {@link org.broadinstitute.hellbender.utils.read.GATKRead}.
 *
 * <p>This statistic could be applied to single or pair-end reads.
 *
 * @param <S> statistic value
 * @param <T> type of record to compute the statistic.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface StatFunction<S, T> {

    /**
     * Initialize the stat function (if needed).
     *
     * <p>Default implementation does nothing.
     *
     * @throws IllegalArgumentException if the state of the function does not allow to initialize.
     */
    public default void init() {}

    /**
     * Gets the statistic value for the record.
     *
     * @param record the record to compute the statistic from.
     *
     * @return the value for the statistic.
     */
    public S compute(T record);

    /**
     * Gets the name for the statistic.
     *
     * <p>For several statistics in the same function, the return value should be tab-separated.
     *
     * @return statistic(s) name.
     */
    public String getStatName();

    /**
     * Combines a statistic generated from one pair and a running accumulator.
     *
     * <p>Example of implementations can be median, mean or counts.
     *
     * @param current     the current value for the pair.
     * @param accumulator the accumulator. If not initialized, it is {@code null}.
     *
     * @return the reduced value.
     */
    public S reduce(final S current, final S accumulator);

    /**
     * Formats the result as a string.
     *
     * <p>For several statistics in the same function, the return value should be tab-separated.
     *
     * <p>Default implementation returns {@link #tableMissingFormat()} if {@code result} is
     * {@code null}; otherwise, it calls {@code result.toString()}.
     *
     * @param result the result to print.
     *
     * @return formatted result(s).
     */
    public default String tableResultFormat(final S result) {
        return (result == null) ? tableMissingFormat() : result.toString();
    }

    /**
     * Gets the formatted value for missing data.
     *
     * <p>For several statistics in the same function, the return value should be tab-separated.
     *
     * <p>Default implementation returns {@code "NA"}.
     *
     * @return formatted missing value.
     */
    public default String tableMissingFormat() {
        return "NA";
    }
}
