package org.magicdgs.readtools.tools.mappability.gem;

import org.magicdgs.readtools.RTBaseTest;

import org.apache.commons.lang3.Range;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GemScoreMethodUnitTest extends RTBaseTest {

    private static final Long MIN_VALUE = 1L;
    private static final String MIN_STRING = "1";
    private static final Double MID_VALUE = 50.5;
    private static final String MID_STRING = "50.5";
    private static final Long MAX_VALUE = 100L;
    private static final String MAX_STRING = "100";

    @DataProvider
    public Object[][] scoreData() {
        return new Object[][] {
                // test a single value always returns the same value
                {GemScoreMethod.MIN, Range.is(MAX_VALUE), MAX_VALUE},
                {GemScoreMethod.MAX, Range.is(MAX_VALUE), MAX_VALUE},
                {GemScoreMethod.MID, Range.is(MAX_VALUE), MAX_VALUE.doubleValue()},
                // a range of values
                {GemScoreMethod.MIN, Range.between(MIN_VALUE, MAX_VALUE), MIN_VALUE},
                {GemScoreMethod.MAX, Range.between(MIN_VALUE, MAX_VALUE), MAX_VALUE},
                {GemScoreMethod.MID, Range.between(MIN_VALUE, MAX_VALUE), MID_VALUE},
        };
    }

    @Test(dataProvider = "scoreData")
    public void testScore(final GemScoreMethod method, final Range<Long> mappability,
            final Number expected) {
        Assert.assertEquals(method.score(mappability), expected);
    }

    @DataProvider
    public static Object[][] formatScoreData() {
        return new Object[][] {
                // test a single value always returns the same value
                {GemScoreMethod.MIN, Range.is(MAX_VALUE), MAX_STRING},
                {GemScoreMethod.MAX, Range.is(MAX_VALUE), MAX_STRING},
                {GemScoreMethod.MID, Range.is(MAX_VALUE), MAX_STRING},
                // a range of values
                {GemScoreMethod.MIN, Range.between(MIN_VALUE, MAX_VALUE), MIN_STRING},
                {GemScoreMethod.MAX, Range.between(MIN_VALUE, MAX_VALUE), MAX_STRING},
                {GemScoreMethod.MID, Range.between(MIN_VALUE, MAX_VALUE), MID_STRING},
        };
    }

    @Test(dataProvider = "formatScoreData")
    public void testFormatScore(final GemScoreMethod method, final Range<Long> mappability,
            final String expected) {
        Assert.assertEquals(method.formatScore(mappability), expected);
    }
}