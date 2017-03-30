/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.tools.trimming;

import org.magicdgs.readtools.cmd.plugin.TrimmerPluginDescriptor;
import org.magicdgs.readtools.metrics.FilterMetric;
import org.magicdgs.readtools.metrics.TrimmerMetric;
import org.magicdgs.readtools.utils.read.transformer.trimming.CutReadTrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrailingNtrimmer;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;
import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.barclay.argparser.CommandLineArgumentParser;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineParser;
import org.broadinstitute.hellbender.cmdline.GATKPlugin.GATKReadFilterPluginDescriptor;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.engine.filters.ReadLengthReadFilter;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimAndFilterPipelineUnitTest extends RTBaseTest {

    @DataProvider
    public Object[][] badParams() {
        return new Object[][] {
                {null, Collections.emptyList()},
                {Collections.emptyList(), Collections.emptyList()},
                {Collections.emptyList(), null}
        };
    }

    @Test(dataProvider = "badParams", expectedExceptions = IllegalArgumentException.class)
    public void testBadConstructorParams(List<TrimmingFunction> trimmers,
            List<ReadFilter> filters) {
        new TrimAndFilterPipeline(trimmers, filters);
    }

    @DataProvider
    public Object[][] listsForMetrics() throws Exception {
        return new Object[][] {
                {Collections.singletonList(new CutReadTrimmer(1, 1)), Collections.emptyList()},
                {Collections.emptyList(), Collections.singletonList(ReadFilterLibrary.MAPPED)},
                {Collections.singletonList(new CutReadTrimmer(1, 1)),
                        Collections.singletonList(ReadFilterLibrary.MAPPED)},
                {Arrays.asList(new CutReadTrimmer(1, 1), new TrailingNtrimmer()),
                        Arrays.asList(ReadFilterLibrary.MAPPED, ReadFilterLibrary.GOOD_CIGAR)}
        };
    }

    @Test(dataProvider = "listsForMetrics")
    public void testGetMetrics(final List<TrimmingFunction> trimmers,
            final List<ReadFilter> filters) {
        final TrimAndFilterPipeline pipeline =
                new TrimAndFilterPipeline(trimmers, filters);
        final List<TrimmerMetric> trimmerMetrics = pipeline.getTrimmingStats();
        final List<FilterMetric> filterMetrics = pipeline.getFilterStats();

        // the same number of metrics than the passed lists
        Assert.assertEquals(trimmerMetrics.size(), trimmers.size());
        // +1 in filtering, because the completely trimmed read
        Assert.assertEquals(filterMetrics.size(), filters.size() + 1);

        // assert that the first filter metric is CompletelyTrimReadFilter
        testFilterMetric(filterMetrics.get(0), "CompletelyTrimReadFilter", 0, 0);

        // assert that the metrics are initialized at 0
        IntStream.range(0, trimmerMetrics.size()).forEach(index ->
                testTrimmingMetric(trimmerMetrics.get(index),
                        trimmers.get(index).getClass().getSimpleName(), 0, 0, 0, 0));
        IntStream.range(1, filterMetrics.size()).forEach(index ->
                testFilterMetric(filterMetrics.get(index),
                        filters.get(index - 1).getClass().getSimpleName(), 0, 0));

        // returned lists should be unmodifiable for safety
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> trimmerMetrics.add(new TrimmerMetric()));
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

    @Test
    public void testCutReadTrimmerWithoutFilter() throws Exception {
        final String trimmerName = "CutReadTrimmer";
        final boolean[] trueFalse = new boolean[] {true, false};

        for (final boolean disable5p : trueFalse) {
            final int expected5p = (disable5p) ? 0 : 1;
            for (final boolean disable3p : trueFalse) {
                final int expected3p = (disable3p) ? 0 : 1;
                // don not test both disable
                if (!(disable5p && disable3p)) {

                    // should create each time for calling setDisableEnds
                    final TrimmingFunction tf = new CutReadTrimmer(1, 1);
                    tf.setDisableEnds(disable5p, disable3p);

                    final TrimAndFilterPipeline pipeline = new TrimAndFilterPipeline(
                            Collections.singletonList(tf),
                            Collections.emptyList());

                    final GATKRead trimmedRead = ArtificialReadUtils.createArtificialRead("10M");
                    final int lengthAfterTrimming = trimmedRead.getLength()
                            - ((disable5p) ? 0 : 1) - ((disable3p) ? 0 : 1);
                    final GATKRead completelyTrimRead =
                            ArtificialReadUtils.createArtificialRead("1M");
                    final GATKRead conditionalCompletelyTrim =
                            ArtificialReadUtils.createArtificialRead("2M");

                    // this is the actual object
                    final TrimmerMetric metric = pipeline.getTrimmingStats().get(0);
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

                    // apply to the conditional completely trim depends on the disabled primes
                    // if one of then is disabled, it is not going to be completely trimmed and filter out
                    Assert.assertEquals(pipeline.test(conditionalCompletelyTrim),
                            disable5p || disable3p);
                }
            }
        }
    }

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
                            Collections.singletonList(rf));

                    final GATKRead notFilterRead = ArtificialReadUtils.createArtificialRead("10M");
                    final GATKRead filteredRead = ArtificialReadUtils.createArtificialRead("2M");

                    // this is the actual object
                    final FilterMetric metric = pipeline.getFilterStats().get(1);
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

    @Test
    public void testCutReadTrimmerWithFilter() throws Exception {
        final String trimmerName = "CutReadTrimmer";
        final String filterName = "ReadLengthReadFilter";
        final boolean[] trueFalse = new boolean[] {true, false};

        for (final boolean disable5p : trueFalse) {
            final int expected5p = (disable5p) ? 0 : 1;
            for (final boolean disable3p : trueFalse) {
                final int expected3p = (disable3p) ? 0 : 1;

                final ReadFilter rf = new ReadLengthReadFilter(5, 100);

                // don not test both disable
                if (!(disable5p && disable3p)) {
                    // should create each time for calling setDisableEnds
                    final TrimmingFunction tf = new CutReadTrimmer(1, 1);
                    tf.setDisableEnds(disable5p, disable3p);

                    final TrimAndFilterPipeline pipeline = new TrimAndFilterPipeline(
                            Collections.singletonList(tf),
                            Collections.singletonList(rf));

                    // completely trim read
                    final GATKRead completelyTrimAndFilteredRead =
                            ArtificialReadUtils.createArtificialRead("1M");
                    final GATKRead conditionalCompletelyTrim =
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
                    final TrimmerMetric trimMetric = pipeline.getTrimmingStats().get(0);
                    testTrimmingMetric(trimMetric, trimmerName, 0, 0, 0, 0);
                    // this is the actual object
                    final FilterMetric filterMetric = pipeline.getFilterStats().get(1);
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


                    // apply to the conditional completely trim is always filter out because of length
                    Assert.assertEquals(pipeline.test(conditionalCompletelyTrim), false);
                    // the completely trim flag depends on the disabled ends
                    if (disable5p || disable3p) {
                        // if one of then is disabled, it is going to be not completely trimmed
                        Assert.assertEquals(conditionalCompletelyTrim.getAttributeAsString("ct"),
                                "0");
                    } else {
                        // if both are disabled, it is completely trimmed
                        Assert.assertNotEquals(conditionalCompletelyTrim.getAttributeAsString("ct"),
                                "0");
                    }


                }
            }
        }
    }

    @Test
    public void testCollectingTrimmingMetricTransformer() throws Exception {
        final String trimmerName = "CutReadTrimmer";
        final boolean[] trueFalse = new boolean[] {true, false};

        for (final boolean disable5p : trueFalse) {
            final int expected5p = (disable5p) ? 0 : 1;
            for (final boolean disable3p : trueFalse) {
                final int expected3p = (disable3p) ? 0 : 1;
                // dont test both disable
                if (!(disable5p && disable3p)) {
                    final TrimmingFunction tf = new CutReadTrimmer(1, 1);
                    tf.setDisableEnds(disable5p, disable3p);
                    final TrimAndFilterPipeline.CollectingTrimmingMetricTransformer ctmt =
                            new TrimAndFilterPipeline.CollectingTrimmingMetricTransformer(tf);

                    final GATKRead trimmedRead = ArtificialReadUtils.createArtificialRead("10M");
                    final GATKRead completelyTrimRead =
                            ArtificialReadUtils.createArtificialRead("1M");
                    final GATKRead conditionalCompletelyTrim =
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

                    // apply to the conditional
                    ctmt.apply(conditionalCompletelyTrim);
                    // it is only completely trim if both are false
                    final boolean ct = !(disable5p || disable3p);
                    testTrimmingMetric(ctmt.metric, trimmerName, 5,
                            (ct) ? expected5p : expected5p + expected5p,
                            (ct) ? expected3p : expected3p + expected3p,
                            (ct) ? 2 : 1);
                }
            }
        }

        // testing now with anonymous class
        final TrimmingFunction anonymous = new TrimmingFunction() {
            @Override
            protected void fillTrimPoints(GATKRead read, int[] toFill) {
                // do nothing
            }
        };

        final TrimAndFilterPipeline.CollectingTrimmingMetricTransformer ctmt =
                new TrimAndFilterPipeline.CollectingTrimmingMetricTransformer(anonymous);
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

        // test that no FT tags in the reads
        Assert.assertNull(notFilterRead.getAttributeAsString("FT"));
        Assert.assertNull(filterRead.getAttributeAsString("FT"));

        // test filtering and the metrics
        testFilterMetric(cfmf.metric, filterName, 0, 0);
        Assert.assertFalse(cfmf.test(filterRead));
        testFilterMetric(cfmf.metric, filterName, 1, 0);
        Assert.assertTrue(cfmf.test(notFilterRead));
        testFilterMetric(cfmf.metric, filterName, 2, 1);

        // test FT tags update
        Assert.assertNull(notFilterRead.getAttributeAsString("FT"));
        Assert.assertEquals(filterRead.getAttributeAsString("FT"), filterName);

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
    private void testTrimmingMetric(final TrimmerMetric metric,
            final String trimmerName, final int total,
            final int trimmed5p, final int trimmed3p, final int completelyTrim) {
        Assert.assertEquals(metric.TRIMMER, trimmerName, "TrimmerMetric.TRIMMER");
        Assert.assertEquals(metric.TOTAL, total, "TrimmerMetric.TOTAL");
        Assert.assertEquals(metric.TRIMMED_5_P, trimmed5p, "TrimmerMetric.TRIMMED_5_P");
        Assert.assertEquals(metric.TRIMMED_3_P, trimmed3p, "TrimmerMetric.TRIMMED_3_P");
        Assert.assertEquals(metric.TRIMMED_COMPLETE, completelyTrim,
                "TrimmerMetric.TRIMMED_COMPLETE");
    }

    @Test(expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testGetEmptyPipelineBlowsUp() throws Exception {
        // empty argument collection
        TrimAndFilterPipeline.fromPluginDescriptors(
                new TrimmerPluginDescriptor(null),
                new GATKReadFilterPluginDescriptor(null));
    }

    @DataProvider(name = "mutexArgs")
    public Object[][] getMutexArgs() {
        return new Object[][] {
                {false, false},
                {true, false},
                {false, true}
        };
    }

    @DataProvider(name = "trimmersAndFilters")
    public Object[][] getTrimmersAndFilter() throws Exception {
        return new Object[][] {
                {Collections.singletonList(new TrailingNtrimmer()),
                        Collections.emptyList()},
                {Collections.singletonList(new TrailingNtrimmer()),
                        Collections.singletonList(ReadFilterLibrary.MAPPED)},
                {Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Collections.emptyList()},
                {Arrays.asList(new TrailingNtrimmer(), new CutReadTrimmer(1, 1)),
                        Collections.singletonList(ReadFilterLibrary.GOOD_CIGAR)},
                {Collections.singletonList(new TrailingNtrimmer()),
                        Arrays.asList(ReadFilterLibrary.GOOD_CIGAR, ReadFilterLibrary.MAPPED)}
        };
    }

    @Test(dataProvider = "trimmersAndFilters")
    public void testGetPipeline(final List<TrimmingFunction> defaultTrimmers,
            final List<ReadFilter> userFilters) throws Exception {
        // set up the GATKReadFilterPluginDescriptor -> defaults null because they does not matter
        final GATKReadFilterPluginDescriptor filterDescriptor =
                new GATKReadFilterPluginDescriptor(null);
        // this is like parsing the arguments with Barclay
        userFilters.stream().map(ReadFilter::getClass).forEach(rf -> {
            filterDescriptor.userEnabledReadFilterNames.add(rf.getSimpleName());
            try {
                filterDescriptor.getInstance(rf);
            } catch (IllegalAccessException | InstantiationException e) {
                Assert.fail(e.getMessage());
            }
        });

        // get the trimming pipeline arguments
        final TrimAndFilterPipeline pipeline = TrimAndFilterPipeline.fromPluginDescriptors(
                new TrimmerPluginDescriptor(defaultTrimmers), filterDescriptor);

        // check that the pipeline contains the same number of trimmers
        Assert.assertEquals(pipeline.getTrimmingStats().size(), defaultTrimmers.size());
        // and the same number of filters + 1 (completely trimmed)
        Assert.assertEquals(pipeline.getFilterStats().size(), userFilters.size() + 1);
    }

    @Test(dataProvider = "trimmersAndFilters")
    public void testGetPipelineCompatibleWithGATKReadFilterPluginDescriptor(
            final List<TrimmingFunction> defaultTrimmers,
            final List<ReadFilter> defaultFilters) throws Exception {

        // get the trimming pipeline arguments
        final TrimAndFilterPipeline pipeline = TrimAndFilterPipeline.fromPluginDescriptors(
                new TrimmerPluginDescriptor(defaultTrimmers),
                new GATKReadFilterPluginDescriptor(defaultFilters));

        // check that the pipeline contains the same number of trimmers
        Assert.assertEquals(pipeline.getTrimmingStats().size(), defaultTrimmers.size());
        // and the same number of filters + 1 (completely trimmed)
        Assert.assertEquals(pipeline.getFilterStats().size(), defaultFilters.size() + 1);
    }

    @Test(dataProvider = "trimmersAndFilters")
    public void testGetPipelineCompatibleWithGATKReadFilterPluginDescriptorWithUserDuplicated(
            final List<TrimmingFunction> defaultTrimmers,
            final List<ReadFilter> defaultFilters) throws Exception {

        final CommandLineParser clp = new CommandLineArgumentParser(new Object(), Arrays.asList(
                new GATKReadFilterPluginDescriptor(defaultFilters),
                new TrimmerPluginDescriptor(defaultTrimmers)));

        if (defaultFilters.isEmpty()) {
            clp.parseArguments(NULL_PRINT_STREAM, new String[]{});
        } else {
            clp.parseArguments(NULL_PRINT_STREAM, new String[] {"--RF",
                    defaultFilters.get(0).getClass().getSimpleName()});
        }

        // get the trimming pipeline arguments
        final TrimAndFilterPipeline pipeline = TrimAndFilterPipeline.fromPluginDescriptors(
                clp.getPluginDescriptor(TrimmerPluginDescriptor.class),
                clp.getPluginDescriptor(GATKReadFilterPluginDescriptor.class));

        // check that the pipeline contains the same number of trimmers
        Assert.assertEquals(pipeline.getTrimmingStats().size(), defaultTrimmers.size());
        // and the same number of filters + 1 (completely trimmed)
        Assert.assertEquals(pipeline.getFilterStats().size(), defaultFilters.size() + 1);
    }

    @Test(dataProvider = "trimmersAndFilters")
    public void testGetPipelineCompatibleWithGATKReadFilterPluginDescriptorDisableAll(
            final List<TrimmingFunction> defaultTrimmers,
            final List<ReadFilter> defaultFilters) throws Exception {

        final CommandLineParser clp = new CommandLineArgumentParser(new Object(), Arrays.asList(
                new GATKReadFilterPluginDescriptor(defaultFilters),
                new TrimmerPluginDescriptor(defaultTrimmers)));

        final int expectedFilters;
        if (defaultFilters.isEmpty()) {
            // parse arguments with disabling all the read filters
            clp.parseArguments(NULL_PRINT_STREAM, new String[] {"--disableToolDefaultReadFilters"});
            // we only expect the completely trimmed one
            expectedFilters = 1;
        } else {
            // parse arguments with disabling all the read filters and adding the first one
            clp.parseArguments(NULL_PRINT_STREAM, new String[] {
                    "--disableToolDefaultReadFilters",
                    "--readFilter", defaultFilters.get(0).getClass().getSimpleName()});
            // in this case we expect two filters, the first one and the completely trimmed
            expectedFilters = 2;
        }

        // get the trimming pipeline arguments
        final TrimAndFilterPipeline pipeline = TrimAndFilterPipeline.fromPluginDescriptors(
                clp.getPluginDescriptor(TrimmerPluginDescriptor.class),
                clp.getPluginDescriptor(GATKReadFilterPluginDescriptor.class));

        // check that the pipeline contains the same number of trimmers
        Assert.assertEquals(pipeline.getTrimmingStats().size(), defaultTrimmers.size());
        // and the filters
        Assert.assertEquals(pipeline.getFilterStats().size(), expectedFilters);
    }

    @Test
    public void testFilterTagApplyForFirst() throws Exception {
        // pipeline only with read filter
        final TrimAndFilterPipeline pipeline = new TrimAndFilterPipeline(
                Collections.emptyList(),
                Collections.singletonList(new ReadLengthReadFilter(0, 10)));

        // only 2 bases, filter by the read filter
        final GATKRead read = ArtificialReadUtils.createArtificialRead("2M");
        // set the completely trim read -> filtered by the complete read filter
        read.setAttribute("ct", "1");

        // in this case we would like to have in the FT tag the first tag only the first filter
        // so in this case is the CompletelyTrimReadFilter
        Assert.assertFalse(pipeline.test(read));
        Assert.assertEquals(read.getAttributeAsString("FT"), "CompletelyTrimReadFilter");
    }

}