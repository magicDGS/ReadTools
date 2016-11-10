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
 * SOFTWARE.
 */

package org.magicdgs.readtools.utils;

import org.magicdgs.readtools.utils.tests.BaseTest;
import org.magicdgs.readtools.utils.trimming.TrimmingUtil;

import htsjdk.samtools.SAMUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingUtilTest extends BaseTest {

    @Test
    public void trimPointsMott() throws Exception {
        byte[] testQuals = SAMUtils.fastqToPhred("555566");
        // no trim
        Assert.assertEquals(TrimmingUtil.trimPointsMott(testQuals, 19),
                new int[] {0, testQuals.length});
        // trim one end
        Assert.assertEquals(TrimmingUtil.trimPointsMott(testQuals, 20),
                new int[] {4, testQuals.length});
        // trim the other end
        testQuals = SAMUtils.fastqToPhred("665555");
        Assert.assertEquals(TrimmingUtil.trimPointsMott(testQuals, 20),
                new int[] {0, 2});
        // trim both ends
        testQuals = SAMUtils.fastqToPhred("55665555");
        Assert.assertEquals(TrimmingUtil.trimPointsMott(testQuals, 20),
                new int[] {2, 4});
        // trim all
        testQuals = SAMUtils.fastqToPhred("555555");
        Assert.assertEquals(TrimmingUtil.trimPointsMott(testQuals, 20),
                new int[] {testQuals.length, testQuals.length});
    }

}