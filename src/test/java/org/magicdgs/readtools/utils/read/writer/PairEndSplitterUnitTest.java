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

package org.magicdgs.readtools.utils.read.writer;


import org.magicdgs.readtools.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class PairEndSplitterUnitTest extends BaseTest {

    private final static PairEndSplitter SPLITTER = new PairEndSplitter();

    @Test
    public void testGetSplitsByFromHeader() throws Exception {
        final List<String> expected = Arrays.asList("1", "2", "SE");
        // with header
        Assert.assertEquals(SPLITTER.getSplitsBy(new SAMFileHeader()), expected);
        // with null header
        Assert.assertEquals(SPLITTER.getSplitsBy(null), expected);
    }

    @DataProvider(name = "reads")
    public Object[][] readsData() {
        final SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeader();
        final GATKRead singleEnd =
                ArtificialReadUtils.createArtificialUnmappedRead(header, new byte[0], new byte[0]);
        final List<GATKRead> pair =
                ArtificialReadUtils.createPair(header, "name", 10, 2, 2, true, true);
        return new Object[][] {
                {singleEnd, "SE"},
                {pair.get(0), "1"},
                {pair.get(1), "2"}
        };
    }

    @Test(dataProvider = "reads")
    public void testGetSplitByFromRead(final GATKRead read, final String expected)
            throws Exception {
        Assert.assertEquals(SPLITTER.getSplitBy(read, null), expected);
    }
}