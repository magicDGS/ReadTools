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

import org.magicdgs.readtools.utils.read.RTReadUtils;
import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class CutReadTrimmerUnitTest extends RTBaseTest {

    @DataProvider
    public Object[][] badArgs() {
        return new Object[][] {
                {0, 0},
                {0, -1},
                {-1, 0},
        };
    }

    @Test(dataProvider = "badArgs", expectedExceptions = IllegalArgumentException.class)
    public void testFailConstructor(final int fivePrime, final int threePrime)
            throws Exception {
        new CutReadTrimmer(fivePrime, threePrime);
    }

    @Test(expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testFailedValidationOnDefaultConstructor() throws Exception {
        final CutReadTrimmer trimmer = new CutReadTrimmer();
        trimmer.validateArgs();
    }

    @DataProvider
    public Object[][] badDisableForArguments() {
        return new Object[][] {
                {1, 1, true, false},
                {1, 1, false, true}
        };
    }

    @Test(dataProvider = "badDisableForArguments", expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testFailedValidation(final int fivePrime, final int threePrime,
            final boolean disable5p, final boolean disable3p) {
        final CutReadTrimmer trimmer = new CutReadTrimmer(fivePrime, threePrime);
        trimmer.setDisableEnds(disable5p, disable3p);
        trimmer.validateArgs();
    }

    @DataProvider(name = "readLengthsToTrim")
    public Iterator<Object[]> getReadsLengthsToTrim() {
        final List<Object[]> data = new ArrayList<>(40);
        // this reads are completely trimmed in both sides
        for (int i = 1; i < 10; i++) {
            data.add(new Object[] {i, i, 0, true});
        }
        // this read lengths are trimmed always in the 5 prime 10 bases and completely in the other end
        for (int i = 10; i <= 20; i++) {
            data.add(new Object[] {i, 10, 0, true});
        }
        // this reads are trimmed in both end, and they are completely trimmed because they trim from both sides
        for (int i = 21; i <= 30; i++) {
            data.add(new Object[] {i, 10, i - 20, true});
        }
        // this reads are trimmed in both ends, but they are not completely trimmed
        for (int i = 31; i < 40; i++) {
            data.add(new Object[] {i, 10, i - 20, false});
        }
        return data.iterator();
    }

    @Test(dataProvider = "readLengthsToTrim")
    public void testTrimmer(final int readLength, final Integer expectedStart,
            final Integer expectedEnd, final boolean completelyTrim) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialRead(readLength + "M");
        final TrimmingFunction trimmer = new CutReadTrimmer(10, 20);
        trimmer.apply(read);
        Assert.assertEquals(read.getAttributeAsInteger("ts"), expectedStart, "wrong 'ts'");
        Assert.assertEquals(read.getAttributeAsInteger("te"), expectedEnd, "wrong 'te'");
        // this rely on our framework utils, which should be tested in other class
        // either one or the other side should be completely trimmed
        Assert.assertEquals(RTReadUtils.updateCompletelyTrimReadFlag(read), completelyTrim);
    }

    @Test(dataProvider = "readLengthsToTrim")
    public void testOnlyFivePrime(final int readLength, final Integer expectedStart,
            final Integer expectedEnd, final boolean completelyTrim) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialRead(readLength + "M");
        final TrimmingFunction trimmer = new CutReadTrimmer(10, 0);
        trimmer.apply(read);
        Assert.assertEquals(read.getAttributeAsInteger("ts"), expectedStart, "wrong 'ts'");
        Assert.assertEquals(read.getAttributeAsInteger("te").intValue(), readLength, "wrong 'te'");
        // this rely on our framework utils, which should be tested in other class
        Assert.assertEquals(RTReadUtils.updateCompletelyTrimReadFlag(read),
                completelyTrim && expectedStart == readLength);
    }

    @Test(dataProvider = "readLengthsToTrim")
    public void testOnlyThreePrime(final int readLength, final Integer expectedStart,
            final Integer expectedEnd, final boolean completelyTrim) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialRead(readLength + "M");
        final TrimmingFunction trimmer = new CutReadTrimmer(0, 20);
        trimmer.apply(read);
        Assert.assertEquals(read.getAttributeAsInteger("ts").intValue(), 0, "wrong 'ts'");
        Assert.assertEquals(read.getAttributeAsInteger("te"), expectedEnd, "wrong 'te'");
        // this rely on our framework utils, which should be tested in other class
        Assert.assertEquals(RTReadUtils.updateCompletelyTrimReadFlag(read),
                completelyTrim && expectedEnd == 0);
    }

}