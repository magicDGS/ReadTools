/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
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
package org.magicdgs.readtools.utils.barcodes.legacy.dictionary.decoder;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.metrics.barcodes.BarcodeDetector;
import org.magicdgs.readtools.metrics.barcodes.BarcodeStat;
import org.magicdgs.readtools.metrics.barcodes.MatcherStat;
import org.magicdgs.readtools.utils.barcodes.BarcodeDecoder;
import org.magicdgs.readtools.utils.barcodes.BarcodeSet;
import org.magicdgs.readtools.utils.read.RTReadUtils;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.util.Histogram;
import htsjdk.tribble.util.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Helper class for matching sequenced barcodes with the indexes contained in a barcode dictionary.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class LegacyBarcodeDecoder implements BarcodeDecoder {

    private final Logger logger;

    // the barcode dictionary to match against
    private final BarcodeSet dictionary;

    // Switch for considering N as mismatches in this decoder
    private final boolean nAsMismatches;

    // maximum mismatches for each index
    private final int[] maxMismatches;

    // minimum number of differences between the best match and the second best for each index
    private final int[] minDifferenceWithSecond;

    // maximum number of Ns for match a barcode
    private final int maxN;

    // metrics header for this detector
    private final BarcodeDetector metricHeader;

    // statistics for each combined barcode
    private Map<String, MatcherStat> stats;

    // statistics of each barcode by index (list entry)
    private List<Map<String, BarcodeStat>> barcodeStats;

    // histogram of mismatches of each barcode by index (list entry)
    private List<Map<String, Histogram<Integer>>> mismatchesHist;

    // running statistics for each barcode by index (list entry)
    private List<Map<String, MathUtils.RunningStat>> nMean;

    /**
     * Default constructor.
     *
     * @param dictionary              non-null barcode dictionary with indexes to match.
     * @param nAsMismatches           if {@code true}, the Ns count as mismatches.
     * @param maxMismatches           maximum number of mismatches allowed (for each barcode).
     * @param minDifferenceWithSecond the minimum difference in the number of mismatches between
     *                                the first and the second best barcodes (for each barcode).
     *
     * @throws IllegalArgumentException if the thresholds are arrays with different lengths than
     *                                  the number of barcodes in the dictionary.
     */
    public LegacyBarcodeDecoder(final BarcodeSet dictionary, final int maxN,
            final boolean nAsMismatches, final int[] maxMismatches,
            final int[] minDifferenceWithSecond) {
        this.dictionary = Utils.nonNull(dictionary, "null dictionary");

        Utils.validateArg(maxN >= 0, "negative maxN");
        this.maxN = maxN;

        this.nAsMismatches = nAsMismatches;

        this.maxMismatches = Utils.nonNull(maxMismatches, "null maxMismatches");
        Utils.validateArg(maxMismatches.length == dictionary.getMaxNumberOfIndexes(),
                "maxMismatches.size() != number of barcodes");

        this.minDifferenceWithSecond = Utils.nonNull(minDifferenceWithSecond,
                "null minDifferenceWithSecond");
        Utils.validateArg(minDifferenceWithSecond.length == dictionary.getMaxNumberOfIndexes(),
                "minDifferenceWithSecond.size() != number of barcodes");

        this.metricHeader = new BarcodeDetector();
        this.logger = LogManager.getLogger(this.getClass());
        initStats();
    }

    // initilialize the statistics on construction
    private void initStats() {
        // get the statistics for the decoding
        stats = new LinkedHashMap<>();
        final List<String> sampleNames = dictionary.asReadGroupList().stream()
                .map(SAMReadGroupRecord::getSample).collect(Collectors.toList());
        for (int i = 0; i < dictionary.size(); i++) {
            final String combined = dictionary.getJoinedBarcodesForSample(i);
            stats.put(combined, new MatcherStat(combined, sampleNames.get(i)));
        }
        stats.put(BarcodeMatch.UNKNOWN_STRING,
                new MatcherStat(BarcodeMatch.UNKNOWN_STRING, BarcodeMatch.UNKNOWN_STRING));
        // get the statistics for the barcode
        barcodeStats = new LinkedList<>();
        mismatchesHist = new LinkedList<>();
        nMean = new LinkedList<>();
        final String suffix;
        if (dictionary.getMaxNumberOfIndexes() == 1) {
            suffix = null;
        } else {
            suffix = "_";
        }
        for (int j = 0; j < dictionary.getMaxNumberOfIndexes(); j++) {
            final Map<String, BarcodeStat> bar = new LinkedHashMap<>();
            final Map<String, Histogram<Integer>> histM = new LinkedHashMap<>();
            final Map<String, MathUtils.RunningStat> mean = new LinkedHashMap<>();
            for (final String b : dictionary.getSetBarcodesForIndex(j)) {
                final BarcodeStat s =
                        new BarcodeStat((suffix == null) ? b : String.format("%s_%s", b, j + 1));
                bar.put(b, s);
                histM.put(b, new Histogram<>("mismatches", s.SEQUENCE));
                mean.put(b, new MathUtils.RunningStat());
            }
            barcodeStats.add(bar);
            mismatchesHist.add(histM);
            nMean.add(mean);
        }
    }

    /** Gets the barcode dictionary associated with this object. */
    @Override
    public BarcodeSet getBarcodeSet() {
        return dictionary;
    }

    /**
     * Assigns the read group to a read using the raw barcodes. If there is no raw barcode, it is
     * assigned to the UNKNOWN one; otherwise, it is assigned by matching the barcodes using the
     * pipeline in {@link #getBestBarcodeString(String...)}.
     *
     * @param read the read to asssing the read group.
     * @throws UserException.MalformedFile if the raw barcode length and dictionary number of
     * indexes differs.
     */
    @Override
    public void assignReadGroup(final GATKRead read) {
        final String[] barcodes = RTReadUtils.getRawBarcodes(read);
        logger.debug("Raw barcodes: {}", () -> Arrays.toString(barcodes));
        // get the best barcode
        if (barcodes.length == 0) {
            // log a warning if there is no barcode
            logger.warn("{} read does not have raw barcodes: assigned to {} Read Group",
                    read::getName, () -> dictionary.getUnknown().getId());
            read.setReadGroup(dictionary.getUnknown().getReadGroupId());
            stats.get(BarcodeMatch.UNKNOWN_STRING).RECORDS++;
        } else if (barcodes.length != dictionary.getMaxNumberOfIndexes() ) {
            // throw an exception if there is a mismatch with the number of barcodes
            throw new UserException.MalformedFile(String.format(
                    "Barcode dictionary has %s indexes, but read contains %s barcodes."
                            + " Failing read: %s (%s)",
                    dictionary.getMaxNumberOfIndexes(), barcodes.length,
                    read.getName(), String.join(RTDefaults.BARCODE_INDEX_DELIMITER, barcodes)));
        } else {
            // assigned the barcode only if it has the same number as in the dictionary
            final String bestBarcode = getBestBarcodeString(barcodes);
            logger.debug("Detected barcode: {}", () -> bestBarcode);
            final SAMReadGroupRecord readGroup = dictionary.getReadGroupForJoinedBarcode(bestBarcode);
            logger.debug("Detected RG: {}", () -> readGroup);
            read.setReadGroup(readGroup.getReadGroupId());
        }
    }

    /**
     * Gets the best barcode using the BarcodeMatch approach.
     *
     * WARNING: does not check the number of barcodes in the input array.
     *
     * @param barcode the array of barcodes to match.
     *
     * @return the best real barcode in the dictionary (pasted in order if there are more than one).
     */
    private String getBestBarcodeString(final String... barcode) {
        // this assumes that the barcodes are not empty and/or null
        final List<BarcodeMatch> allMatchs = IntStream.range(0, dictionary.getMaxNumberOfIndexes())
                // get the BarcodeMatch for the set of indexes
                .mapToObj(index -> BarcodeMatch.getBestBarcodeMatch(index, barcode[index],
                        dictionary.getSetBarcodesForIndex(index), nAsMismatches))
                // filter only the ones which pass the filters and update the metrics
                .filter(this::passFiltersAndUpdateMetrics)
                .collect(Collectors.toList());
        // early termination
        final String detectedBarcode = (allMatchs.isEmpty())
                ? BarcodeMatch.UNKNOWN_STRING : getBestBarcodeBySampleMajority(allMatchs);
        // update statistics
        stats.get(detectedBarcode).RECORDS++;
        return detectedBarcode;
    }

    /**
     * Performs the algorithm to identify the sample with several barcodes.
     *
     * If a barcode could identify uniquely the sample, returns the combined barcode for that
     * sample. If not, it computes how many times every sample is identify by every barcode. If
     * there is a tie, {@link BarcodeMatch#UNKNOWN_STRING} is returned; otherwise, the barcode for
     * the sample with higher counts is returned.
     *
     * @param matches a list of matches against the barcode dictionary.
     *
     * @return best barcode detected with this algorithm; {@link BarcodeMatch#UNKNOWN_STRING} if
     * impossible to determine unambiguously.
     */
    private String getBestBarcodeBySampleMajority(final List<BarcodeMatch> matches) {
        // map sample indexes and number of times that it occurs
        final Map<Integer, Integer> countsBySample = new HashMap<>();
        // accumulate for each barcode match how many times appears each sample
        for (final BarcodeMatch current : matches) {
            final List<String> allBarcodes =
                    dictionary.getSampleBarcodesForIndex(current.getIndexNumber());
            final int sampleIndex = allBarcodes.indexOf(current.getBarcode());
            // check if it is unique for this set
            if (dictionary.isBarcodeUniqueInAt(current.getBarcode(), current.getIndexNumber())) {
                // return directly the barcode
                return dictionary.getJoinedBarcodesForSample(sampleIndex);
            } else {
                for (int i = sampleIndex; i < allBarcodes.size(); i++) {
                    if (allBarcodes.get(i).equals(current.getBarcode())) {
                        countsBySample.merge(i, 1, (oldVal, one) -> oldVal + one);
                    }
                }
            }
        }
        if (countsBySample.isEmpty()) {
            return BarcodeMatch.UNKNOWN_STRING;
        }
        // if we reach this point, there are non unique barcode that identifies the sample
        // obtain the maximum count
        final int maxCount = Collections.max(countsBySample.values());
        final List<Integer> sampleIndexesWithMax = countsBySample.entrySet().stream()
                .filter(e -> e.getValue() == maxCount)
                .map(Map.Entry::getKey).collect(Collectors.toList());
        return (sampleIndexesWithMax.size() != 1) ? BarcodeMatch.UNKNOWN_STRING
                : dictionary.getJoinedBarcodesForSample(sampleIndexesWithMax.get(0));
    }

    /**
     * Updates the statistics for the barcode stored in the match (if it is matched), and check
     * if it passes all the filters. The header for the metrics file is also updated according to
     * the filtered barcodes.
     *
     * @param match the match to filter and update.
     *
     * @return {@code true} if the match pass the filters; {@code false} otherwise.
     */
    private boolean passFiltersAndUpdateMetrics(final BarcodeMatch match) {
        if (match.isMatch()) {
            barcodeStats.get(match.getIndexNumber()).get(match.getBarcode()).MATCHED++;
            mismatchesHist.get(match.getIndexNumber()).get(match.getBarcode())
                    .increment(match.getMismatches());
            nMean.get(match.getIndexNumber()).get(match.getBarcode()).push(match.getNumberOfNs());
        } else {
            metricHeader.DISCARDED_NO_MATCH++;
            return false;
        }
        if (match.getNumberOfNs() > maxN) {
            metricHeader.DISCARDED_BY_N++;
            return false;
        }
        if (match.getMismatches() > maxMismatches[match.getIndexNumber()]) {
            metricHeader.DISCARDED_BY_MISMATCH++;
            return false;
        }
        if (!match.isAssignable(minDifferenceWithSecond[match.getIndexNumber()])) {
            metricHeader.DISCARDED_BY_DISTANCE++;
            return false;
        }
        return true;
    }

    /**
     * Gets the accumulated statistics for barcodes match/mismatch.
     *
     * Note: calling this method recomputes the percentage every time.
     *
     * @see MatcherStat
     */
    @Override
    public MetricsFile<MatcherStat, Integer> getMatcherStatMetrics() {
        // create the matcher stats
        final MetricsFile<MatcherStat, Integer> matcherStats = new MetricsFile<>();
        // add the header and the metrics
        matcherStats.addHeader(metricHeader);

        // update the percentage value
        final double total = stats.values().stream().mapToInt(i -> i.RECORDS).sum();
        // for each value, set the percentage and add the metric
        stats.values().forEach(ms -> {
            ms.PCT_RECORDS = 100d * ms.RECORDS / total;
            matcherStats.addMetric(ms);
        });
        return matcherStats;
    }

    /**
     * Gets the accumulated statistics for each barcode.
     *
     * @see BarcodeStat
     */
    @Override
    public MetricsFile<BarcodeStat, Integer> getBarcodeStatMetrics() {
        // create the barcode stats
        final MetricsFile<BarcodeStat, Integer> barcode = new MetricsFile<>();
        for (int i = 0; i < dictionary.getMaxNumberOfIndexes(); i++) {
            final Map<String, BarcodeStat> current = barcodeStats.get(i);
            for (final Map.Entry<String, Histogram<Integer>> entry : mismatchesHist.get(i)
                    .entrySet()) {
                final BarcodeStat s = current.get(entry.getKey());
                s.MEAN_MISMATCH = entry.getValue().getMean();
                s.MEAN_N = nMean.get(i).get(entry.getKey()).mean();
                barcode.addHistogram(entry.getValue());
            }
            barcode.addAllMetrics(current.values());
        }
        return barcode;
    }
}
