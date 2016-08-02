/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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
 */
package org.magicdgs.methods.barcodes.dictionary.decoder;

import static org.magicdgs.utils.record.SequenceMatch.mismatchesCount;
import static org.magicdgs.utils.record.SequenceMatch.missingCount;

import org.magicdgs.methods.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.methods.barcodes.dictionary.decoder.stats.BarcodeDetector;
import org.magicdgs.methods.barcodes.dictionary.decoder.stats.BarcodeStat;
import org.magicdgs.methods.barcodes.dictionary.decoder.stats.MatcherStat;
import org.magicdgs.utils.misc.Formats;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.util.Histogram;
import htsjdk.samtools.util.Log;
import htsjdk.tribble.util.MathUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Class for testing barcodes against a dictionary
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeDecoder {

    private static Log logger = Log.getInstance(BarcodeDecoder.class);

    /**
     * Default number of mismatches for BarcodeMethods
     */
    public static final int DEFAULT_MAXIMUM_MISMATCHES = 0;

    /**
     * Default minimum number of differences between the best barcode and the second
     */
    public static final int DEFAULT_MIN_DIFFERENCE_WITH_SECOND = 1;

    /**
     * Default N as mismatches
     */
    public static final boolean DEFAULT_N_AS_MISMATCHES = true;

    /**
     * Default number of Ns in the barcode to call as a no match
     */
    public static final int DEFAULT_MAXIMUM_N = Integer.MAX_VALUE;

    /**
     * The barcode dictionary
     */
    private final BarcodeDictionary dictionary;

    /**
     * Switch for the decoder considering N as mismatches
     */
    private final boolean nAsMismatches;

    /**
     * The number of maximum mismatches for the decoder
     */
    private final int[] maxMismatches;

    /**
     * The minimum number of differences between the best match and the second best for the decoder
     */
    private final int[] minDifferenceWithSecond;

    /**
     * Maximum number of Ns for match the barcode
     */
    private final int maxN;

    /**
     * The metric header for this detector
     */
    private final BarcodeDetector metricHeader;

    /**
     * Statistics for each combined barcode
     */
    private Hashtable<String, MatcherStat> stats;

    /**
     * Statistics for the barcode
     */
    private ArrayList<Hashtable<String, BarcodeStat>> barcodeStats;

    /**
     * Create the mismatches histogram
     */
    private ArrayList<Hashtable<String, Histogram<Integer>>> mismatchesHist;

    /**
     * Create the mismatches histogram
     */
    private ArrayList<Hashtable<String, MathUtils.RunningStat>> nMean;

    /**
     * Initialize the object with a dictionary and default parameters
     *
     * @param dictionary the dictionary with the barcodes
     */
    public BarcodeDecoder(BarcodeDictionary dictionary) {
        this(dictionary, DEFAULT_MAXIMUM_N, DEFAULT_N_AS_MISMATCHES, null, null);
    }

    /**
     * Initialize the object with a dictionary, maximum number of mismatches and default parameters
     *
     * @param dictionary    the dictionary with the barcodes
     * @param maxMismatches the maximum number of mismatches allowed (for each barcode)
     */
    public BarcodeDecoder(BarcodeDictionary dictionary, int... maxMismatches) {
        this(dictionary, DEFAULT_MAXIMUM_N, DEFAULT_N_AS_MISMATCHES, maxMismatches, null);
    }

    /**
     * Default constructor with all the parameters
     *
     * @param dictionary              the dictionary with the barcodes
     * @param nAsMismatches           if <code>true</code> the Ns count as mismatches
     * @param maxMismatches           the maximum number of mismatches allowed (for each barcode)
     * @param minDifferenceWithSecond the minimum difference in the number of mismatches between the
     *                                first and the
     *                                second best barcodes (for each barcode)
     *
     * @throws java.lang.IllegalArgumentException if the thresholds are arrays with different
     *                                            lengths than the number of
     *                                            barcodes in the dictionary
     */
    public BarcodeDecoder(BarcodeDictionary dictionary, int maxN, boolean nAsMismatches,
            int[] maxMismatches,
            int[] minDifferenceWithSecond) {
        this.dictionary = dictionary;
        this.maxN = maxN;
        this.nAsMismatches = nAsMismatches;
        this.maxMismatches = setIntParameter(DEFAULT_MAXIMUM_MISMATCHES, maxMismatches);
        this.minDifferenceWithSecond =
                setIntParameter(DEFAULT_MIN_DIFFERENCE_WITH_SECOND, minDifferenceWithSecond);
        this.metricHeader = new BarcodeDetector();
        initStats();
    }

    /**
     * Get the int array used for the decoder regarding the number of parameters
     *
     * @param parameter a single value if it is the same for all the barcodes, an array containing
     *                  the values for each
     *                  barcode or <code>null</code> if default value is requested
     *
     * @return the int array formatted to use in the decoder
     */
    private int[] setIntParameter(int defaultValue, int... parameter) {
        final int[] toReturn = new int[dictionary.getNumberOfBarcodes()];
        if (parameter == null) {
            Arrays.fill(toReturn, defaultValue);
        } else if (parameter.length == 1) {
            Arrays.fill(toReturn, parameter[0]);
        } else if (parameter.length == dictionary.getNumberOfBarcodes()) {
            return parameter;
        } else {
            throw new IllegalArgumentException(
                    "Thresholds and dictionary should have the same length or be equals 1");
        }
        return toReturn;
    }

    /**
     * Initialize the statistics
     */
    private void initStats() {
        // get the statistics for the decoding
        stats = new Hashtable<>();
        List<String> sampleNames = dictionary.getSampleNames();
        for (int i = 0; i < dictionary.numberOfSamples(); i++) {
            final String combined = dictionary.getCombinedBarcodesFor(i);
            stats.put(combined, new MatcherStat(combined, sampleNames.get(i)));
        }
        stats.put(BarcodeMatch.UNKNOWN_STRING,
                new MatcherStat(BarcodeMatch.UNKNOWN_STRING, BarcodeMatch.UNKNOWN_STRING));
        // get the statistics for the barcode
        barcodeStats = new ArrayList<>();
        mismatchesHist = new ArrayList<>();
        nMean = new ArrayList<>();
        String suffix;
        if (dictionary.getNumberOfBarcodes() == 1) {
            suffix = null;
        } else {
            suffix = "_";
        }
        for (int j = 0; j < dictionary.getNumberOfBarcodes(); j++) {
            Hashtable<String, BarcodeStat> bar = new Hashtable<>();
            Hashtable<String, Histogram<Integer>> histM = new Hashtable<>();
            Hashtable<String, MathUtils.RunningStat> mean = new Hashtable<>();
            for (String b : dictionary.getSetBarcodesFromIndex(j)) {
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

    /**
     * Get the barcode dictionary associated with this object
     *
     * @return the barcode dictionary
     */
    public BarcodeDictionary getDictionary() {
        return dictionary;
    }

    /**
     * Get the best barcode with the parameters for this decoder
     *
     * @param barcode the array of barcodes to match
     *
     * @return the best real barcode in the dictionary (pasted in order if there are more than one)
     *
     * @throws IllegalArgumentException if the length of the arrays does not match the number of
     *                                  barcodes in the
     *                                  dictionary
     */
    public String getBestBarcode(String... barcode) {
        if (barcode.length != dictionary.getNumberOfBarcodes()) {
            throw new IllegalArgumentException(
                    "Asking for matching a number of barcodes that does not fit with the ones contained in the barcode dictionary");
        }
        return getBestBarcode(nAsMismatches, maxMismatches, minDifferenceWithSecond, barcode);
    }

    /**
     * Get the bes barcode with the parameter for this decoder and return the associated sample
     *
     * @param barcode the array of barcodes to match
     *
     * @return the best associated sample; {@link org.magicdgs.methods.barcodes.dictionary.BarcodeDictionaryFactory#UNKNOWN_READGROUP_INFO}
     * if not found any
     */
    public SAMReadGroupRecord getBestReadGroupRecord(String... barcode) {
        return dictionary.getReadGroupFor(getBestBarcode(barcode));
    }

    /**
     * Get the best barcode using the BarcodeMatch approach
     *
     * @param nAsMismatches           if <code>true</code> the Ns count as mismatches
     * @param maxMismatches           the maximum number of mismatches allowed (for each barcode)
     * @param minDifferenceWithSecond the minimum difference in the number of mismatches between the
     *                                first and the
     *                                second best barcodes (for each barcode)
     * @param barcode                 the array of barcodes to match
     *
     * @return the best real barcode in the dictionary (pasted in order if there are more than one)
     */
    private String getBestBarcode(boolean nAsMismatches, int[] maxMismatches,
            int[] minDifferenceWithSecond,
            String... barcode) {
        List<BarcodeMatch> bestMatchs = getBestBarcodeMatch(nAsMismatches, barcode);
        // if only one barcode
        if (bestMatchs.size() == 1) {
            BarcodeMatch match = bestMatchs.get(0);
            if (updateMatchStatsAndPassFilters(0, match, maxMismatches[0],
                    minDifferenceWithSecond[0])) {
                stats.get(match.barcode).RECORDS++;
                return match.barcode;
            } else {
                return addUnknownToMetricsAndReturnIt();
            }
        }
        // map sample indexes and number of times that it occurs
        HashMap<Integer, Integer> samples = new HashMap<>();
        // for each barcode, check the quality
        for (int i = 0; i < bestMatchs.size(); i++) {
            BarcodeMatch current = bestMatchs.get(i);
            if (updateMatchStatsAndPassFilters(i, current, maxMismatches[i],
                    minDifferenceWithSecond[i])) {
                ArrayList<String> allBarcodes = dictionary.getBarcodesFromIndex(i);
                int sampleIndex = allBarcodes.indexOf(current.barcode);
                // check if it is unique for this set
                if (dictionary.isBarcodeUniqueInAt(current.barcode, i)) {
                    final String toReturn = dictionary.getCombinedBarcodesFor(sampleIndex);
                    stats.get(toReturn).RECORDS++;
                    // return directly the barcode
                    return toReturn;
                } else {
                    for (; sampleIndex < allBarcodes.size(); sampleIndex++) {
                        if (allBarcodes.get(sampleIndex).equals(current.barcode)) {
                            Integer count = samples.get(sampleIndex);
                            samples.put(sampleIndex, (count == null) ? 1 : count + 1);
                        }
                    }
                }
            }
        }
        if (samples.size() == 0) {
            return addUnknownToMetricsAndReturnIt();
        }
        // if we reach this point, there are non unique barcode that identifies the sample
        // obtain the maximum count
        int maxCount = Collections.max(samples.values());
        // if there are more than one sample that could be associated with the barcode
        if (Collections.frequency(samples.values(), maxCount) != 1) {
            // it is not determined
            return addUnknownToMetricsAndReturnIt();
        } else {
            for (Integer sampleIndex : samples.keySet()) {
                if (samples.get(sampleIndex) == maxCount) {
                    stats.get(dictionary.getCombinedBarcodesFor(sampleIndex)).RECORDS++;
                    return dictionary.getCombinedBarcodesFor(sampleIndex);
                }
            }
        }
        // in principle, this cannot be reached
        throw new IllegalStateException("Unreachable code");
    }

    /**
     * Add a record count to the unknown barcode and return the unknown string
     *
     * @return {@link org.magicdgs.methods.barcodes.dictionary.decoder.BarcodeMatch#UNKNOWN_STRING}
     */
    private String addUnknownToMetricsAndReturnIt() {
        stats.get(BarcodeMatch.UNKNOWN_STRING).RECORDS++;
        return BarcodeMatch.UNKNOWN_STRING;
    }

    /**
     * Check if the match is a match and pass the provided filters: maximum mismatches and minimum
     * differences with the
     * second barcode
     *
     * @param match                   the match to check
     * @param maxMismatches           the maximum number of mismatches
     * @param minDifferenceWithSecond the minimum differences with the second barcode
     *
     * @return <code>true</code> if pass all the filters; <code>false</code> otherwise
     */
    private boolean updateMatchStatsAndPassFilters(int index, BarcodeMatch match, int maxMismatches,
            int minDifferenceWithSecond) {
        BarcodeStat toUpdate;
        // first check if it passing
        if (match.isMatch()) {
            toUpdate = barcodeStats.get(index).get(match.barcode);
            mismatchesHist.get(index).get(match.barcode).increment(match.mismatches);
            nMean.get(index).get(match.barcode).push(match.numberOfNs);
            toUpdate.MATCHED++;
        } else {
            metricHeader.DISCARDED_NO_MATCH++;
            return false;
        }
        if (!(match.numberOfNs <= maxN)) {
            metricHeader.DISCARDED_BY_N++;
            return false;
        }
        if (!(match.mismatches <= maxMismatches)) {
            metricHeader.DISCARDED_BY_MISMATCH++;
            return false;
        }
        if (!(match.getDifferenceWithSecond() >= minDifferenceWithSecond)) {
            metricHeader.DISCARDED_BY_DISTANCE++;
            return false;
        }
        return true;
    }

    /**
     * Get the best barcode match
     *
     * @param barcode       the barcode to test
     * @param nAsMismatches if <code>true</code> the Ns count as mismatches
     * @param index         the index of the barcode
     *
     * @return the best barcode matched
     */
    private BarcodeMatch getBestBarcodeMatch(int index, boolean nAsMismatches, String barcode) {
        BarcodeMatch best = new BarcodeMatch(barcode.length());
        for (String b : dictionary.getSetBarcodesFromIndex(index)) {
            String subBarcode;
            if (barcode.length() > b.length()) {
                // cut the barcode if it is longer
                subBarcode = barcode.substring(0, b.length());
            } else {
                subBarcode = barcode;
            }
            int currentMismatch = mismatchesCount(subBarcode, b, nAsMismatches);
            if (currentMismatch < best.mismatches) {
                // if the count of mismatches is better than the previous
                best.mismatchesToSecondBest = best.mismatches;
                best.mismatches = currentMismatch;
                best.barcode = b;
                best.numberOfNs = missingCount(b);
            } else if (currentMismatch < best.mismatchesToSecondBest) {
                // if it is the second best, track the result
                best.mismatchesToSecondBest = currentMismatch;
            }
        }
        return best;
    }

    /**
     * Get the best barcode match for several barcodes
     *
     * @param barcode the array of barcodes to match
     *
     * @return the list of matched barcodes
     *
     * @throws IllegalArgumentException if the length of the arrays does not match the number of
     *                                  barcodes in the
     *                                  dictionary
     */
    private List<BarcodeMatch> getBestBarcodeMatch(boolean nAsMismatches, String... barcode) {
        if (barcode.length != dictionary.getNumberOfBarcodes()) {
            throw new IllegalArgumentException(
                    "Asking for matching a number of barcodes that does not fit with the ones contained in the barcode dictionary");
        }
        ArrayList<BarcodeMatch> toReturn = new ArrayList<>();
        // only one barcode
        if (barcode.length == 1) {
            toReturn.add(getBestBarcodeMatch(0, nAsMismatches, barcode[0]));
        } else {
            // more than one barcode
            // we need check in order
            for (int i = 0; i < dictionary.getNumberOfBarcodes(); i++) {
                toReturn.add(getBestBarcodeMatch(i, nAsMismatches, barcode[i]));
            }
        }
        return toReturn;
    }

    /**
     * Log the results for the matcher dictionary
     *
     * @param log the log to use for logging
     */
    public void logMatcherResult(Log log) {
        for (MatcherStat s : stats.values()) {
            log.info("Found ", Formats.commaFmt.format(s.RECORDS), " records for ", s.SAMPLE, " (",
                    s.BARCODE, ")");
        }
    }

    /**
     * Output the statistics for the processed barcodes
     *
     * @param statsFile the file to output the statistics
     */
    public void outputStats(File statsFile) throws IOException {
        Writer statsWriter = new FileWriter(statsFile);
        // create the matcher stats
        MetricsFile<MatcherStat, Integer> matcherStats = new MetricsFile<>();
        // add the header and the metrics
        matcherStats.addHeader(metricHeader);
        matcherStats.addAllMetrics(stats.values());
        // write matcher stats
        matcherStats.write(statsWriter);
        // create the barcode stats
        MetricsFile<BarcodeStat, Integer> barcode = new MetricsFile<>();
        for (int i = 0; i < dictionary.getNumberOfBarcodes(); i++) {
            Hashtable<String, BarcodeStat> current = barcodeStats.get(i);
            for (Map.Entry<String, Histogram<Integer>> entry : mismatchesHist.get(i).entrySet()) {
                BarcodeStat s = current.get(entry.getKey());
                s.MEAN_MISMATCH = entry.getValue().getMean();
                s.MEAN_N = nMean.get(i).get(entry.getKey()).mean();
                barcode.addHistogram(entry.getValue());
            }
            barcode.addAllMetrics(current.values());
        }
        // write barcode stats
        barcode.write(statsWriter);
        // close the metrics file
        statsWriter.close();
    }
}
