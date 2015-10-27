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
package org.vetmeduni.utils;

import htsjdk.samtools.util.Log;

import static org.vetmeduni.utils.Formats.commaFmt;
import static org.vetmeduni.utils.Formats.timeFmt;

/**
 * Logger for Fastq files, similar to {@link htsjdk.samtools.util.ProgressLogger} but without the need of input a read
 *
 * @author Daniel Gómez Sánchez
 */
public class FastqLogger {

	private final Log log;

	private final int n;

	private final String verb;

	private final String noun;

	private final long startTime = System.currentTimeMillis();

	private long processed = 0;

	// Set to -1 until the first record is added
	private long lastStartTime = -1;

	/**
	 * Construct a progress logger.
	 *
	 * @param log  the Log object to write outputs to
	 * @param n    the frequency with which to output (i.e. every N records)
	 * @param verb the verb to log, e.g. "Processed, Read, Written".
	 * @param noun the noun to use when logging, e.g. "Records, Variants, Loci"
	 */
	public FastqLogger(final Log log, final int n, final String verb, final String noun) {
		this.log = log;
		this.n = n;
		this.verb = verb;
		this.noun = noun;
	}

	/**
	 * Construct a progress logger.
	 *
	 * @param log  the Log object to write outputs to
	 * @param n    the frequency with which to output (i.e. every N records)
	 * @param verb the verb to log, e.g. "Processed, Read, Written".
	 */
	public FastqLogger(final Log log, final int n, final String verb) {
		this(log, n, verb, "reads");
	}

	/**
	 * Construct a progress logger with the desired log and frequency and the verb "Processed".
	 *
	 * @param log the Log object to write outputs to
	 * @param n   the frequency with which to output (i.e. every N records)
	 */
	public FastqLogger(final Log log, final int n) {
		this(log, n, "Processed");
	}

	/**
	 * Construct a progress logger with the desired log, the verb "Processed" and a period of 1m records.
	 *
	 * @param log the Log object to write outputs to
	 */
	public FastqLogger(final Log log) {
		this(log, 1000000);
	}

	public synchronized boolean add() {
		if (this.lastStartTime == -1) {
			this.lastStartTime = System.currentTimeMillis();
		}
		if (++this.processed % this.n == 0) {
			final long now = System.currentTimeMillis();
			final long lastPeriodSeconds = (now - this.lastStartTime) / 1000;
			this.lastStartTime = now;
			final long seconds = (System.currentTimeMillis() - startTime) / 1000;
			final String elapsed = formatElapseTime(seconds);
			final String period = pad(commaFmt.format(lastPeriodSeconds), 4);
			final String processed = pad(commaFmt.format(this.processed), 13);
			log.info(this.verb, " ", processed, " " + noun + ".  Elapsed time: ", elapsed, "s.  Time for last ",
				commaFmt.format(this.n), ": ", period, "s.");
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the count of records processed.
	 */
	public long getCount() {
		return this.processed;
	}

	/**
	 * Returns the number of seconds since progress tracking began.
	 */
	public long getElapsedSeconds() {
		return (System.currentTimeMillis() - this.startTime) / 1000;
	}

	/**
	 * Left pads a string until it is at least the given length.
	 */
	private String pad(String in, final int length) {
		while (in.length() < length) {
			in = " " + in;
		}
		return in;
	}

	/**
	 * Formats a number of seconds into hours:minutes:seconds.
	 */
	private String formatElapseTime(final long seconds) {
		final long s = seconds % 60;
		final long allMinutes = seconds / 60;
		final long m = allMinutes % 60;
		final long h = allMinutes / 60;
		return timeFmt.format(h) + ":" + timeFmt.format(m) + ":" + timeFmt.format(s);
	}

	public synchronized String numberOfVariantsProcessed() {
		final long seconds = (System.currentTimeMillis() - startTime) / 1000;
		final String elapsed = formatElapseTime(seconds);
		return String
			.format("%s %s %s. Elapsed time: %s", this.verb, commaFmt.format(this.processed), this.noun, elapsed);
	}
}

