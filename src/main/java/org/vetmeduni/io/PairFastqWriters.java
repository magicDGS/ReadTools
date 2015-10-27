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
package org.vetmeduni.io;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.Lazy;
import htsjdk.samtools.util.SequenceUtil;
import htsjdk.samtools.util.StringUtil;
import org.vetmeduni.utils.Formats;

import java.io.File;

/**
 * Simple class to have two writers in pairs
 *
 * @author Daniel Gómez-Sánchez
 */
public class PairFastqWriters {

	private final static FastqWriterFactory DEFAULT_FACTORY = new FastqWriterFactory();

	// Pairs are always initializer; single is not
	private final FastqWriter first, second;

	private final Lazy<FastqWriter> single;

	private long countPairs, countSingle;

	/**
	 * Default constructor
	 *
	 * @param first  writer for the first pair
	 * @param second writer for the second pair
	 * @param single lazy writer for the single pair
	 */
	public PairFastqWriters(FastqWriter first, FastqWriter second, Lazy<FastqWriter> single) {
		this.first = first;
		this.second = second;
		this.single = single;
		this.countPairs = 0;
		this.countSingle = 0;
	}

	/**
	 * Easy constructor: prefix_1.fq prefix_2.fq and prefix_SE.fq formated output files
	 *
	 * @param prefix  the prefix for the files
	 * @param gzip    if gzip
	 * @param factory the factory to create the fastq files
	 */
	public PairFastqWriters(String prefix, boolean gzip, FastqWriterFactory factory) {
		final String first = buildExtension(prefix, "_1", gzip);
		final String second = buildExtension(prefix, "_2", gzip);
		final String single = buildExtension(prefix, "_SE", gzip);
		this.first = factory.newWriter(new File(first));
		this.second = factory.newWriter(new File(second));
		this.single = new Lazy<FastqWriter>(new Lazy.LazyInitializer<FastqWriter>() {

			@Override
			public FastqWriter make() {
				return factory.newWriter(new File(single));
			}
		});
		this.countPairs = 0;
		this.countSingle = 0;
	}

	/**
	 * Easy constructor with default factory
	 *
	 * @param prefix
	 * @param gzip
	 */
	public PairFastqWriters(String prefix, boolean gzip) {
		this(prefix, gzip, DEFAULT_FACTORY);
	}

	/**
	 * Generate the extension
	 *
	 * @param prefix prefix to add
	 * @param suffix suffix to add
	 * @param gzip   if is gzip
	 *
	 * @return the name in the format prefixsuffix.fq or prefixsuffix.fq.gz
	 */
	private String buildExtension(String prefix, String suffix, boolean gzip) {
		StringBuilder builder = new StringBuilder(prefix);
		builder.append(suffix);
		builder.append(".fq");
		if (gzip) {
			builder.append(".gz");
		}
		return builder.toString();
	}

	/**
	 * Write in the first writer (not accesible: must to be paired)
	 *
	 * @param record
	 */
	private void writeFirst(FastqRecord record) {
		first.write(record);
	}

	/**
	 * Write in the second writer (not accesible: must to be paired)
	 *
	 * @param record
	 */
	private void writeSecond(FastqRecord record) {
		second.write(record);
	}

	/**
	 * Write in the single writer (the only accessible, because does not need to be paired)
	 *
	 * @param record
	 */
	public void writeSingle(FastqRecord record) {
		countSingle++;
		single.get().write(record);
	}

	/**
	 * Write in the single writer (the only accessible, because does not need to be paired)
	 *
	 * @param record
	 */
	public void writeSingle(SAMRecord record) {
		writeSingle(toFastqRecord(record, null));
	}

	/**
	 * Write in pairs
	 *
	 * @param firstRecord
	 * @param secondRecord
	 */
	public void writePairs(FastqRecord firstRecord, FastqRecord secondRecord) {
		countPairs++;
		writeFirst(firstRecord);
		writeSecond(secondRecord);
	}

	/**
	 * Check if the BAM/SAM records are correct pairs and write them as a Fastq The first is the
	 *
	 * @param record1
	 * @param record2
	 */
	public void writePairs(SAMRecord record1, SAMRecord record2) {
		assertPairedMates(record1, record2);
		FastqRecord pair1 = (record1.getFirstOfPairFlag()) ? toFastqRecord(record1, 1) : toFastqRecord(record2, 1);
		FastqRecord pair2 = (record1.getFirstOfPairFlag()) ? toFastqRecord(record2, 2) : toFastqRecord(record1, 2);
		writePairs(pair1, pair2);
	}

	/**
	 * Convert a SAMRecord to a FastqRecord (reverse complement if this flag is set)
	 *
	 * @param record
	 * @param mateNumber
	 *
	 * @return
	 */
	private static FastqRecord toFastqRecord(SAMRecord record, Integer mateNumber) {
		String seqName = (mateNumber == null) ?
			record.getReadName() :
			String.format("%s/%d", record.getReadName(), mateNumber);
		String readString = record.getReadString();
		String qualityString = record.getBaseQualityString();
		if (record.getReadNegativeStrandFlag()) {
			readString = SequenceUtil.reverseComplement(readString);
			qualityString = StringUtil.reverseString(qualityString);
		}
		return new FastqRecord(seqName, readString, "", qualityString);
	}

	/**
	 * Assert that both pairs are mates
	 *
	 * @param record1
	 * @param record2
	 */
	private static void assertPairedMates(final SAMRecord record1, final SAMRecord record2) {
		if (!(record1.getFirstOfPairFlag() && record2.getSecondOfPairFlag() || record2.getFirstOfPairFlag() && record1
			.getSecondOfPairFlag())) {
			throw new SAMException("Illegal mate state: " + record1.getReadName());
		}
	}

	public String toString() {
		return String.format("%s reads in pairs; %s reads without mate", Formats.commaFmt.format(countPairs),
			Formats.commaFmt.format(countSingle));
	}

	/**
	 * Close all the readers
	 */
	public void close() {
		first.close();
		second.close();
		if (single.isInitialized()) {
			single.get().close();
		}
	}
}
