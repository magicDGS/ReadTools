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

import org.magicdgs.readtools.RTBaseTest;
import org.magicdgs.readtools.utils.trimming.TrimmingUtilTest;

import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class MottQualityTrimmerUnitTest extends RTBaseTest {

    @DataProvider(name = "badArgs")
    public Object[][] badQualityThresholds() {
        return new Object[][] {
                {-1}, {-2}, {-3}
        };
    }

    @Test(dataProvider = "badArgs", expectedExceptions = IllegalArgumentException.class)
    public void testIllegalArg(final int badQualThreshold) throws Exception {
        new MottQualityTrimmer(badQualThreshold);
    }

    @Test(dataProvider = "badArgs")
    public void testFailValidation(final int badQualThreshold) throws Exception {
        final MottQualityTrimmer trimmer = new MottQualityTrimmer();
        // this should not thrown
        trimmer.validateArgs();
        // a bad quality threshold after construction should thrown
        trimmer.qualThreshold = badQualThreshold;
        Assert.assertThrows(CommandLineException.BadArgumentValue.class, trimmer::validateArgs);
    }

    @Test(dataProvider = "trimMottData", dataProviderClass = TrimmingUtilTest.class)
    public void testTrimmer(final byte[] quals, final int threshold, final int[] expected)
            throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialRead(
                Utils.repeatBytes((byte) 'A', quals.length), quals, quals.length + "M");
        final TrimmingFunction trimmingFunction = new MottQualityTrimmer(threshold);
        trimmingFunction.apply(read);
        Assert.assertEquals(read.getAttributeAsInteger("ts").intValue(), expected[0]);
        Assert.assertEquals(read.getAttributeAsInteger("te").intValue(), expected[1]);
    }

}