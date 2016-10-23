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
package org.magicdgs.readtools.utils.trimming;

import static org.magicdgs.readtools.utils.fastq.QualityUtils.getQuality;
import static org.magicdgs.readtools.utils.record.FastqRecordUtils.cutRecord;
import static org.magicdgs.readtools.utils.record.SequenceMatch.endN;
import static org.magicdgs.readtools.utils.record.SequenceMatch.startN;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;

import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * Different trimming algorithms for FastqRecords implemented as static methods
 *
 * @author Daniel Gómez-Sánchez
 */
public class TrimmingMethods {

    /**
     * Trimming of Ns at the beginning and end of the record
     *
     * @param record the record to trim
     *
     * @return the trimmed record; <code>null</code> if the record is completely trimmed
     */
    public static FastqRecord trimNs(FastqRecord record) {
        return trimNs(record, false);
    }

    /**
     * Trimming of Ns at the beginning (if no5ptrim requested) and end of the record
     *
     * @param record   the record to trim
     * @param no5ptrim no trim 5' of the sequence
     *
     * @return the trimmed record; <code>null</code> if the record is completely trimmed
     */
    public static FastqRecord trimNs(FastqRecord record, boolean no5ptrim) {
        if (record == null) {
            return null;
        }
        if (no5ptrim) {
            return trim3pNs(record);
        }
        String nucleotide = record.getReadString();
        int start = 0;
        int end = nucleotide.length();
        Matcher matchStart = startN.matcher(nucleotide);
        Matcher matchEnd = endN.matcher(nucleotide);
        if (matchStart.find()) {
            start = matchStart.end();
        }
        if (start != end && matchEnd.find()) {
            end = matchEnd.start();
        }
        return cutRecord(record, start, end);
    }

    /**
     * Trimming of Ns at the end of the record
     *
     * @param record the record to trim
     *
     * @return the trimmed record; <code>null</code> if the record is completely trimmed
     */
    public static FastqRecord trim3pNs(FastqRecord record) {
        if (record == null) {
            return null;
        }
        String nucleotide = record.getReadString();
        int end = nucleotide.length();
        Matcher matchEnd = endN.matcher(nucleotide);
        if (matchEnd.find()) {
            end = matchEnd.start();
        }
        return cutRecord(record, 0, end);
    }

    /**
     * Trimming of Ns at the beginning of the record
     *
     * @param record the record to trim
     *
     * @return the trimmed record; <code>null</code> if the record is completely trimmed
     */
    public static FastqRecord trim5pNs(FastqRecord record) {
        if (record == null) {
            return null;
        }
        String nucleotide = record.getReadString();
        int start = 0;
        Matcher matchStart = startN.matcher(nucleotide);
        if (matchStart.find()) {
            start = matchStart.end();
        }
        return cutRecord(record, start, nucleotide.length());
    }

    /**
     * Trim the quality using the Mott algorithm
     *
     * @param record        the record to trim
     * @param encoding      encoding for quality
     * @param qualThreshold quality threshold for the trimming
     * @param no5ptrim      if <code>true</code> do not include the 5' end
     *
     * @return the trimmed record, <code>null</code> if the read is completely trim
     */
    public static FastqRecord trimQualityMott(FastqRecord record, FastqQualityFormat encoding,
            int qualThreshold,
            boolean no5ptrim) {
        return (no5ptrim) ?
                trimQualityMott3p(record, encoding, qualThreshold) :
                trimQualityMott(record, encoding, qualThreshold);
    }

    /**
     * Trim the quality using the Mott algorithm
     *
     * @param record        the record to trim
     * @param encoding      encoding for quality
     * @param qualThreshold quality threshold for the trimming
     *
     * @return the trimmed record, <code>null</code> if the read is completely trim
     */
    public static FastqRecord trimQualityMott(FastqRecord record, FastqQualityFormat encoding,
            int qualThreshold) {
        if (record == null) {
            return null;
        }
        char[] quals = record.getBaseQualityString().toCharArray();
        TreeMap<Integer, StartEndTupple> hsps = new TreeMap<>();
        int highScore = 0, activeScore = 0, highScoreStart = -1, highScoreEnd = 0;
        for (int i = 0; i < quals.length; i++) {
            int toSub = getQuality(quals[i], encoding) - qualThreshold;
            activeScore += toSub;
            if (activeScore > 0) {
                if (activeScore > highScore) {
                    highScore = activeScore;
                    highScoreEnd = i;
                }
                if (highScoreStart == -1) {
                    highScoreStart = i;
                }
            } else {
                if (highScore > 0) {
                    hsps.put(highScore, new StartEndTupple(highScoreStart, highScoreEnd + 1));
                }
                highScoreStart = -1;
                activeScore = highScore = highScoreEnd = 0;
            }
        }
        if (highScore > 0) {
            hsps.put(highScore, new StartEndTupple(highScoreStart, highScoreEnd + 1));
        }
        if (hsps.isEmpty()) {
            return null;
        }
        StartEndTupple maxScoreStartEnd = hsps.get(hsps.lastKey());
        if (maxScoreStartEnd.getStart() == 0 && maxScoreStartEnd.getEnd() == quals.length) {
            return record;
        }
        return cutRecord(record, maxScoreStartEnd.getStart(), maxScoreStartEnd.getEnd());
    }

    /**
     * Trim the quality using the Mott algorithm without touching the 5' end
     *
     * @param record        the record to trim
     * @param encoding      encoding for quality
     * @param qualThreshold quality threshold for the trimming
     *
     * @return the trimmed record, <code>null</code> if the read is completely trim
     */
    public static FastqRecord trimQualityMott3p(FastqRecord record, FastqQualityFormat encoding,
            int qualThreshold) {
        if (record == null) {
            return null;
        }
        char[] quals = record.getBaseQualityString().toCharArray();
        int highScore = 0, activeScore = 0, highScoreEnd = -1;
        for (int i = 0; i < quals.length; i++) {
            int ts = getQuality(quals[i], encoding) - qualThreshold;
            activeScore += ts;
            if (activeScore > highScore) {
                highScore = activeScore;
                highScoreEnd = i;
            }
        }
        highScoreEnd++;
        if (highScore == 0) {
            return null;
        }
        if (highScoreEnd == quals.length) {
            return record;
        }
        return cutRecord(record, 0, highScoreEnd);
    }

    /**
     * Class to store a tupple with start and end for the trimming algorithms
     */
    private static class StartEndTupple {

        private final int start;

        private final int end;

        StartEndTupple(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }
}
