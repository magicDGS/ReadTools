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

import org.magicdgs.readtools.engine.sourcehandler.ReadsSourceHandler;
import org.magicdgs.readtools.metrics.FilterMetric;
import org.magicdgs.readtools.metrics.TrimmingMetric;
import org.magicdgs.readtools.metrics.trimming.TrimStat;
import org.magicdgs.readtools.tools.trimming.trimmers.Trimmer;
import org.magicdgs.readtools.tools.trimming.trimmers.TrimmerBuilder;
import org.magicdgs.readtools.utils.read.RTReadUtils;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;
import org.magicdgs.readtools.utils.read.filter.NoAmbiguousSequenceReadFilter;
import org.magicdgs.readtools.utils.read.transformer.trimming.CutReadTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.MottQualityTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrailingNtrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;
import org.magicdgs.readtools.utils.tests.BaseTest;
import org.magicdgs.readtools.utils.tests.TestResourcesUtils;

import htsjdk.samtools.util.Histogram;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.engine.filters.ReadLengthReadFilter;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimAndFilterPipelineUnitTest extends BaseTest {

    @DataProvider
    public Object[][] badParams() {
        return new Object[][] {
                {null, true, false, Collections.emptyList()},
                {Collections.emptyList(), true, true, Collections.emptyList()},
                {Collections.emptyList(), false, true, null}
        };
    }

    @Test(dataProvider = "badParams", expectedExceptions = IllegalArgumentException.class)
    public void testBadConstructorParams(List<TrimmingFunction> trimmers, boolean disable5p,
            boolean disable3p, List<ReadFilter> filters) {
        new TrimAndFilterPipeline(trimmers, disable5p, disable3p, filters);
    }

    @DataProvider
    public Object[][] listsForMetrics() throws Exception {
        final TrimmingFunction cutRead = new CutReadTrimmer(1, 1);
        final TrimmingFunction trimNs = new TrailingNtrimmer();
        return new Object[][] {
                {Collections.emptyList(), Collections.emptyList()},
                {Collections.singletonList(cutRead), Collections.emptyList()},
                {Collections.emptyList(), Collections.singletonList(ReadFilterLibrary.MAPPED)},
                {Collections.singletonList(cutRead),
                        Collections.singletonList(ReadFilterLibrary.MAPPED)},
                {Arrays.asList(cutRead, trimNs),
                        Arrays.asList(ReadFilterLibrary.MAPPED, ReadFilterLibrary.GOOD_CIGAR)}
        };
    }

    @Test(dataProvider = "listsForMetrics")
    public void testGetMetrics(final List<TrimmingFunction> trimmers,
            final List<ReadFilter> filters) {
        final TrimAndFilterPipeline pipeline =
                new TrimAndFilterPipeline(trimmers, false, false, filters);
        final List<TrimmingMetric> trimmingMetrics = pipeline.getTrimmingStats();
        final List<FilterMetric> filterMetrics = pipeline.getFilterStats();

        // the same number of metrics than the passed lists
        Assert.assertEquals(trimmingMetrics.size(), trimmers.size());
        Assert.assertEquals(filterMetrics.size(), filters.size());

        // assert that the metrics are initialized at 0
        IntStream.range(0, trimmingMetrics.size()).forEach(index ->
                testTrimmingMetric(trimmingMetrics.get(index),
                        trimmers.get(index).getClass().getSimpleName(), 0, 0, 0, 0));
        IntStream.range(0, filterMetrics.size()).forEach(index ->
                testFilterMetric(filterMetrics.get(index),
                        filters.get(index).getClass().getSimpleName(), 0, 0));

        // returned lists should be unmodifiable for safety
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> trimmingMetrics.add(new TrimmingMetric()));
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> filterMetrics.add(new FilterMetric()));
    }

    @DataProvider(name = "cutReadData")
    public Iterator<Object[]> getCutReadData() throws Exception {
        final List<Object[]> data = new ArrayList<>();
        // this are completely trim in both sides
        for (int i = 1; i <= 10; i++) {
            data.add(new Object[] {ArtificialReadUtils.createArtificialRead(i + "M"), true, true});
        }
        return data.iterator();
    }

    // TODO: this test may fail for completely trimmed read if it takes into account disabling 3/5 prime
    @Test
    public void testCutReadTrimmerWithoutFilter() throws Exception {
        final TrimmingFunction tf = new CutReadTrimmer(1, 1);
        final String trimmerName = "CutReadTrimmer";
        final boolean[] trueFalse = new boolean[] {true, false};

        for (final boolean disable5p : trueFalse) {
            final int expected5p = (disable5p) ? 0 : 1;
            for (final boolean disable3p : trueFalse) {
                final int expected3p = (disable3p) ? 0 : 1;
                // don not test both disable
                if (!(disable5p && disable3p)) {
                    final TrimAndFilterPipeline pipeline = new TrimAndFilterPipeline(
                            Collections.singletonList(tf),
                            disable5p, disable3p,
                            Collections.emptyList());

                    final GATKRead trimmedRead = ArtificialReadUtils.createArtificialRead("10M");
                    final int lengthAfterTrimming = trimmedRead.getLength()
                            - ((disable5p) ? 0 : 1) - ((disable3p) ? 0 : 1);
                    final GATKRead completelyTrimRead =
                            ArtificialReadUtils.createArtificialRead("2M");

                    // this is the actual object
                    final TrimmingMetric metric = pipeline.getTrimmingStats().get(0);
                    testTrimmingMetric(metric, trimmerName, 0, 0, 0, 0);

                    // apply the trimmed read -> pass the filter and the length is less
                    Assert.assertTrue(pipeline.test(trimmedRead));
                    Assert.assertEquals(trimmedRead.getLength(), lengthAfterTrimming,
                            "no trimming applied");
                    testTrimmingMetric(metric, trimmerName, 1, expected5p, expected3p, 0);

                    // apply to the completely trim read -> do not pass the filter
                    Assert.assertFalse(pipeline.test(completelyTrimRead));
                    final String ctTag = completelyTrimRead.getAttributeAsString("ct");
                    Assert.assertNotNull(ctTag);
                    Assert.assertNotEquals(ctTag, "0");
                    testTrimmingMetric(metric, trimmerName, 2, expected5p, expected3p, 1);
                }
            }
        }
    }

    // TODO: this test may fail for completely trimmed read if it takes into account disabling 3/5 prime
    @Test
    public void testNoTrimmerWithFilter() throws Exception {
        final ReadFilter rf = new ReadLengthReadFilter(5, 100);
        final String filterName = "ReadLengthReadFilter";
        final boolean[] trueFalse = new boolean[] {true, false};

        for (final boolean disable5p : trueFalse) {
            for (final boolean disable3p : trueFalse) {
                // don not test both disable
                if (!(disable5p && disable3p)) {
                    final TrimAndFilterPipeline pipeline = new TrimAndFilterPipeline(
                            Collections.emptyList(),
                            disable5p, disable3p,
                            Collections.singletonList(rf));

                    final GATKRead notFilterRead = ArtificialReadUtils.createArtificialRead("10M");
                    final GATKRead filteredRead = ArtificialReadUtils.createArtificialRead("2M");

                    // this is the actual object
                    final FilterMetric metric = pipeline.getFilterStats().get(0);
                    testFilterMetric(metric, filterName, 0, 0);

                    // apply the not filtered read
                    Assert.assertTrue(pipeline.test(notFilterRead));
                    testFilterMetric(metric, filterName, 1, 1);

                    // apply to the filtered read
                    Assert.assertFalse(pipeline.test(filteredRead));
                    testFilterMetric(metric, filterName, 2, 1);
                }
            }
        }
    }

    // TODO: this test may fail for completely trimmed read if it takes into account disabling 3/5 prime
    @Test
    public void testCutReadTrimmerWithFilter() throws Exception {
        final TrimmingFunction tf = new CutReadTrimmer(1, 1);
        final String trimmerName = "CutReadTrimmer";
        final ReadFilter rf = new ReadLengthReadFilter(5, 100);
        final String filterName = "ReadLengthReadFilter";
        final boolean[] trueFalse = new boolean[] {true, false};

        for (final boolean disable5p : trueFalse) {
            final int expected5p = (disable5p) ? 0 : 1;
            for (final boolean disable3p : trueFalse) {
                final int expected3p = (disable3p) ? 0 : 1;
                // don not test both disable
                if (!(disable5p && disable3p)) {
                    final TrimAndFilterPipeline pipeline = new TrimAndFilterPipeline(
                            Collections.singletonList(tf),
                            disable5p, disable3p,
                            Collections.singletonList(rf));

                    // completely trim read
                    final GATKRead completelyTrimAndFilteredRead =
                            ArtificialReadUtils.createArtificialRead("2M");
                    // trimed and not filtered -> compute the length for assertion
                    final GATKRead trimmedReadNotFiltered =
                            ArtificialReadUtils.createArtificialRead("10M");
                    final int lengthAfterTrimmingNotFiltered =
                            trimmedReadNotFiltered.getLength()
                                    - ((disable5p) ? 0 : 1) - ((disable3p) ? 0 : 1);
                    // trimmed and filtered read -> compute length for assertion
                    final GATKRead trimmedReadAndFiltered =
                            ArtificialReadUtils.createArtificialRead("4M");
                    final int lengthAfterTrimmingFiltered =
                            trimmedReadAndFiltered.getLength()
                                    - ((disable5p) ? 0 : 1) - ((disable3p) ? 0 : 1);


                    // this is the actual object
                    final TrimmingMetric trimMetric = pipeline.getTrimmingStats().get(0);
                    testTrimmingMetric(trimMetric, trimmerName, 0, 0, 0, 0);
                    // this is the actual object
                    final FilterMetric filterMetric = pipeline.getFilterStats().get(0);
                    testFilterMetric(filterMetric, filterName, 0, 0);

                    // apply the trimmed read -> pass the filter and the length is less
                    Assert.assertTrue(pipeline.test(trimmedReadNotFiltered));
                    Assert.assertEquals(trimmedReadNotFiltered.getLength(),
                            lengthAfterTrimmingNotFiltered, "no trimming applied");
                    testTrimmingMetric(trimMetric, trimmerName, 1, expected5p, expected3p, 0);
                    testFilterMetric(filterMetric, filterName, 1, 1);

                    // apply to the completely trim read -> do not pass the filter
                    Assert.assertFalse(pipeline.test(completelyTrimAndFilteredRead));
                    final String ctTag = completelyTrimAndFilteredRead.getAttributeAsString("ct");
                    Assert.assertNotNull(ctTag);
                    Assert.assertNotEquals(ctTag, "0");
                    testTrimmingMetric(trimMetric, trimmerName, 2, expected5p, expected3p, 1);
                    // this is not updated, because completely trim ones do not reach the filters
                    testFilterMetric(filterMetric, filterName, 1, 1);

                    // apply the trimmed read -> do not pass the filter and the length is less
                    Assert.assertFalse(pipeline.test(trimmedReadAndFiltered));
                    Assert.assertEquals(trimmedReadAndFiltered.getLength(),
                            lengthAfterTrimmingFiltered, "no trimming applied");
                    testTrimmingMetric(trimMetric, trimmerName, 3,
                            expected5p + expected5p, expected3p + expected3p, 1);
                    testFilterMetric(filterMetric, filterName, 2, 1);

                }
            }
        }
    }

    // TODO: this test may fail for completely trimmed read if it takes into account disabling 3/5 prime
    @Test
    public void testCollectingTrimmingMetricTransformer() throws Exception {
        final TrimmingFunction tf = new CutReadTrimmer(1, 1);
        final String trimmerName = "CutReadTrimmer";
        final boolean[] trueFalse = new boolean[] {true, false};

        for (final boolean disable5p : trueFalse) {
            final int expected5p = (disable5p) ? 0 : 1;
            for (final boolean disable3p : trueFalse) {
                final int expected3p = (disable3p) ? 0 : 1;
                // dont test both disable
                if (!(disable5p && disable3p)) {
                    final TrimAndFilterPipeline.CollectingTrimmingMetricTransformer ctmt =
                            new TrimAndFilterPipeline.CollectingTrimmingMetricTransformer(tf,
                                    disable5p, disable3p);

                    final GATKRead trimmedRead = ArtificialReadUtils.createArtificialRead("10M");
                    final GATKRead completelyTrimRead =
                            ArtificialReadUtils.createArtificialRead("2M");

                    testTrimmingMetric(ctmt.metric, trimmerName, 0, 0, 0, 0);
                    // apply the trimmed read
                    ctmt.apply(trimmedRead);
                    testTrimmingMetric(ctmt.metric, trimmerName, 1, expected5p, expected3p, 0);
                    // apply to the completely trim read
                    ctmt.apply(completelyTrimRead);
                    testTrimmingMetric(ctmt.metric, trimmerName, 2, expected5p, expected3p, 1);
                    // now if we pass them, it only updates the total
                    // they are already trimmed
                    ctmt.apply(trimmedRead);
                    ctmt.apply(completelyTrimRead);
                    testTrimmingMetric(ctmt.metric, trimmerName, 4, expected5p, expected3p, 1);
                }
            }
        }

        // testing now with anonymous class
        final TrimmingFunction anonymous = new TrimmingFunction() {
            @Override
            protected void update(GATKRead read) {
                // do nothing
            }
        };
        final TrimAndFilterPipeline.CollectingTrimmingMetricTransformer ctmt =
                new TrimAndFilterPipeline.CollectingTrimmingMetricTransformer(anonymous,
                        false, false);
        testTrimmingMetric(ctmt.metric, "DEFAULT", 0, 0, 0, 0);
    }

    @Test
    public void testCollectingFilterMetricFilter() throws Exception {
        final String filterName = "GoodCigarReadFilter";
        final TrimAndFilterPipeline.CollectingFilterMetricFilter cfmf =
                new TrimAndFilterPipeline.CollectingFilterMetricFilter(
                        ReadFilterLibrary.GOOD_CIGAR);
        final GATKRead notFilterRead = ArtificialReadUtils.createArtificialRead("10M");
        final GATKRead filterRead = ArtificialReadUtils.createArtificialRead("1M1I1I1M");
        testFilterMetric(cfmf.metric, filterName, 0, 0);
        Assert.assertFalse(cfmf.test(filterRead));
        testFilterMetric(cfmf.metric, filterName, 1, 0);
        Assert.assertTrue(cfmf.test(notFilterRead));
        testFilterMetric(cfmf.metric, filterName, 2, 1);

        // testing now with anonymous class
        final ReadFilter anonymous = new ReadFilter() {
            @Override
            public boolean test(GATKRead read) {
                return false;
            }
        };
        final TrimAndFilterPipeline.CollectingFilterMetricFilter cfmf2 =
                new TrimAndFilterPipeline.CollectingFilterMetricFilter(anonymous);
        testFilterMetric(cfmf2.metric, "DEFAULT", 0, 0);
    }

    // helper method for test filter metric
    private void testFilterMetric(final FilterMetric metric,
            final String filterName, final int total, final int passed) {
        Assert.assertEquals(metric.FILTER, filterName, "FilterMetric.FILTER");
        Assert.assertEquals(metric.TOTAL, total, "FilterMetric.TOTAL");
        Assert.assertEquals(metric.PASSED, passed, "FilterMetric.PASSED");
    }

    // helper method for test trimming metric
    private void testTrimmingMetric(final TrimmingMetric metric,
            final String trimmerName, final int total,
            final int trimmed5p, final int trimmed3p, final int completelyTrim) {
        Assert.assertEquals(metric.TRIMMER, trimmerName, "TrimmingMetric.TRIMMER");
        Assert.assertEquals(metric.TOTAL, total, "TrimmingMetric.TOTAL");
        Assert.assertEquals(metric.TRIMMED_5_P, trimmed5p, "TrimmingMetric.TRIMMED_5_P");
        Assert.assertEquals(metric.TRIMMED_3_P, trimmed3p, "TrimmingMetric.TRIMMED_3_P");
        Assert.assertEquals(metric.TRIMMED_COMPLETE, completelyTrim,
                "TrimmingMetric.TRIMMED_COMPLETE");
    }

    @DataProvider(name = "concordanceData")
    public Iterator<Object[]> trimmerConcordanceData() throws Exception {
        // TODO: these file will disappear at some point
        final File input1 = TestResourcesUtils
                .getReadToolsTestResource("org/magicdgs/readtools/data/SRR1931701_1.fq");
        final File input2 = TestResourcesUtils
                .getReadToolsTestResource("org/magicdgs/readtools/data/SRR1931701_2.fq");
        final List<Object[]> data = new ArrayList<>();
        final boolean[] trueFalse = new boolean[] {true, false};
        for (final int qualThreshold : new int[] {18, 20}) {
            for (final int minLength : new int[] {40, 60}) {
                for (final int maxLength : new int[] {75, Integer.MAX_VALUE}) {
                    for (final boolean discardRemainingNs : trueFalse) {
                        for (final boolean no5p : trueFalse) {
                            data.add(new Object[] {input1,
                                    qualThreshold, minLength, maxLength, discardRemainingNs, no5p});
                            data.add(new Object[] {input2,
                                    qualThreshold, minLength, maxLength, discardRemainingNs, no5p});
                        }
                    }
                }
            }
        }
        return data.iterator();
    }

    // TODO: this test should be removed
    // this test only reads a file and check that the pipeline provides the same result as the previous pipeline
    @Test(dataProvider = "concordanceData")
    public void testEqualTrimmingWithOldTrimmer(final File inputFile, final int qualThreshold,
            final int minLength, final int maxLength,
            final boolean discardRemainingNsm, final boolean no5p) throws Exception {
        // setting the trimmer
        final Trimmer trimmer = new TrimmerBuilder(true)
                .setTrimQuality(true)
                .setQualityThreshold(qualThreshold)
                .setMinLength(minLength)
                .setMaxLength(maxLength)
                .setDiscardRemainingNs(discardRemainingNsm)
                .setNo5pTrimming(no5p)
                .build();

        final Histogram<Integer> lengthHistogram = new Histogram<>();

        // for reusing
        final ReadLengthReadFilter rlrf = new ReadLengthReadFilter(minLength, maxLength);
        // setting the pipeline
        final TrimAndFilterPipeline pipeline = new TrimAndFilterPipeline(
                // in the previous pipeline, only two trimmers were applied
                Arrays.asList(new TrailingNtrimmer(), new MottQualityTrimmer(qualThreshold)),
                no5p, false, // in the previous trimmer, it was false
                (discardRemainingNsm)
                        ? Arrays.asList(new NoAmbiguousSequenceReadFilter(), rlrf)
                        : Collections.singletonList(rlrf)
        );

        // open the file with default factory
        final ReadsSourceHandler handler = ReadsSourceHandler
                .getHandler(inputFile.getAbsolutePath(), new ReadReaderFactory());
        final Iterable<GATKRead> readsIt = handler::toIterator;

        // for using trimRead
        final TrimStat trimStat = trimmer.getTrimStats().get(0);

        int totalDiscardedInternalNs = 0;
        int checkedDiscardedInternalNs = 0;
        for (final GATKRead readForTrimmer : readsIt) {
            // copy and trim both independentlt
            final GATKRead readForPipeline = readForTrimmer.deepCopy();
            trimmer.trimRead(readForTrimmer, trimStat, lengthHistogram);
            final boolean passFilter = pipeline.test(readForPipeline);

            // this is a known bug in the previous trimmer
            // see https://github.com/magicDGS/ReadTools/issues/101 for more details
            if (discardRemainingNsm && readForTrimmer.getAttributeAsInteger("ct") == 11) {
                totalDiscardedInternalNs++;
                // if the length is the same, we can check concordance
                if (readForTrimmer.getLength() == readForPipeline.getLength()) {
                    checkedDiscardedInternalNs++;
                    // it should not pass the filter
                    Assert.assertFalse(passFilter,
                            "not filtering by remaining Ns in the same way (trimmer vs. pipeline):\n"
                                    + readForTrimmer.getSAMString() + "\n"
                                    + readForPipeline.getSAMString() + "\n");
                    Assert.assertEquals(readForPipeline.getBases(),
                            readForTrimmer.getBases(), "wrong bases");
                    Assert.assertEquals(readForPipeline.getBaseQualities(),
                            readForTrimmer.getBaseQualities(), "wrong quals");
                }
            } else {
                // check the trimming
                Assert.assertEquals(passFilter, !RTReadUtils.isCompletelyTrimRead(readForTrimmer),
                        "no filter in the same way (trimer vs. pipeline):\n"
                                + readForTrimmer.getSAMString() + "\n"
                                + readForPipeline.getSAMString() + "\n");
                Assert.assertEquals(readForPipeline.getLength(),
                        readForTrimmer.getLength(), "wrong length");
                Assert.assertEquals(readForPipeline.getBases(),
                        readForTrimmer.getBases(), "wrong bases");
                Assert.assertEquals(readForPipeline.getBaseQualities(),
                        readForTrimmer.getBaseQualities(), "wrong quals");
            }
        }

        // log to the test output that this is not considered
        if (totalDiscardedInternalNs != checkedDiscardedInternalNs) {
            log(String.format(
                    "%s/%s internal Ns discarded reads were not checked due to a known issue.",
                    totalDiscardedInternalNs - checkedDiscardedInternalNs,
                    totalDiscardedInternalNs));
        }

        handler.close();
    }

}