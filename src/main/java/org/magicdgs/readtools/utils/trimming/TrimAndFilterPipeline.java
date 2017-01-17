/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.utils.trimming;

import org.magicdgs.readtools.metrics.FilterMetric;
import org.magicdgs.readtools.metrics.TrimmingMetric;
import org.magicdgs.readtools.utils.read.RTReadUtils;
import org.magicdgs.readtools.utils.read.filter.CompletelyTrimReadFilter;
import org.magicdgs.readtools.utils.read.transformer.trimming.ApplyTrimResultReadTransfomer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;

import com.google.common.annotations.VisibleForTesting;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Implements a pipeline for trimming in place (through {@link TrimmingFunction}) and filter
 * (through {@link ReadFilter}) a {@link GATKRead}, collecting at the same time statistics for the
 * pipeline into metrics.
 *
 * The pipeline is as following for {@link #test(GATKRead)}:
 *
 * - Each of the trimmers is applied in order.
 * - After all trimmers are applied, {@link ApplyTrimResultReadTransfomer} updates the read.
 * - A first filter is applied to check if the read is completely trimmed.
 * - A composed AND filter with the provided ones is applied and returned the value.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimAndFilterPipeline extends ReadFilter {

    private static final ReadFilter COMPLETELY_TRIM_READ_FILTER = new CompletelyTrimReadFilter();

    // these are the two pipelines, visible in the package for testing alone
    private final ReadTransformer trimmingPipeline;
    private final ReadFilter filteringPipeline;

    // these are the metrics, accumulated on construction for the collecting wrappers
    private final List<TrimmingMetric> trimmingMetrics;
    private final List<FilterMetric> filterMetrics;

    /**
     * Constructor.
     *
     * @param trimmers      trimmers to apply in order.
     * @param disable5pTrim disable trimming in the 5 prime.
     * @param disable3pTrim disable trimming in the 3 prime.
     * @param filters       filters to apply in order after trimming.
     */
    public TrimAndFilterPipeline(final List<TrimmingFunction> trimmers,
            final boolean disable5pTrim, final boolean disable3pTrim,
            final List<ReadFilter> filters) {
        // param checking
        Utils.nonNull(trimmers, "null trimmers");
        Utils.nonNull(filters, "null filters");
        Utils.validateArg(!(disable5pTrim && disable3pTrim),
                "at least one end (5' or 3') should be trimmed");

        // setting simple params
        this.trimmingMetrics = new ArrayList<>(trimmers.size());
        this.filterMetrics = new ArrayList<>(filters.size());

        // set up the trimming pipeline
        if (trimmers.isEmpty()) {
            // we do not need to apply the trimming result
            this.trimmingPipeline = ReadTransformer.identity();
        } else {
            this.trimmingPipeline = composeTrimmingFunction(trimmers, disable5pTrim, disable3pTrim);
        }

        // set up the filter pipeline
        if (filters.isEmpty()) {
            this.filteringPipeline = COMPLETELY_TRIM_READ_FILTER;
        } else {
            // this should leave at the beginning the COMPLETELY_TRIM_READ_FILTER
            this.filteringPipeline = filters.stream()
                    .map(rf -> {
                        final CollectingFilterMetricFilter cfmf =
                                new CollectingFilterMetricFilter(rf);
                        filterMetrics.add(cfmf.metric);
                        return (ReadFilter) cfmf;
                    })
                    .reduce(COMPLETELY_TRIM_READ_FILTER, ReadFilter::and);
        }

    }

    // helper function to compose the trimming functions into a single ReadTransformer
    private ReadTransformer composeTrimmingFunction(final List<TrimmingFunction> trimmingFunctions,
            final boolean disable5pTrim, final boolean disable3pTrim) {

        // Reducing will leave an unnecessary identity function at the start, so iterate instead
        final CollectingTrimmingMetricTransformer first =
                new CollectingTrimmingMetricTransformer(trimmingFunctions.get(0),
                        disable5pTrim, disable3pTrim);
        trimmingMetrics.add(first.metric);
        ReadTransformer composed = first;

        // accumulate the rest
        for (int i = 1; i < trimmingFunctions.size(); i++) {
            final CollectingTrimmingMetricTransformer ctmt =
                    new CollectingTrimmingMetricTransformer(trimmingFunctions.get(i),
                            disable5pTrim, disable3pTrim);
            trimmingMetrics.add(ctmt.metric);
            composed = composed.andThen(ctmt);
        }

        return composed.andThen(new ApplyTrimResultReadTransfomer(disable5pTrim, disable3pTrim));
    }

    /**
     * Apply the trimming/filtering pipeline.
     *
     * @return {@code true} if the read pass all the filters after trimming; {@code false}
     * otherwise.
     */
    @Override
    public boolean test(final GATKRead read) {
        // maybe pre-filter will allow to reduce computation,
        // but this can be done with a different tool
        return filteringPipeline.test(trimmingPipeline.apply(read));
    }

    /** Gets the trimming statistics as a unmodifiable list. */
    public List<TrimmingMetric> getTrimmingStats() {
        return Collections.unmodifiableList(trimmingMetrics);
    }

    /** Gets the filtering statistics as an unmodifiable list. */
    public List<FilterMetric> getFilterStats() {
        return Collections.unmodifiableList(filterMetrics);
    }

    // class for collect metrics for the trimming pipeline
    @VisibleForTesting
    static class CollectingTrimmingMetricTransformer implements ReadTransformer {
        public static final long serialVersionUID = 1L;

        @VisibleForTesting
        final TrimmingMetric metric;
        private final TrimmingFunction delegate;
        private final BiPredicate<GATKRead, Integer> fivePrimeUpdate;
        private final BiPredicate<GATKRead, Integer> threePrimeUpdate;

        @VisibleForTesting
        CollectingTrimmingMetricTransformer(final TrimmingFunction delegate,
                final boolean disable5pTrim, final boolean disable3pTrim) {
            // we don't need validation here for 5/3 prime disabling, because it was done before
            this.delegate = delegate;
            this.metric = new TrimmingMetric(delegate.getClass().getSimpleName());

            this.fivePrimeUpdate = (disable5pTrim)
                    ? (read, previous) -> false
                    : (read, previous) -> RTReadUtils.getTrimmingStartPoint(read) != previous;
            this.threePrimeUpdate = (disable3pTrim)
                    ? (read, previous) -> false
                    : (read, previous) -> RTReadUtils.getTrimmingEndPoint(read) != previous;
        }

        @Override
        public GATKRead apply(final GATKRead read) {
            metric.TOTAL++;
            // get the completely trim flag before
            final boolean wasCompletelyTrim = RTReadUtils.updateCompletelyTrimReadFlag(read);
            final int previousStartTrimPoint = RTReadUtils.getTrimmingStartPoint(read);
            final int previousEndTrimPoint = RTReadUtils.getTrimmingEndPoint(read);

            // trimming function modify in place the read
            delegate.apply(read);
            // update the metrics
            // TODO: maybe this should check the disable 3/5 prime?
            if (!wasCompletelyTrim && RTReadUtils.updateCompletelyTrimReadFlag(read)) {
                metric.TRIMMED_COMPLETE++;
            } else {
                if (fivePrimeUpdate.test(read, previousStartTrimPoint)) {
                    metric.TRIMMED_5_P++;
                }
                if (threePrimeUpdate.test(read, previousEndTrimPoint)) {
                    metric.TRIMMED_3_P++;
                }
            }
            return read;
        }
    }

    // class for collect metric for the filtering pipeline
    @VisibleForTesting
    static class CollectingFilterMetricFilter extends ReadFilter {
        public static final long serialVersionUID = 1L;

        @VisibleForTesting
        final FilterMetric metric;
        private final ReadFilter delegate;

        @VisibleForTesting
        CollectingFilterMetricFilter(final ReadFilter delegate) {
            this.delegate = delegate;
            this.metric = new FilterMetric(delegate.getClass().getSimpleName());
        }

        @Override
        public boolean test(final GATKRead read) {
            metric.TOTAL++;
            // trimming function modify in place the read
            final boolean pass = delegate.test(read);
            // update the metrics
            if (pass) {
                metric.PASSED++;
            }
            return pass;
        }
    }
}
