package org.broadinstitute.hellbender.engine.filters;

import org.testng.Assert;

// TODO: this is a hack for testing the custom GATK-plugin
public final class ReadFilterUnitTest {

    public static int verifyAndFilterOrder(final ReadFilter rf, final String[] expectedOrder) {
        Assert.assertEquals(rf.getClass(), ReadFilter.ReadFilterAnd.class);
        return verifyAndFilterOrder((ReadFilter.ReadFilterAnd) rf, expectedOrder);
    }

    public static int verifyAndFilterOrder(final ReadFilter.ReadFilterAnd rf, final String[] expectedOrder) {
        if (rf.lhs instanceof ReadFilter.ReadFilterAnd) {
            int count = verifyAndFilterOrder((ReadFilter.ReadFilterAnd) rf.lhs, expectedOrder);
            Assert.assertEquals(expectedOrder[count], rf.rhs.getClass().getSimpleName());
            return ++count;
        } else {
            int count = 0;
            Assert.assertEquals(expectedOrder[count], rf.lhs.getClass().getSimpleName());
            count++;
            Assert.assertEquals(expectedOrder[count], rf.rhs.getClass().getSimpleName());
            count++;
            return count;
        }
    }

}