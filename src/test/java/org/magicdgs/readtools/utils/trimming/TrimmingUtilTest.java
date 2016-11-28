/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingUtilTest extends BaseTest {

    @DataProvider(name = "trimMottData")
    public static Object[][] trimMottData() {
        final byte[] quals1 = SAMUtils.fastqToPhred("555566");
        final byte[] quals2 = SAMUtils.fastqToPhred("665555");
        final byte[] quals3 = SAMUtils.fastqToPhred("55665555");
        final byte[] quals4 = SAMUtils.fastqToPhred("555555");
        return new Object[][] {
                // no trim
                {quals1, 19, new int[] {0, quals1.length}},
                {quals2, 19, new int[] {0, quals2.length}},
                {quals3, 19, new int[] {0, quals3.length}},
                {quals4, 19, new int[] {0, quals4.length}},
                // trim one end or the other
                {quals1, 20, new int[] {4, quals1.length}},
                {quals2, 20, new int[] {0, 2}},
                // trim both ends
                {quals3, 20, new int[] {2, 4}},
                {quals4, 20, new int[] {quals4.length, quals4.length}}
        };
    }

    @Test(dataProvider = "trimMottData")
    public void testTrimPointsMott(final byte[] quals, final int threshold, final int[] expected) {
        Assert.assertEquals(TrimmingUtil.trimPointsMott(quals, threshold), expected);
    }

    @DataProvider(name = "trimTrailingNdata")
    public static Object[][] trimTrailingNdata() {
        return new Object[][] {
                // not trimming because there is no N
                {new byte[] {'A', 'T', 'T', 'G', 'C', 'T'}, new int[] {0, 6}},
                // not trimming internal Ns
                {new byte[] {'A', 'T', 'T', 'G', 'N', 'T'}, new int[] {0, 6}},
                // trimming last bases
                {new byte[] {'A', 'T', 'T', 'G', 'C', 'N'}, new int[] {0, 5}},
                {new byte[] {'A', 'T', 'T', 'G', 'N', 'N'}, new int[] {0, 4}},
                {new byte[] {'A', 'T', 'N', 'G', 'N', 'N'}, new int[] {0, 4}},
                // trimming first bases
                {new byte[] {'N', 'T', 'T', 'G', 'C', 'T'}, new int[] {1, 6}},
                {new byte[] {'N', 'N', 'T', 'G', 'C', 'T'}, new int[] {2, 6}},
                {new byte[] {'N', 'N', 'T', 'N', 'C', 'T'}, new int[] {2, 6}},
                // trimming both ends
                {new byte[] {'N', 'T', 'T', 'G', 'C', 'N'}, new int[] {1, 5}},
                {new byte[] {'N', 'N', 'T', 'G', 'C', 'N'}, new int[] {2, 5}},
                {new byte[] {'N', 'N', 'T', 'N', 'C', 'N'}, new int[] {2, 5}},
                {new byte[] {'N', 'T', 'T', 'G', 'N', 'N'}, new int[] {1, 4}},
                {new byte[] {'N', 'N', 'T', 'G', 'N', 'N'}, new int[] {2, 4}},
                // trimming all
                {new byte[] {'N', 'N', 'N', 'N', 'N', 'N'}, new int[] {6, 6}}
        };
    }

    @Test(dataProvider = "trimTrailingNdata")
    public void testTrimPointsTrailingNs(final byte[] bases, final int[] expected)
            throws Exception {
        Assert.assertEquals(TrimmingUtil.trimPointsTrailingNs(bases), expected);
    }

}