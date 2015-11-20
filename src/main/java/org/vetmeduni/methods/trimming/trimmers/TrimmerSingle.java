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
package org.vetmeduni.methods.trimming.trimmers;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.Histogram;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.methods.trimming.trimmers.stats.SingleEndTrimming;
import org.vetmeduni.methods.trimming.trimmers.stats.TrimStat;

import java.io.File;

/**
 * Implementation of trimmer for single end that is accumulating the stats as a single end even for FastqPairEndRecords
 *
 * @author Daniel Gómez-Sánchez
 */
public class TrimmerSingle extends Trimmer {

	/**
	 * The metrics for the trimmer
	 */
	private final TrimStat metric;

	/**
	 * The histogram for the trimmer
	 */
	private final Histogram<Integer> histogram;

	/**
	 * The header for the trimmer (it is always the same)
	 */
	private static final SingleEndTrimming header = new SingleEndTrimming();

	protected TrimmerSingle(boolean trimQuality, int qualThreshold, int minLength, boolean discardRemainingNs,
		boolean no5ptrim) {
		super(trimQuality, qualThreshold, minLength, discardRemainingNs, no5ptrim);
		metric = new TrimStat("single");
		histogram = new Histogram<>("length", "count");
	}

	@Override
	public FastqPairedRecord trimFastqPairedRecord(FastqPairedRecord record, FastqQualityFormat format) {
		FastqRecord record1 = trimFastqRecord(record.getRecord1(), format);
		FastqRecord record2 = trimFastqRecord(record.getRecord2(), format);
		return new FastqPairedRecord(record1, record2);
	}

	@Override
	public FastqRecord trimFastqRecord(FastqRecord record, FastqQualityFormat format) {
		return trimFastqRecord(record, format, metric, histogram);
	}

	@Override
	public void printTrimmerMetrics(File metricsFile) {
		final MetricsFile<TrimStat, Integer> metrics = new MetricsFile<>();
		metrics.addMetric(metric);
		metrics.addHistogram(histogram);
		metrics.addHeader(header);
		metrics.write(metricsFile);
	}
}
