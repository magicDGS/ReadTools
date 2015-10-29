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

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.Log;
import org.vetmeduni.io.readers.FastqPairReaderImpl;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.io.writers.PairFastqWriters;
import org.vetmeduni.utils.IOUtils;
import org.vetmeduni.utils.fastq.FastqLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.vetmeduni.utils.record.FastqRecordUtils.cutRecord;
import static org.vetmeduni.utils.fastq.QualityUtils.getQuality;

/**
 * Trimming algorithm implemented in Kofler et al 2011
 *
 * TODO: unit tests
 *
 * @author Daniel Gómez-Sánchez
 */
public class MottAlgorithm {

	// for the multi-thread
	private static final int BUFFER_SIZE_FACTOR = 2;

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
		if (record == null || newRecord.length() < minLength) {
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
	 * Process single-end data in a single thread
	 *
	 * @param input         the input file
	 * @param output_prefix the output prefix
	 * @param format        the fastq format
	 * @param logger        the logger for the progress
	 * @param gzip          it is the output gzipped?
	 */
	private void processSEsingleThread(File input, String output_prefix, FastqQualityFormat format, boolean verbose,
		Log logger, boolean gzip) {
		// Obtain output name
		String output = IOUtils.makeInputFastqWithDefaults(output_prefix, gzip);
		FastqReader reader = new FastqReader(input);
		FastqWriter writer = new FastqWriterFactory().newWriter(new File(output));
		FastqLogger progress = null;
		TrimmingStats stats = null;
		progress = new FastqLogger(logger);
		if (verbose) {
			stats = new TrimmingStats();
		}
		while (reader.hasNext()) {
			FastqRecord record = reader.next();
			FastqRecord newRecord = trimFastqRecord(record, format, stats);
			if (newRecord != null) {
				writer.write(newRecord);
			}
			progress.add();
			writer.write(newRecord);
		}
		logger.info(progress.numberOfVariantsProcessed());
		if (verbose) {
			System.out.print("Read processed: ");
			System.out.println(progress.getCount());
			System.out.println("\n");
			System.out.println("READ STATISTICS");
			stats.report(System.out);
		}
		reader.close();
		writer.close();
	}

	/**
	 * Process pair-end in a single thread
	 *
	 * @param input1        the input for the first pair
	 * @param input2        the input for the second pair
	 * @param output_prefix the output prefix
	 * @param format        the fastq format
	 * @param logger        the logger for the progress
	 * @param gzip          it is the output gzipped?
	 */
	private void processPEsingleThread(File input1, File input2, String output_prefix, FastqQualityFormat format,
		boolean verbose, Log logger, boolean gzip) throws IOException {
		// Open readers
		FastqPairReaderImpl reader = new FastqPairReaderImpl(input1, input2);
		// creating progress
		FastqLogger progress = new FastqLogger(logger, 1000000, "Processed", "read-pairs");
		TrimmingStats stats1 = null;
		TrimmingStats stats2 = null;
		int paired = 0;
		int single = 0;
		if (verbose) {
			stats1 = new TrimmingStats();
			stats2 = new TrimmingStats();
		}
		// Open writer
		PairFastqWriters writer = new PairFastqWriters(output_prefix, gzip);
		while (reader.hasNext()) {
			FastqPairedRecord record = reader.next();
			FastqRecord newRecord1 = trimFastqRecord(record.getRecord1(), format, stats1);
			FastqRecord newRecord2 = trimFastqRecord(record.getRecord2(), format, stats2);
			if (newRecord1 != null && newRecord2 != null) {
				writer.writePairs(newRecord1, newRecord2);
				paired++;
			} else if (newRecord1 != null) {
				writer.writeSingle(newRecord1);
				single++;
			} else if (newRecord2 != null) {
				writer.writeSingle(newRecord2);
				single++;
			}
			progress.add();
		}
		logger.info(progress.numberOfVariantsProcessed());
		if (verbose) {
			System.out.print("Read-pairs processed: ");
			System.out.println(progress.getCount());
			System.out.print("Read-pairs trimmed in pairs: ");
			System.out.println(paired);
			System.out.print("Read-pairs trimmed as singles: ");
			System.out.println(single);
			System.out.println("\n");
			System.out.println("FIRST READ STATISTICS");
			stats1.report(System.out);
			System.out.println("\n");
			System.out.println("SECOND READ STATISTICS");
			stats2.report(System.out);
		}
		reader.close();
		writer.close();
	}

	/**
	 * Trimming of Ns in the beggining and end of the record and returns a new FastqRecord
	 *
	 * @param record the record to trim
	 * @param stats  accumulator for trimming statistics; if null, it is ignored
	 *
	 * @return the trimmed record
	 */
	public FastqRecord trimNs(FastqRecord record, TrimmingStats stats) {
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

	/**
	 * Callable class for multi-thread SE processing
	 */
	private class TrimFastqCallableSE implements Callable<FastqRecord> {

		private final FastqLogger logger;

		private final FastqRecord record;

		private final FastqQualityFormat format;

		private final TrimmingStats stats;

		public TrimFastqCallableSE(FastqLogger progressLogger, FastqRecord record, FastqQualityFormat format,
			TrimmingStats stats) {
			this.logger = progressLogger;
			this.record = record;
			this.format = format;
			this.stats = stats;
		}

		@Override
		public FastqRecord call() throws Exception {
			FastqRecord newRecord = trimFastqRecord(record, format, stats);
			logger.add();
			return newRecord;
		}
	}

	/**
	 * Callable class for multi-thread PE processing
	 */
	private class TrimFastqCallablePE implements Callable<FastqPairedRecord> {

		private final FastqLogger logger;

		private final FastqPairedRecord record;

		private final FastqQualityFormat format;

		private final TrimmingStats stats1, stats2;

		public TrimFastqCallablePE(FastqLogger progressLogger, FastqPairedRecord record, FastqQualityFormat format,
			TrimmingStats stats1, TrimmingStats stats2) {
			this.logger = progressLogger;
			this.record = record;
			this.format = format;
			this.stats1 = stats1;
			this.stats2 = stats2;
		}

		@Override
		public FastqPairedRecord call() throws Exception {
			FastqRecord newRecord1 = trimFastqRecord(record.getRecord1(), format, stats1);
			FastqRecord newRecord2 = trimFastqRecord(record.getRecord2(), format, stats2);
			logger.add();
			return new FastqPairedRecord(newRecord1, newRecord2);
		}
	}

	/**
	 * Process pair-end files with the specifications for this object.
	 *
	 * @param input1        the input for the first pair
	 * @param input2        the input for the second pair
	 * @param output_prefix the output prefix
	 * @param format        the fastq format
	 * @param threads       number of threads to use
	 * @param verbose       should we output the statistics to STDOUT?
	 * @param logger        the logger for the progress
	 * @param gzip          it is the output gzipped?
	 *
	 * @throws IOException                if there is some error with the files
	 * @throws java.lang.RuntimeException if the number of threads is lower than 1
	 */
	public void processPE(File input1, File input2, String output_prefix, FastqQualityFormat format, int threads,
		boolean verbose, Log logger, boolean gzip) throws IOException {
		// check if threads make sense
		if (threads < 0) {
			throw new RuntimeException("Number of threads should be bigger than 1");
		} else if (threads == 1) {
			logger.debug("Single thread mode");
			processPEsingleThread(input1, input2, output_prefix, format, verbose, logger, gzip);
		} else {
			logger.debug("Multi-thread mode: " + threads);
			processPEmulti(input1, input2, output_prefix, format, threads, verbose, logger, gzip);
		}
	}

	/**
	 * Process single-end files with the specification for this object
	 *
	 * @param input         the input file
	 * @param output_prefix the output prefix
	 * @param format        the fastq format
	 * @param threads       number of threads to use
	 * @param verbose       should we output the statistics to STDOUT?
	 * @param logger        the logger for the progress
	 * @param gzip          it is the output gzipped?
	 *
	 * @throws IOException                if there is some error with the files
	 * @throws java.lang.RuntimeException if the number of threads is lower than 1
	 */
	public void processSE(File input, String output_prefix, FastqQualityFormat format, int threads, boolean verbose,
		Log logger, boolean gzip) throws IOException {
		// check if threads make sense
		if (threads < 0) {
			throw new RuntimeException("Number of threads should be bigger than 1");
		} else if (threads == 1) {
			processSEsingleThread(input, output_prefix, format, verbose, logger, gzip);
		} else {
			processSEmulti(input, output_prefix, format, threads, verbose, logger, gzip);
		}
	}

	/**
	 * Process pair-end with multiple threads
	 *
	 * @param input1        the input for the first pair
	 * @param input2        the input for the second pair
	 * @param output_prefix the output prefix
	 * @param format        the fastq format
	 * @param nThreads      number of threads
	 * @param verbose       should we output the statistics to STDOUT?
	 * @param logger        the logger for the progress
	 * @param gzip          it is the output gzipped?
	 *
	 * @throws IOException if there is some error with the files
	 */
	private void processPEmulti(File input1, File input2, String output_prefix, FastqQualityFormat format, int nThreads,
		boolean verbose, Log logger, boolean gzip) throws IOException {
		// Open readers
		FastqPairReaderImpl reader = new FastqPairReaderImpl(input1, input2);
		// creating progress
		FastqLogger progress = new FastqLogger(logger, 1000000, "Processed", "read-pairs");
		TrimmingStats stats1 = null;
		TrimmingStats stats2 = null;
		int paired = 0;
		int single = 0;
		if (verbose) {
			stats1 = new TrimmingStats();
			stats2 = new TrimmingStats();
		}
		// Open writer
		FastqWriterFactory factory = new FastqWriterFactory();
		factory.setUseAsyncIo(true);
		PairFastqWriters writer = new PairFastqWriters(output_prefix, gzip, factory);
		// PairFastqWriters writer = new PairFastqWriters(output_prefix, gzip);
		// open the executor
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads);
		// the buffer size for the executor is set to 5 the number of threads
		final int bufferSize = nThreads * BUFFER_SIZE_FACTOR;
		Collection<Callable<FastqPairedRecord>> jobs = new ArrayList<>();
		// iterate
		while (reader.hasNext()) {
			TrimFastqCallablePE record = new TrimFastqCallablePE(progress, reader.next(), format, stats1, stats2);
			jobs.add(record);
			if (jobs.size() >= bufferSize) {
				// run all and empty the list
				try {
					List<Future<FastqPairedRecord>> result = executor.invokeAll(jobs);
					for (Future<FastqPairedRecord> future : result) {
						FastqPairedRecord newRecord = future.get();
						FastqRecord newRecord1 = newRecord.getRecord1();
						FastqRecord newRecord2 = newRecord.getRecord2();
						if (newRecord1 != null && newRecord2 != null) {
							writer.writePairs(newRecord1, newRecord2);
							paired++;
						} else if (newRecord1 != null) {
							writer.writeSingle(newRecord1);
							single++;
						} else if (newRecord2 != null) {
							writer.writeSingle(newRecord2);
							single++;
						}
					}
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e.getMessage());
				}
				jobs.clear();
			}
		}
		logger.debug("Jobs: " + jobs.size(), ". Terminated: ", executor.isTerminated());
		// run the remaining jobs if they are not added
		if (jobs.size() != 0) {
			try {
				List<Future<FastqPairedRecord>> result = executor.invokeAll(jobs);
				for (Future<FastqPairedRecord> future : result) {
					FastqPairedRecord newRecord = future.get();
					FastqRecord newRecord1 = newRecord.getRecord1();
					FastqRecord newRecord2 = newRecord.getRecord2();
					if (newRecord1 != null && newRecord2 != null) {
						writer.writePairs(newRecord1, newRecord2);
						paired++;
					} else if (newRecord1 != null) {
						writer.writeSingle(newRecord1);
						single++;
					} else if (newRecord2 != null) {
						writer.writeSingle(newRecord2);
						single++;
					}
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		logger.info(progress.numberOfVariantsProcessed());
		if (verbose) {
			System.out.print("Read-pairs processed: ");
			System.out.println(progress.getCount());
			System.out.print("Read-pairs trimmed in pairs: ");
			System.out.println(paired);
			System.out.print("Read-pairs trimmed as singles: ");
			System.out.println(single);
			System.out.println("\n");
			System.out.println("FIRST READ STATISTICS");
			stats1.report(System.out);
			System.out.println("\n");
			System.out.println("SECOND READ STATISTICS");
			stats2.report(System.out);
		}
		reader.close();
		writer.close();
	}

	/**
	 * Process single-end data with the specifications in this object
	 *
	 * @param input         the input file
	 * @param output_prefix the output prefix
	 * @param format        the fastq format
	 * @param nThreads      number of threads
	 * @param verbose       should we output the statistics to STDOUT?
	 * @param logger        the logger for the progress
	 * @param gzip          it is the output gzipped?
	 */
	private void processSEmulti(File input, String output_prefix, FastqQualityFormat format, int nThreads,
		boolean verbose, Log logger, boolean gzip) {
		// Obtain output name
		String output = IOUtils.makeInputFastqWithDefaults(output_prefix, gzip);
		FastqReader reader = new FastqReader(input);
		// Open writer
		FastqWriterFactory factory = new FastqWriterFactory();
		factory.setUseAsyncIo(true);
		FastqWriter writer = factory.newWriter(new File(output));
		// open the executor
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads);
		// the buffer size for the executor is set to twice the number of threads
		final int bufferSize = nThreads * BUFFER_SIZE_FACTOR;
		Collection<Callable<FastqRecord>> jobs = new ArrayList<>();
		FastqLogger progress = null;
		TrimmingStats stats = null;
		progress = new FastqLogger(logger);
		if (verbose) {
			stats = new TrimmingStats();
		}
		while (reader.hasNext()) {
			TrimFastqCallableSE record = new TrimFastqCallableSE(progress, reader.next(), format, stats);
			jobs.add(record);
			if (jobs.size() >= bufferSize) {
				// run all and empty the list
				try {
					List<Future<FastqRecord>> result = executor.invokeAll(jobs);
					for (Future<FastqRecord> future : result) {
						FastqRecord newRecord = future.get();
						if (newRecord != null) {
							writer.write(newRecord);
						}
						writer.write(newRecord);
					}
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e.getMessage());
				}
				jobs.clear();
			}
		}
		if (jobs.size() != 0) {
			// run all and empty the list
			try {
				List<Future<FastqRecord>> result = executor.invokeAll(jobs);
				for (Future<FastqRecord> future : result) {
					FastqRecord newRecord = future.get();
					if (newRecord != null) {
						writer.write(newRecord);
					}
					writer.write(newRecord);
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		logger.info(progress.numberOfVariantsProcessed());
		if (verbose) {
			System.out.print("Read processed: ");
			System.out.println(progress.getCount());
			System.out.println("\n");
			System.out.println("READ STATISTICS");
			stats.report(System.out);
		}
		reader.close();
		writer.close();
	}
}
