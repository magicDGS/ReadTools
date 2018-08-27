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
 * {@link SingleReadStatFunction} and {@link PairEndReadStatFunction}. Users should provide
 * reads to the {@link #addRead(GATKRead)} method to include the read on the calculation (if
 * applicable). Importantly, supplementary/secondary alignment should not be added because the
 * calculator expects two fragments per template (unpredictable results otherwise); unmapped reads
 * can be added, but they would be ignored as they are not included on the window.
 *
 * <p>By default, the calculator computes all the reads that are in the window ("total"), proper
 * pairs included ("proper", as defined in {@link #mappedPairSameContig(GATKRead)}) and pairs
 * missing in the file ("missing").
 *
 * <p>Computation for both single and pair statistics are only triggered for "proper pairs". In the
 * case of {@link PairEndReadStatFunction}, pairs with "missing" reads are not considered.
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

    // TODO: performance improvements related with the cache (https://github.com/magicDGS/ReadTools/issues/451)
    // stores a cache of hash for the first added read and list of temporary stats
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
     * <p>Note: for all the statistics, the {@link StatFunction#init()} method should be already
     * called. Otherwise, the statistic might throw an exception or be wrong.
     *
     * @param window      interval to compute the statistic.
     * @param singleStats single-read statistics to compute.
     * @param pairStats   pair-end statistics to compute.
     */
    ProperStatWindowCalculator(final SimpleInterval window,
            final List<SingleReadStatFunction> singleStats,
            final List<PairEndReadStatFunction> pairStats) {
        this(window, singleStats, pairStats, createColumnNames(singleStats, pairStats));
    }

    /**
     * Constructor for a window calculator with different column names.
     *
     * <p>Note: for all the statistics, the {@link StatFunction#init()} method should be already
     * called. Otherwise, the statistic might throw an exception or be wrong.
     *
     * @param window      interval to compute the statistic.
     * @param singleStats single-read statistics to compute.
     * @param pairStats   pair-end statistics to compute.
     * @param columnNames column-names for the statistics. Best practice is to call
     *                    {@link #createColumnNames(List, List)} to cache the column names and
     *                    construct several calculators for the same set of statistics.
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
    protected static List<String> createColumnNames(final List<SingleReadStatFunction> singleStats,
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
     * Adds the read to the window calculator.
     *
     * <p>WARNING: this method should not be called if {@link GATKRead#isSecondaryAlignment()}
     * or {@link GATKRead#isSupplementaryAlignment()} is {@code true}. Only two fragments are
     * expected per template - adding secondary/supplementary alignments might produce unpredictable
     * results.
     *
     * <p>Statistics are computed as following:
     * <ul>
     *     <li>Single reads overlapping the window are added to the total.</li>
     *     <li>Single "proper" reads are passed to single-read statistics.</li>
     *     <li>Pairs where at least one of the reads is on the window are cached, but not computed
     *         until the pair is added.</li>
     * </ul>
     *
     * @param read alignment to be added.
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

        // TODO: maybe check first if there is any pair-end stat to check (performance improvement - https://github.com/magicDGS/ReadTools/issues/451)
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
     * Adds a single read, updating counts and statistics.
     *
     * <p>Note: statistics are only computed if the read is considered proper.
     *
     * @param read     alignment to add.
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
     * Adds to the cache the intermediate values for the first read.
     *
     * @param readKey pre-computed read hash.
     * @param read    alignment to add to the cache.
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
     * Checks if the read is "proper pair".
     *
     * <p>A "proper" pair" for the calculator is the one where the read and the mate are mapped on
     * the same contig. Note that unmapped reads/mates with assigned positions are not "proper" with
     * this definition.
     *
     * @param read alignment to check.
     *
     * @return {@code true} if it is part of a "proper pair"; {@code false} otherwise.
     */
    private static boolean mappedPairSameContig(final GATKRead read) {
        // the unmapped check is important to do not have NPE
        return !read.isUnmapped() &&
                read.isPaired() &&
                !read.mateIsUnmapped() &&
                read.getContig().equals(read.getMateContig());
    }

    /**
     * Checks if the position overlaps with the window.
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
     * Summarizes the window in a table format.
     *
     * <p>This method recomputes the feature in every call, so it might be expensive if lots
     * of statistics are computed.
     *
     * <p>Note: this method might return different values if {@link #addRead(GATKRead)} is called
     * afterwards.
     *
     * @return table feature with the information for the statistics.
     */
    public TableFeature toTableFeature() {
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
     * Formats the results as a String and add to a container of results.
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
     * For formatting purposes, use {@link #toTableFeature()}.
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
        // true if this hash comes from a read that was on the window; false otherwise
        // this is important to mark reads as missing (only if they are in the window)
        private final boolean isInWin;

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
