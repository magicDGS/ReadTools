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

package org.magicdgs.readtools.utils.read.filter;

import org.magicdgs.readtools.utils.tests.BaseTest;

import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class CompletelyTrimFilterUnitTest extends BaseTest {

    @DataProvider(name  = "reads")
    public Object[][] toFilterReads() {
        final GATKRead filtered1 = ArtificialReadUtils.createArtificialRead("4M");
        filtered1.setAttribute("ct", 1);
        final GATKRead filtered2 = ArtificialReadUtils.createArtificialRead("4M");
        filtered2.setAttribute("ts", 4);
        final GATKRead filtered3 = ArtificialReadUtils.createArtificialRead("4M");
        filtered3.setAttribute("te", 0);
        final GATKRead unfiltered1 = ArtificialReadUtils.createArtificialRead("4M");
        final GATKRead unfiltered2 = ArtificialReadUtils.createArtificialRead("4M");
        unfiltered2.setAttribute("ts", 1);
        final GATKRead unfiltered3 = ArtificialReadUtils.createArtificialRead("1M");
        unfiltered3.setAttribute("te", 1);
        return new Object[][] {
                {filtered1, false},
                {filtered2, false},
                {filtered3, false},
                {unfiltered1, true},
                {unfiltered2, true},
                {unfiltered3, true}
        };
    }

    private final static ReadFilter filter = new CompletelyTrimReadFilter();

    @Test(dataProvider = "reads")
    public void testFilter(final GATKRead read, final boolean pass) {
        Assert.assertEquals(filter.test(read), pass);
    }

}