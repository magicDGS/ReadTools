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

import java.io.PrintStream;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Statistics accumulator for the trimming algorithm. It is thread-safe
 *
 * @author Daniel Gómez-Sánchez
 */
public class TrimmingStats {

	// number of 5' Ns trim
	private int count5ptr;

	// number of 3' Ns trim
	private int count3ptr;

	// number of reads discarted because of internal Ns
	private int countRemainingNdiscards;

	// number of reads trimmed by quality
	private int countQualityTrims;

	// number of reads discarded by length
	private int countLengthDiscard;

	// number of reads passing
	private int readPassing;

	// the read length distribution
	private SortedMap<Integer, Integer> rld;

	public TrimmingStats() {
		count5ptr = 0;
		count3ptr = 0;
		countRemainingNdiscards = 0;
		countQualityTrims = 0;
		countLengthDiscard = 0;
		readPassing = 0;
		rld = new TreeMap<>();
	}

	/**
	 * Track a 5 prime trimmed read
	 */
	public synchronized void addCount5ptr() {
		count5ptr++;
	}

	/**
	 * Track a 3 prime trimmed read
	 */
	public synchronized void addCount3ptr() {
		count3ptr++;
	}

	/**
	 * Track a read discarded because of internal Ns
	 */
	public synchronized void addCountRemainingNdiscards() {
		countRemainingNdiscards++;
	}

	/**
	 * Track a quality trimmed read
	 */
	public synchronized void addCountsQualityTrims() {
		countQualityTrims++;
	}

	/**
	 * Track a read discarded by length
	 */
	public synchronized void addCountLengthDiscard() {
		countLengthDiscard++;
	}

	/**
	 * Track a read that pass and update the read length distribution
	 *
	 * @param length the read length of the read to update the distribution
	 */
	public synchronized void addReadPassing(int length) {
		readPassing++;
		updateReadLengthDistribution(length);
	}

	/**
	 * Update the read length distribution
	 *
	 * @param length value to update
	 */
	private void updateReadLengthDistribution(int length) {
		int counts = (rld.containsKey(length)) ? rld.get(length) + 1 : 1;
		rld.put(length, counts);
	}

	/**
	 * Report in a PrintStream (for example System.err or System.out) the statistics
	 *
	 * @param out the output
	 */
	public void report(PrintStream out) {
		out.print("Reads passed filtering: ");
		out.println(readPassing);
		out.print("5p poly-N sequences trimmed: ");
		out.println(count5ptr);
		out.print("3p poly-N sequences trimmed: ");
		out.println(count3ptr);
		out.print("Reads discarded during 'remaining N filtering': ");
		out.println(countRemainingNdiscards);
		out.print("Reads discarded during length filtering: ");
		out.println(countLengthDiscard);
		out.print("Count sequences trimed during quality filtering: ");
		out.println(countQualityTrims);
		out.println();
		out.print("Read length distribution");
		out.println();
		out.println("length\tcount");
		for (int key : rld.keySet()) {
			out.print(key);
			out.print("\t");
			out.println(rld.get(key));
		}
	}
}
