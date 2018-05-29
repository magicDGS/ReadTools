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

import org.magicdgs.readtools.utils.read.stats.PairEndReadStatFunction;
import org.magicdgs.readtools.utils.read.stats.SingleReadStatFunction;
import org.magicdgs.readtools.utils.read.stats.StatFunction;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.util.CoordMath;
import htsjdk.samtools.util.Locatable;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.codecs.table.TableFeature;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Calculator for several {@link StatFunction} over a window.
 *
 * <p>This calculator works only for {@link GATKRead} functions, concretely using implementations of
 * {@link SingleReadStatFunction} and {@link PairEndReadStatFunction}.
 *
 * <p>Computation of statistics happens only for pair-end reads. By default, the calculator computes
 * all the reads that are in the window ("total"), all proper pairs ("proper") and missing pairs in
 * the file ("missing"). Values for {@link SingleReadStatFunction} are only computed for "proper"
 * pairs; for {@link PairEndReadStatFunction} "missing" reads are not considered.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class ProperStatWindowCalculator implements Locatable {

    /** Column name for 'total' reads (first column in output). */
    public static final String DEFAULT_TOTAL_COLUMN_NAME = "total";
    /** Column name for 'proper' reads (second column in output). */
    public static final String DEFAULT_PROPER_COLUMN_NAME = "proper";
    /** Column name for 'missing' reads (third column in output). */
    public static final String DEFAULT_MISSING_COLUMN_NAME = "missing";

    // running statistics are always the same and should be in sync with the formatColumnNames/formatValues method
    private static final List<String> DEFAULT_COLUMN_NAMES = Arrays.asList(
            DEFAULT_TOTAL_COLUMN_NAME, DEFAULT_PROPER_COLUMN_NAME, DEFAULT_MISSING_COLUMN_NAME
    );

    // store the interval data
    private final SimpleInterval window;

    // store the current results (pairs)
    private final Map<PairEndReadStatFunction, Object> pairEndResult;
    // store the current results (single)
    private final Map<SingleReadStatFunction, Object> singleResult;

    // TODO: check if we can improve performance with a differen Map and a sensible cache value
    // TODO: maybe a good idea is to use the PatriciaTrie<Map<PairEndReadStatFunction, Object>> instead
    // TODO: an avoid the ProperReadHash
    // stores a cache of Hash for the first read and list of temporary stats
    // the key is the read name hashCode
    private final Map<ProperReadHash, Map<PairEndReadStatFunction, Object>> firstCache = new HashMap<>();

    // cached column names
    private final List<String> columnNames;
    // total reads
    private int total = 0;
    // total proper-pair reads
    private int proper = 0;

    /**
     * Constructor for a window calculator with default column names.
     *
     * @param window      interval to compute the statistic.
     * @param singleStats single-read statistics to compute (init should be already called)
     * @param pairStats   pair-end statistics to compute (init should be already called).
     */
    ProperStatWindowCalculator(final SimpleInterval window,
            final List<SingleReadStatFunction> singleStats,
            final List<PairEndReadStatFunction> pairStats) {
        this(window, singleStats, pairStats, createColumnNames(singleStats, pairStats));
    }

    /**
     * Constructor for a window calculator with different column names.
     *
     * @param window      interval to compute the statistic.
     * @param singleStats single-read statistics to compute (init should be already called)
     * @param pairStats   pair-end statistics to compute (init should be already called).
     * @param columnNames column-names for the statistics.
     */
    ProperStatWindowCalculator(final SimpleInterval window,
            final List<SingleReadStatFunction> singleStats,
            final List<PairEndReadStatFunction> pairStats,
            final List<String> columnNames) {
        this.window = Utils.nonNull(window);
        // check non-null lists
        Utils.nonNull(singleStats);
        Utils.nonNull(pairStats);

        // initialize column names
        this.columnNames = Utils.nonNull(columnNames);
        // validate number of columns
        Utils.validateArg(
                columnNames.size() == singleStats.size() + pairStats.size() + DEFAULT_COLUMN_NAMES
                        .size(),
                () -> "invalid number of columns");

        // initialize maps
        this.singleResult = initResultMap(singleStats);
        this.pairEndResult = initResultMap(pairStats);
    }

    // initialize the result map
    private static <T> Map<T, Object> initResultMap(final List<T> list) {
        final Map<T, Object> result = new LinkedHashMap<>(list.size());
        list.forEach(s -> result.put(s, null));
        return result;
    }

    /**
     * Create column names for a list of statistics.
     *
     * <p>Use this method on construction to modify the number.
     */
    static List<String> createColumnNames(final List<SingleReadStatFunction> singleStats,
            final List<PairEndReadStatFunction> pairStats) {
        final List<String> columns = new ArrayList<>(
                singleStats.size() + pairStats.size() + DEFAULT_COLUMN_NAMES.size());
        columns.addAll(DEFAULT_COLUMN_NAMES);
        singleStats.stream().map(SingleReadStatFunction::getStatName).forEach(columns::add);
        pairStats.stream().map(PairEndReadStatFunction::getStatName).forEach(columns::add);
        return columns;
    }

    /**
     * Gets the column names of windows for the statistics computed in the window.
     *
     * @return all the column names, in order, for the statistics computed on the window.
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    @Override
    public String getContig() {
        return window.getContig();
    }

    @Override
    public int getStart() {
        return window.getStart();
    }

    @Override
    public int getEnd() {
        return window.getEnd();
    }

    /**
     * Checks if the window has some cached pairs.
     *
     * <p>Cached pairs are the ones where the mate was not included yet. They might not be included
     * in the total/proper if the mate is the one added to the window.
     *
     * @return {@code true} if there are cached pairs; {@code false} otherwise.
     */
    public boolean hasCachedPairs() {
        return !firstCache.isEmpty();
    }

    /**
     * Gets the number of missing reads (out of total).
     *
     * <p>Note: It is possible that there are 0 missing reads, but still cached-pairs.
     *
     * @return number of missing reads.
     */
    public long getMissing() {
        return firstCache.keySet().stream().filter(hash -> hash.isInWin).count();
    }

    /**
     * Checks if the fragment (read and its pair, if any) overlaps with this window.
     *
     * <p>This method can be used ot check if a read might be included on a window.
     *
     * @param read read containing the information from the fragment.
     * @return {@code true} if the whole-fragment overlaps; {@code false} otherwise.
     */
    public boolean fragmentOverlaps(final GATKRead read) {
        // either the read is in the window or the read is proper pair and the mate starts on the window
        final boolean isInWin = startOverlaps(read.getContig(), read.getStart());
        if (isInWin) {
            return true;
        }
        return mappedPairSameContig(read) && startOverlaps(read.getMateContig(), read.getMateStart());
    }

    /**
     * Add the read to the window calculator.
     *
     * <p>Statistics are computed as following:
     * <ul>
     *     <li>Single reads overlapping the window are added to the total.</li>
     *     <li>Single "proper" reads are passed to single-read statistics.</li>
     *     <li>Pairs where at least one of the reads is on the window are cached, but not computed
     *         until the pair is added.</li>
     * </ul>
     *
     * @param read read to be added.
     *
     * @return {@code true} if the read was added or downstream reads (in coordinate order) should
     * be added; {@code false} otherwise.
     */
    public void addRead(final GATKRead read) {
        // compute if the read is in the window or it is properly paired
        final boolean isInWin = startOverlaps(read.getContig(), read.getStart());
        final boolean isProper = mappedPairSameContig(read);

        if (isInWin) {
            addSingleRead(read, isProper);
        }

        // only if it is proper and in the window or if it is cached
        if (isProper && (isInWin || startOverlaps(read.getMateContig(), read.getMateStart()))) {
            // compute the readKey and check if there is a cached value
            final ProperReadHash readKey = new ProperReadHash(read, isInWin);
            final Map<PairEndReadStatFunction, Object> cacheValues = firstCache.remove(readKey);

            if (cacheValues == null) {
                // if not present, add to cache
                addToCache(readKey, read);
            } else {
                // otherwise, compute from cache
                computeFromCache(cacheValues, read);
            }
        }
    }

    /**
     * Add a single read, updating counts and statistics.
     *
     * <p>Note: statistics are only computed if the read is considered proper.
     *
     * @param read     read to add.
     * @param isProper pre-computed proper status.
     */
    @SuppressWarnings("unchecked")
    private void addSingleRead(final GATKRead read, final boolean isProper) {
        // update the total count of reads
        total++;
        if (isProper) {
            proper++;
            for (final SingleReadStatFunction func : singleResult.keySet()) {
                singleResult.compute(func, (f, v) -> f.reduce(f.compute(read), v));
            }
        }
    }

    /**
     * Add to the cache the intermediate values for the first read.
     *
     * @param readKey pre-computed read hash.
     * @param read    read to add to the cache.
     */
    private void addToCache(final ProperReadHash readKey, final GATKRead read) {
        final Map<PairEndReadStatFunction, Object> cache = new HashMap<>(pairEndResult.size());
        for (final PairEndReadStatFunction f : pairEndResult.keySet()) {
            cache.put(f, f.computeIntermediateFirst(read));
        }
        firstCache.put(readKey, cache);
    }

    /**
     * Computes the final result for the pair using the cache values, and reduce it for the final
     * results.
     *
     * @param cacheValues cache values for the pair.
     * @param read        the second read added for the pair.
     */
    @SuppressWarnings("unchecked")
    private void computeFromCache(final Map<PairEndReadStatFunction, Object> cacheValues,
            final GATKRead read) {
        // compute the new value for each cache entry
        for (final Map.Entry<PairEndReadStatFunction, Object> cache : cacheValues.entrySet()) {
            final Object current = cache.getKey().mergePairValues(
                    cache.getValue(),
                    cache.getKey().computeIntermediateSecond(read));

            pairEndResult.compute(cache.getKey(), (func, prev) -> func.reduce(current, prev));
        }
    }

    /**
     * Check if the read is proper pair for the sake of this calculator.
     *
     * @param read the read to check.
     *
     * @return {@code true} if it is proper; {@code false} otherwise.
     */
    private static boolean mappedPairSameContig(final GATKRead read) {
        // the unmapped check is important to do not have NPE
        return !read.isUnmapped() &&
                read.isPaired() &&
                !read.mateIsUnmapped() &&
                read.getContig().equals(read.getMateContig());
    }

    /**
     * Check if the position overlaps with the window.
     *
     * @param contig the contig.
     * @param start  the start position.
     *
     * @return {@code true} if it overlaps; {@code false} otherwise.
     */
    private boolean startOverlaps(final String contig, final int start) {
        // this is similar to the implementation of Locatable.overlaps
        // but without constructing a new object such as:
        // new SimpleFeature(contig, start, start).overlaps(window);
        return contig != null && Objects.equals(contig, window.getContig()) &&
                CoordMath.overlaps(start, start, window.getStart(), window.getEnd());
    }

    /**
     * Summarize the window in a table format.
     *
     * <p>This method recomputes the feature in every call, so it might be expensive if lots
     * of statistics are computed.
     *
     * <p>Note: this method might return different values if {@link #addRead(GATKRead)} is called
     * afterwards.
     *
     * @return table feature with the information for the statistics.
     */
    public TableFeature format() {
        // first, add the total/proper/missing
        final List<String> values = new ArrayList<>(columnNames.size());
        values.add(String.valueOf(total));
        values.add(String.valueOf(proper));
        values.add(String.valueOf(getMissing()));

        // now, add all the values from the windows
        addFormatted(singleResult, values);
        addFormatted(pairEndResult, values);

        return new TableFeature(window, values, columnNames);
    }

    /**
     * Format the results as a String and add to a container of results.
     *
     * @param results   results to format.
     * @param formatted container of formatted results.
     */
    @SuppressWarnings("unchecked")
    private static void addFormatted(final Map<? extends StatFunction, Object> results,
            final List<String> formatted) {
        results.entrySet().stream().map(e -> e.getKey().tableResultFormat(e.getValue()))
                .forEach(formatted::add);
    }

    /**
     * Returns an string representation of this calculator.
     *
     * <p>Note: this string does not show the state of the calculation, but just the definition.
     * For formatting purposes, use {@link #format()}.
     *
     * @return calculator representation.
     */
    public String toString() {
        // for debug purposes only!
        return String.format("%s@%s%s", this.getClass().getSimpleName(), window, columnNames);
    }

    // helper class to store proper reads hash information
    // the hashCode is computed only once on construction
    private static class ProperReadHash {
        private final String name;
        private final int hashCode;
        public final boolean isInWin;

        private ProperReadHash(final GATKRead read, final boolean isInWin) {
            this.name = read.getName();
            this.hashCode = this.name.hashCode();
            this.isInWin = isInWin;
        }

        @Override
        public boolean equals(final Object obj) {
            return (obj instanceof ProperReadHash) && name.equals(((ProperReadHash) obj).name);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

    }
}
