package org.magicdgs.utils;

import htsjdk.samtools.SAMUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingUtilTest {

    @Test
    public void trimPointsMott() throws Exception {
        byte[] testQuals = SAMUtils.fastqToPhred("555566");
        // no trim
        Assert.assertArrayEquals(new int[] {0, testQuals.length},
                TrimmingUtil.trimPointsMott(testQuals, 19));
        // trim one end
        Assert.assertArrayEquals(new int[] {4, testQuals.length},
                TrimmingUtil.trimPointsMott(testQuals, 20));
        // trim the other end
        testQuals = SAMUtils.fastqToPhred("665555");
        Assert.assertArrayEquals(new int[] {0, 2},
                TrimmingUtil.trimPointsMott(testQuals, 20));
        // trim both ends
        testQuals = SAMUtils.fastqToPhred("55665555");
        Assert.assertArrayEquals(new int[] {2, 4},
                TrimmingUtil.trimPointsMott(testQuals, 20));
        // trim all
        testQuals = SAMUtils.fastqToPhred("555555");
        Assert.assertArrayEquals(new int[] {testQuals.length, testQuals.length},
                TrimmingUtil.trimPointsMott(testQuals, 20));
    }

}