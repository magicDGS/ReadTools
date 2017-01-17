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
package org.magicdgs.readtools.tools.trimming.trimmers;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.readtools.metrics.trimming.SingleEndTrimming;
import org.magicdgs.readtools.metrics.trimming.TrimStat;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.Histogram;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of trimmer for single end that is accumulating the stats as a single end even for
 * FastqPairEndRecords
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @deprecated use {@link org.magicdgs.readtools.utils.trimming.TrimAndFilterPipeline} instead.
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

    TrimmerSingle(final boolean trimQuality, final int qualThreshold, final int minLength,
            final int maxLength, final boolean discardRemainingNs, final boolean no5ptrim) {
        super(trimQuality, qualThreshold, minLength, maxLength, discardRemainingNs, no5ptrim);
        metric = new TrimStat("single");
        histogram = new Histogram<>("length", "count");
    }

    @Override
    public FastqPairedRecord trimFastqPairedRecord(final FastqPairedRecord record,
            final FastqQualityFormat format) {
        FastqRecord record1 = trimFastqRecord(record.getRecord1(), format);
        FastqRecord record2 = trimFastqRecord(record.getRecord2(), format);
        return new FastqPairedRecord(record1, record2);
    }

    @Override
    public FastqRecord trimFastqRecord(final FastqRecord record, final FastqQualityFormat format) {
        return trimFastqRecord(record, format, metric, histogram);
    }

    @Override
    public void printTrimmerMetrics(final Path metricsFile) throws IOException {
        try (final Writer writer = Files.newBufferedWriter(metricsFile)) {
            final MetricsFile<TrimStat, Integer> metrics = new MetricsFile<>();
            metrics.addMetric(metric);
            metrics.addHistogram(histogram);
            metrics.addHeader(header);
            metrics.write(writer);
        }
    }

    @Override
    public List<TrimStat> getTrimStats() {
        return Collections.singletonList(metric);
    }
}
