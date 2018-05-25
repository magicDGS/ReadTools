/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.utils.read.stats.singlestat;

import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ContainSoftclipCounterUnitTest extends RTBaseTest {
    private static final ContainSoftclipCounter STAT = new ContainSoftclipCounter();

    @Test
    public void testComputeLeftSoftclipRead() throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialRead("1S2M");
        Assert.assertEquals(STAT.compute(read).intValue(), 1);
    }

    @Test
    public void testComputeRightSoftclipRead() throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialRead("2M1S");
        Assert.assertEquals(STAT.compute(read).intValue(), 1);
    }

    @Test
    public void testComputeNoSoftclipRead() throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialRead("3M");
        Assert.assertEquals(STAT.compute(read).intValue(), 0);
    }
}