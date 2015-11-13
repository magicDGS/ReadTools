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
package org.vetmeduni.methods.trimming;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.Log;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.vetmeduni.utils.fastq.QualityUtils.getQuality;
import static org.vetmeduni.utils.record.FastqRecordUtils.cutRecord;

/**
 * Trimming algorithm implemented in Kofler et al 2011
 *
 * TODO: unit tests
 *
 * @author Daniel Gómez-Sánchez
 */
public class MottAlgorithm {

	// for pattern matching
	private static final Pattern startN = Pattern.compile("^N+", Pattern.CASE_INSENSITIVE);

	private static final Pattern endN = Pattern.compile("N+$", Pattern.CASE_INSENSITIVE);

	private static final Pattern Ns = Pattern.compile("N", Pattern.CASE_INSENSITIVE);

	private static final Log logger = Log.getInstance(MottAlgorithm.class);

	private final boolean trimQuality;

	private final int qualThreshold;

	private final int minLength;

	private final boolean discardRemainingNs;

	private final boolean no5ptrim;

	/**
	 * Constructor for a new Mott algorithm
	 *
	 * @param trimQuality        should the quality be trimmed?
	 * @param qualThreshold      quality threshold for the trimming
	 * @param minLength          minimum length for the trimming
	 * @param discardRemainingNs should we discard reads with Ns in the middle?
	 * @param no5ptrim           no trim the 5 prime end
	 */
	public MottAlgorithm(boolean trimQuality, int qualThreshold, int minLength, boolean discardRemainingNs,
		boolean no5ptrim) {
		this.trimQuality = trimQuality;
		this.qualThreshold = qualThreshold;
		this.minLength = minLength;
		this.discardRemainingNs = discardRemainingNs;
		this.no5ptrim = no5ptrim;
	}

	/**
	 * Trim the quality using the mott algorithm. WARNING: It does not check if trim quality is set or not
	 *
	 * @param record   the record to trim
	 * @param encoding encoding for quality
	 * @param stats    accumulator for trimming statistics; if null, it is ignored
	 *
	 * @return the trimmed record, <code>null</code> if the read is completely trim
	 */
	public FastqRecord trimQualityMott(FastqRecord record, FastqQualityFormat encoding, TrimmingStats stats) {
		if(record == null) {
			return null;
		}
		char[] quals = record.getBaseQualityString().toCharArray();
		TreeMap<Integer, StartEndTupple> hsps = new TreeMap<Integer, StartEndTupple>();
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
			if (stats != null) {
				stats.addCountsQualityTrims();
			}
			return null;
		}
		StartEndTupple maxScoreStartEnd = hsps.get(hsps.lastKey());
		if (maxScoreStartEnd.getStart() == 0 && maxScoreStartEnd.getEnd() == quals.length) {
			return record;
		}
		if (stats != null) {
			stats.addCountsQualityTrims();
		}
		return cutRecord(record, maxScoreStartEnd.getStart(), maxScoreStartEnd.getEnd());
	}

	/**
	 * Trim the read without including the 5'; returns a new FastqRecord or null if the read is completely trim
	 *
	 * @param record   Record to trim
	 * @param encoding encoding for quality
	 * @param stats    accumulator for trimming statistics; if null, it is ignored
	 *
	 * @return the trimmed record, <code>null</code> if the read is completely trim
	 */
	public FastqRecord trimNo5pTrim(FastqRecord record, FastqQualityFormat encoding, TrimmingStats stats) {
		if(record == null) {
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
			stats.addCountsQualityTrims();
			return null;
		}
		if (highScoreEnd == quals.length) {
			return record;
		}
		stats.addCountsQualityTrims();
		return cutRecord(record, 0, highScoreEnd);
	}

	/**
	 * Trim the record with the provided settings in the MottAlgorithm object
	 *
	 * @param record the record to trim
	 * @param stats  accumulator for trimming statistics; if null, it is ignored
	 *
	 * @return the trimmed record or null if does not pass filters
	 */
	public FastqRecord trimFastqRecord(FastqRecord record, FastqQualityFormat format, TrimmingStats stats) {
		FastqRecord newRecord = trimNs(record, stats);
		// discard remaining Ns
		if (discardRemainingNs) {
			if (containsNs(newRecord, stats)) {
				return null;
			}
		}
		if (trimQuality) {
			if (no5ptrim) {
				newRecord = trimNo5pTrim(record, format, stats);
			} else {
				newRecord = trimQualityMott(newRecord, format, stats);
			}
		}
		if (newRecord == null || newRecord.length() < minLength) {
			stats.addCountLengthDiscard();
			return null;
		}
		stats.addReadPassing(newRecord.length());
		return newRecord;
	}

	/**
	 * Check if the record contain Ns (if null, it is counted as it contains Ns)
	 *
	 * @param record the record to test
	 * @param stats  accumulator for trimming statistics; if null, it is ignored
	 *
	 * @return <code>true</code> if the record is null or contain Ns; <code>false</code> otherwise
	 */
	public boolean containsNs(FastqRecord record, TrimmingStats stats) {
		if (record == null || Ns.matcher(record.getReadString()).find()) {
			if (stats != null) {
				stats.addCountRemainingNdiscards();
			}
			return true;
		}
		return false;
	}

	/**
	 * Trimming of Ns in the begining and end of the record and returns a new FastqRecord
	 *
	 * @param record the record to trim
	 * @param stats  accumulator for trimming statistics; if null, it is ignored
	 *
	 * @return the trimmed record; <code>null</code> if the record is completely trimmed
	 */
	public FastqRecord trimNs(FastqRecord record, TrimmingStats stats) {
		if(record == null) {
			return null;
		}
		String nucleotide = record.getReadString();
		int start = 0;
		int end = nucleotide.length();
		Matcher matchStart = startN.matcher(nucleotide);
		Matcher matchEnd = endN.matcher(nucleotide);
		if (!no5ptrim && matchStart.find()) {
			start = matchStart.end();
			if (stats != null) {
				stats.addCount5ptr();
			}
		}
		if (start != end && matchEnd.find()) {
			end = matchEnd.start();
			if (stats != null) {
				stats.addCount3ptr();
			}
		}
		return cutRecord(record, start, end);
	}

	/**
	 * Class to store a tupple with start and end for the MottAlgorithm method
	 */
	private static class StartEndTupple {

		private int start;

		private int end;

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
