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

import htsjdk.samtools.SAMSequenceDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.codecs.table.TableFeature;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: documentation
public class WindowStatsEngine implements Closeable {

    private final Logger logger = LogManager.getLogger(this.getClass());

    // list with all output windows
    // TODO: possible performance improvements:
    // TODO: - use IntervalsSkipList: retrieve overlapping intervals per read and add only there
    // TODO: - it also requires to retrieve overlapping windows for the mate
    private final SortedMap<String, List<StatWindowCalculator>> windowsPerContig;

    // cache column names
    private final List<String> columnNames;

    // option to print all the windows
    private final boolean printAll;

    // ouptut writer
    // TODO: maybe choose a different implementation for writing
    // TODO: in addition, this does not allow to check for writing errors!
    private final PrintStream writer;

    public WindowStatsEngine(final SAMSequenceDictionary dict,
            final List<SimpleInterval> intervals,
            final List<SingleReadStatFunction> singleStats,
            final List<PairEndReadStatFunction> pairStats,
            final Path output, final boolean printAll) throws IOException {
        Utils.nonNull(intervals);
        Utils.nonNull(singleStats);
        Utils.nonNull(pairStats);
        Utils.nonNull(output);

        // create first the columnNames
        this.columnNames =
                StatWindowCalculator.createColumnNames(singleStats, pairStats);

        // create the TreeMap and add all the sequences from the dictionary
        this.windowsPerContig = new TreeMap<>(Comparator.comparingInt(dict::getSequenceIndex));
        dict.getSequences().forEach(s -> windowsPerContig.put(s.getSequenceName(), new ArrayList<>()));
        // add all the intervals for compute the interval
        intervals.stream()
                .map(i -> new StatWindowCalculator(i, singleStats, pairStats, columnNames))
                .forEach(swc -> windowsPerContig.compute(swc.getContig(), (k, v) -> {
                    v.add(swc); return v;
                }));


        // create the writer and write first the header
        // TODO: maybe allow also compressed output?
        this.writer = new PrintStream(Files.newOutputStream(output));
        this.printAll = printAll;
        writeHeader();
    }

    /**
     * Adds the read to the computation engine.
     *
     * <p>Note that it is expected to be added in coordinate sorted order.
     *
     * @param read the read to be added.
     */
    public void addRead(final GATKRead read) {
        logger.debug("Adding {}", () -> read);
        final List<StatWindowCalculator> wins = flushUpToRead(read);
        for (final StatWindowCalculator w: wins) {
            // TODO: performance improvement - should add a check for break the for loop once
            // TODO: there are windows that does not require this read
            // TODO: it also requires that the the window list/queue is ordered
            w.addRead(read);
        }
    }

    /**
     * Flushes the queue of windows up to the provided read.
     *
     * @param read next read to be added.
     * @returns current read windows.
     */
    private List<StatWindowCalculator> flushUpToRead(final GATKRead read) {
        logger.debug("Before flush: {} contigs", windowsPerContig::size);

        // store the first key
        String firstKey = (windowsPerContig.isEmpty()) ? null : windowsPerContig.firstKey();
        // only if the read is mapped, otherwise is unmapped
        if (!read.isUnmapped()) {
            final String readContig = read.getContig();
            while (readContig.equals(firstKey)) {
                logger.debug("Flushing contig {}", firstKey);
                windowsPerContig.remove(firstKey).forEach(this::maybeWriteWindow);
                firstKey = (windowsPerContig.isEmpty()) ? null : windowsPerContig.firstKey();
            }
        }

        logger.debug("After flush: {} contigs", windowsPerContig::size);
        return (firstKey == null) ? Collections.emptyList() : windowsPerContig.get(firstKey);
    }

    /**
     * Writes the header for the output file.
     */
    private void writeHeader() {
        logger.debug("Writing header");
        // the column names from the first window is the one to take into account
        writer.println("window\t" + Utils.join("\t", columnNames));
    }

    /**
     * Writes the window to disk if it is requested (e.g., more than 0 or print-all is included)
     *
     * @param win the window to be printed.
     */
    private void maybeWriteWindow(final StatWindowCalculator win) {
        final TableFeature feature = win.format();
        if (printAll || Integer.valueOf(feature.get("total")) != 0) {
            writeWindow(feature);
        } else {
            logger.debug("Do not output 0-reads feature: {} ", () -> feature);
        }
    }

    /**
     * Writes the feature record.
     *
     * @param feature the feature to be written.
     */
    private void writeWindow(final TableFeature feature) {
        if (logger.isWarnEnabled()) {
            final int missing = Integer.valueOf(feature.get("missing"));
            if (missing != 0) {
                logger.warn("{} missing pairs for {}",
                        missing, IntervalUtils.locatableToString(feature));
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
        windowsPerContig.entrySet().forEach(k -> {
            logger.debug("Output {} windows for {}", () -> k.getValue().size(), k::getKey);
            k.getValue().forEach(this::maybeWriteWindow);
        });
        // finally, close the writer
        logger.debug("Clear windows");
        windowsPerContig.clear();
        logger.debug("Closing writer");
        writer.close();
    }
}
