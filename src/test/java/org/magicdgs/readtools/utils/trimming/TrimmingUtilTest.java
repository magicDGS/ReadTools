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

package org.magicdgs.readtools.utils.trimming;

import org.magicdgs.readtools.RTBaseTest;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingUtilTest extends RTBaseTest {

    @DataProvider(name = "trimMottData")
    public static Object[][] trimMottData() {
        return new Object[][] {
                // this are test which mimics the ones in the perl script from PoPoolation
                {new byte[] {20, 20, 20, 20}, 20, new int[] {4, 0}},
                {new byte[] {21, 21, 21, 20, 19}, 20, new int[] {0, 3}},
                {new byte[] {20, 20, 21, 21, 21, 21, 20, 20}, 20, new int[] {2, 6}},
                {new byte[] {19, 19, 21, 21, 21, 21, 1, 1, 21, 21, 21, 21, 21}, 20,
                        new int[] {8, 13}},
                {new byte[] {19, 19, 21, 21, 21, 21, 1, 1, 21, 21, 21, 21, 21, 19}, 20,
                        new int[] {8, 13}},
                {new byte[] {19, 19, 21, 21, 21, 21, 21, 1, 1, 21, 21, 21, 21}, 20,
                        new int[] {2, 7}},
                {new byte[] {19, 19, 21, 21, 21, 20, 20, 19, 19, 21, 21, 21, 20}, 20,
                        new int[] {2, 12}},
                {new byte[] {19, 21, 21, 21, 20, 20, 19, 19, 21, 21, 21, 20, 19}, 20,
                        new int[] {1, 11}},
                {new byte[] {19, 21, 21, 21, 20, 20, 19, 19, 21, 21, 21, 20, 19, 19, 21, 20, 19},
                        20, new int[] {1, 11}},
                {new byte[] {21, 20, 20, 19, 19, 21, 21, 21, 20, 20, 19, 19, 21, 21, 21, 20, 19},
                        20, new int[] {5, 15}}


        };
    }

    @Test(dataProvider = "trimMottData")
    public void testTrimPointsMott(final byte[] quals, final int threshold, final int[] expected) {
        Assert.assertEquals(TrimmingUtil.trimPointsMott(quals, threshold), expected);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTrimPointsMottNullQuals() {
        TrimmingUtil.trimPointsMott(null, 1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTrimPointsMottNegativeThreshold() {
        TrimmingUtil.trimPointsMott(new byte[] {20, 20}, -1);
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

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTrimPoitnTrailingNsNullBases() {
        TrimmingUtil.trimPointsTrailingNs(null);
    }

}