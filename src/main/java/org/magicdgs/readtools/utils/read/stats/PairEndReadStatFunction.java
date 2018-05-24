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

import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

/**
 * Function for pair-end reads ({@link GATKRead}) that can be computed in two steps.
 *
 * <p>This is useful when iterating over a file where the order keep read-pairs separated (e.g.
 * coordinate). It allows to cache needed information in a temporary object and use it when the
 * other read is reached.
 *
 * <p>The main function is {@link #compute(Tuple2)}, which define was is the expected behavior. If
 * iteration is performed with unordered read pairs, the caller is expected to find the read
 * pairs and call the following methods when found:
 *
 * <ol>
 *  <li>{@link #computeIntermediateFirst(GATKRead)} on the first pair ({@link Tuple2#_1()}</li>
 *  <li>{@link #computeIntermediateSecond(GATKRead)} on the second pair ({@link Tuple2#_2()}</li>
 *  <li>{@link #mergePairValues(Object, Object)} on returned objects.</li>
 * </ol>
 *
 * <p>For details on how to use this method, please refer to TODO
 *
 * @param <S> statistic value.
 * @param <T> temporary value (might be the same or different).
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface PairEndReadStatFunction<S, T> extends StatFunction<S, Tuple2<GATKRead, GATKRead>> {

    /**
     * Applies the function to both read pairs.
     *
     * <p>This is the main method for the {@link PairEndReadStatFunction} and should implement the
     * following pseudo-code (default implementation):
     *
     * <ol>
     *  <li>{@link #computeIntermediateFirst(GATKRead)} on the first pair ({@link Tuple2#_1()}</li>
     *  <li>{@link #computeIntermediateSecond(GATKRead)} on the second pair ({@link Tuple2#_2()}</li>
     *  <li>{@link #mergePairValues(Object, Object)} on returned objects.</li>
     * </ol>
     *
     * <p>Implementations are not enforced to validate that reads in the
     * {@code Tuple2<GATKRead, GATKRead> pair} are valid pairs. That means that calling on non-pairs
     * might return a result as well.
     *
     * @return the statistic applied to the pair of reads.
     */
    public default S compute(final Tuple2<GATKRead, GATKRead> pair) {
        final T first = computeIntermediateFirst(pair._1());
        final T second = computeIntermediateSecond(pair._2());
        return mergePairValues(first, second);
    }

    /**
     * Apply the function to the first read of the pair.
     *
     * @param read first read of the pair.
     *
     * @return the temporary value for the statistic. This might not be used for consumption.
     */
    public T computeIntermediateFirst(final GATKRead read);

    /**
     * Applies the function to the second read of the pair.
     *
     * <p>Default implementation returns the result of {@link #computeIntermediateFirst(GATKRead)}}.
     *
     * @param read second read of the pair.
     *
     * @return the temporary value for the statistic. This might not be used for consumption.
     */
    public default T computeIntermediateSecond(final GATKRead read) {
        return computeIntermediateFirst(read);
    }

    /**
     * Merges the values coming from both read pairs.
     *
     * @param first  the value from the first read of the pair.
     * @param second the value for the second read of the pair.
     *
     * @return the merged value.
     */
    public S mergePairValues(final T first, final T second);

}
