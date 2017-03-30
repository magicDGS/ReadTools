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

package org.magicdgs.readtools.utils.read.transformer.trimming;

import org.magicdgs.readtools.BaseTest;
import org.magicdgs.readtools.utils.trimming.TrimmingUtilTest;

import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrailingNtrimmerUnitTest extends BaseTest {

    // trimmmer to test
    private final static TrimmingFunction TRIMMER = new TrailingNtrimmer();

    @Test(dataProvider = "trimTrailingNdata", dataProviderClass = TrimmingUtilTest.class)
    public void testTrimmer(final byte[] bases, final int[] expected) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialRead(bases,
                Utils.repeatBytes((byte) 'I', bases.length), bases.length + "M");
        TRIMMER.apply(read);
        Assert.assertEquals(read.getAttributeAsInteger("ts").intValue(), expected[0]);
        Assert.assertEquals(read.getAttributeAsInteger("te").intValue(), expected[1]);
    }

    @Test
    public void testValidateArgsNotThrown() throws Exception {
        TRIMMER.validateArgs();
    }

}