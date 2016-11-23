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

package org.magicdgs.readtools.utils.read.filter;

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class NoAmbiguousSequenceReadFilterUnitTest extends BaseTest {

    private final static SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeader();

    @DataProvider(name = "readsToFilter")
    public Object[][] toFilterData() {
        return new Object[][] {
                {new byte[] {'A'}, true},
                {new byte[] {'A', 'C', 'T'}, true},
                {new byte[] {'N'}, false},
                {new byte[] {'A', 'N', 'T'}, false},
                {new byte[] {'N', 'C', 'T'}, false},
                {new byte[] {'A', 'C', 'N'}, false},
                {new byte[] {'A', 'n', 'T'}, false},
                {new byte[] {'n', 'C', 'T'}, false},
                {new byte[] {'A', 'C', 'n'}, false}
        };
    }

    private final static ReadFilter filter = new NoAmbiguousSequenceReadFilter();

    @Test(dataProvider = "readsToFilter")
    public void testFilter(final byte[] bases, final boolean pass) {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(header,
                bases, Utils.dupBytes((byte) 'I', bases.length));
        Assert.assertEquals(filter.test(read), pass);
    }
}