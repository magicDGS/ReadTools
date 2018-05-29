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
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.IOUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.codecs.table.TableFeature;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadCoordinateComparator;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Engine to compute several {@link StatFunction} over the provided windows.
 *
 * <p>This class takes a list of intervals and {@link StatFunction} and expects reads to be added
 * to compute on-the-fly the statistics provided. Nevertheless, the engine has the following
 * assumptions that the caller of {@link #addRead(GATKRead)} should met:
 *
 * <p>The engine has the following assumptions to work:
 *
 * <ul>
 *     <li>Provided intervals are coordinate-sorted.</li>
 *     <li>Reads are added in a coordinate-sorted manner.</li>
 *     <li>Only two fragments per template will be added (supplementary/secondary alignments are filtered.</li>
 * </ul>
 *
 * <p>If these assumptions are not met, windows are output without all the data and statistics
 * would be incorrect.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ProperStatWindowEngine implements Closeable {

    private final Logger logger = LogManager.getLogger(this.getClass());

    // list with all output windows
    // TODO: possible performance improvements:
    // TODO: - use IntervalsSkipList: retrieve overlapping intervals per read and add only there
    // TODO: - it also requires to retrieve overlapping windows for the mate (extract location)
    private final SortedMap<String, List<ProperStatWindowCalculator>> windowsPerContig;

    // cache column names
    private final List<String> columnNames;
    // option to print all the windows
    private final boolean printAll;
    // ouptut writer
    private final PrintStream writer;

    /**
     * Constructor for the engine.
     *
     * @param dict sequence dictionary to use with intervals.
     * @param intervals coordinate-sorted intervals to compute the statistics.
     * @param singleStats statistics for single-reads (without considering pairs).
     * @param pairStats statistics for pair-reads (considering the mate).
     * @param output output file to populate the results.
     * @param printAll if {@code false}, windows with 0 reads will be discarded; otherwise,
     *                 they will be populated.
     * @throws IOException if an I/O error occurs while opening the output.
     */
    public ProperStatWindowEngine(final SAMSequenceDictionary dict,
            final List<SimpleInterval> intervals,
            final List<SingleReadStatFunction> singleStats,
            final List<PairEndReadStatFunction> pairStats,
            final Path output,
            final boolean printAll) throws IOException {
        // TODO: maybe support also compressed output if end with a compressed extension?
        this(dict, intervals, singleStats, pairStats, Files.newOutputStream(output), printAll);
    }

    /**
     * Testing constructor.
     *
     * <p>Use a {@link OutputStream} storing the results to test them.
     *
     * @param dict sequence dictionary to use with intervals.
     * @param intervals coordinate-sorted intervals to compute the statistics.
     * @param singleStats statistics for single-reads (without considering pairs).
     * @param pairStats statistics for pair-reads (considering the mate).
     * @param stream stream for the output.
     * @param printAll if {@code false}, windows with 0 reads will be discarded; otherwise,
     *                 they will be populated.
     */
    @VisibleForTesting
    ProperStatWindowEngine(final SAMSequenceDictionary dict,
            final List<SimpleInterval> intervals,
            final List<SingleReadStatFunction> singleStats,
            final List<PairEndReadStatFunction> pairStats,
            final OutputStream stream,
            final boolean printAll) {
        Utils.nonNull(intervals);
        Utils.nonNull(singleStats);
        Utils.nonNull(pairStats);
        Utils.nonNull(stream);

        // create first the columnNames
        this.columnNames =
                ProperStatWindowCalculator.createColumnNames(singleStats, pairStats);

        // create the TreeMap with sorting order using the dictionary
        this.windowsPerContig = new TreeMap<>(Comparator.comparingInt(dict::getSequenceIndex));
        intervals.stream()
                .map(i -> new ProperStatWindowCalculator(i, singleStats, pairStats, columnNames))
                .forEach(swc -> windowsPerContig.compute(swc.getContig(), (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(swc);
                    return v;
                }));


        // create the writer and write first the header
        this.writer = new PrintStream(stream);
        this.printAll = printAll;
        writeHeader();
    }

    /**
     * Adds the read to the computation engine.
     *
     * <p>Note that the caller is responsible of assesing the following requirements:
     *
     * <ul>
     *     <li>Reads are added in a coordinate sorted ordering.</li>
     *     <li>Supplementary/Secondary alignments are not passed to this method.</li>
     * </ul>
     *
     * <p>Results are unpredictable otherwise.
     *
     * @param read the read to be added.
     */
    public void addRead(final GATKRead read) {
        final List<ProperStatWindowCalculator> wins = flushUpToRead(read);
        for (final ProperStatWindowCalculator w: wins) {
            // TODO: performance improvement - should add a check for break the for loop once
            // TODO: there are no-windows requiring this read
            // TODO: if using IntervalsSkipList it might be easier by querying needed ones
            w.addRead(read);
        }
    }

    /**
     * Flushes the queue of windows up to the provided read.
     *
     * @param read next read to be added.
     * @return windows overlapping with the read.
     */
    private List<ProperStatWindowCalculator> flushUpToRead(final GATKRead read) {
        // edge case where the windows are already finalized
        if (windowsPerContig.isEmpty()) {
            return Collections.emptyList();
        }

        // store the first key
        String firstKey = windowsPerContig.firstKey();
        // only if the read is mapped, otherwise does not have a contig
        if (!read.isUnmapped()) {
            logger.debug("Flush up to {} ({} contigs)", read::toString, windowsPerContig::size);
            final String readContig = read.getContig();
            // remove the contigs that are before the current read
            while (!windowsPerContig.isEmpty() && windowsPerContig.comparator().compare(firstKey, readContig) < 0) {
                logger.debug("Output contig {}", firstKey);
                windowsPerContig.remove(firstKey).forEach(this::maybeWriteWindow);
                firstKey = (windowsPerContig.isEmpty()) ? null : windowsPerContig.firstKey();
            }
            logger.debug("After flush {} contigs remain", windowsPerContig::size);
        }
        // TODO: performance improvement - here we can also flush the List<ProperStatWindowCalculator>
        // TODO: if the list is sorted, we should check for overlapping windows with the left-most coordinate (read or mate)

        return (firstKey == null) ? Collections.emptyList() : windowsPerContig.get(firstKey);
    }

    /**
     * Writes the header for the output file.
     */
    private void writeHeader() {
        // the column names from the first window is the one to take into account
        writer.println("window\t" + Utils.join("\t", columnNames));
    }

    /**
     * Writes the window to disk if it is requested (e.g., more than 0 or print-all is included)
     *
     * @param win the window to be printed.
     */
    private void maybeWriteWindow(final ProperStatWindowCalculator win) {
        final TableFeature feature = win.toTableFeature();
        if (printAll || Integer.valueOf(feature.get("total")) != 0) {
            writeWindow(feature);
        }
    }

    /**
     * Writes the feature record.
     *
     * @param feature the feature to be written.
     */
    private void writeWindow(final TableFeature feature) {
        if (logger.isWarnEnabled() ) {
            final int missing = Integer.valueOf(feature.get("missing"));
            if (missing != 0) {
                logger.warn("{} missing pairs for {}", missing, IntervalUtils.locatableToString(feature));
            }
        }
        writer.println(feature.toString());
    }

    /**
     * Closes the engine, including the underlying writer.
     *
     * <p>Note: it is important to call this method to ensure that all the windows are output.
     *
     * @throws IOException if there is an IO error.
     */
    @Override
    public void close() throws IOException {
        // write all the remaining windows
        logger.debug("Output last {} contigs", windowsPerContig::size);
        windowsPerContig.forEach((key, value) -> value.forEach(this::maybeWriteWindow));
        // finally, close the writer
        logger.debug("Clear windows");
        windowsPerContig.clear();
        logger.debug("Closing writer");
        writer.close();
    }
}
