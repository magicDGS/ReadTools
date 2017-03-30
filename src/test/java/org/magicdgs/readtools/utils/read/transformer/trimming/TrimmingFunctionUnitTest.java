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

import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.function.Consumer;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimmingFunctionUnitTest extends RTBaseTest {

    /** Use for testing methods with trimming functions that does not require an implementation. */
    public static class NoOpTrimmingFunction extends TrimmingFunction {
        public static final long serialVersionUID = 1L;

        @Override
        protected void fillTrimPoints(GATKRead read, int[] toFill) {
            // do nothing
        }
    }

    /** Trim 1 base in each end of the read. */
    public static class OneBaseInEachEndTrimmingFunction extends TrimmingFunction {
        public static final long serialVersionUID = 1L;

        @Override
        protected void fillTrimPoints(final GATKRead read, final int[] toFill) {
            toFill[0] = 1;
            toFill[1] = read.getLength() - 1;
        }
    }

    // get a trimmer after calling a function
    private static TrimmingFunction getTrimmerAfterCalling(
            final Consumer<TrimmingFunction> toCall) {
        final TrimmingFunction trimmer = new NoOpTrimmingFunction();
        toCall.accept(trimmer);
        return trimmer;
    }

    @DataProvider(name = "alreadySet")
    public Object[][] getAlreadySetTrimmers() throws Exception {
        return new Object[][] {
                {getTrimmerAfterCalling(t -> t.setDisableEnds(true, false))},
                {getTrimmerAfterCalling(
                        t -> t.apply(ArtificialReadUtils.createArtificialRead("1M")))},
                {getTrimmerAfterCalling(TrimmingFunction::validateArgs)}
        };
    }

    @Test(dataProvider = "alreadySet", expectedExceptions = IllegalStateException.class)
    public void testSetDisableEndsIllegalState(final TrimmingFunction trimmer) throws Exception {
        trimmer.setDisableEnds(false, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetDisableWrongArguments() throws Exception {
        final TrimmingFunction trimmer = new NoOpTrimmingFunction();
        trimmer.setDisableEnds(true, true);
    }

    @DataProvider(name = "disableArguments")
    public Object[][] getCorrectDisableEndsParams() {
        return new Object[][] {
                {true, false},
                {false, true},
                {false, false}
        };
    }

    @Test(dataProvider = "disableArguments")
    public void testSetDisable(final boolean disable5p, final boolean disable3p) throws Exception {
        final TrimmingFunction trimmer = new NoOpTrimmingFunction();
        Assert.assertFalse(trimmer.isDisable5prime());
        Assert.assertFalse(trimmer.isDisable3prime());
        trimmer.setDisableEnds(disable5p, disable3p);
        Assert.assertEquals(trimmer.isDisable5prime(), disable5p);
        Assert.assertEquals(trimmer.isDisable3prime(), disable3p);
        Assert.assertThrows(IllegalStateException.class,
                () -> trimmer.setDisableEnds(disable5p, disable3p));
    }

    @Test(dataProvider = "disableArguments")
    public void testNoTrimmingEnds(final boolean disable5p, final boolean disable3p) {
        final GATKRead read = ArtificialReadUtils.createArtificialRead("2M");
        final TrimmingFunction fixed = new OneBaseInEachEndTrimmingFunction();
        fixed.setDisableEnds(disable5p, disable3p);
        fixed.apply(read);
        // if none of then is disabled, it is completely trimmed
        Assert.assertEquals(RTReadUtils.isCompletelyTrimRead(read), !(disable5p || disable3p));
        // test the trimming points
        Assert.assertEquals(read.getAttributeAsInteger("ts").intValue(), (disable5p) ? 0 : 1);
        Assert.assertEquals(read.getAttributeAsInteger("te").intValue(), (disable3p) ? 2 : 1);
    }

}