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

package org.magicdgs.readtools.engine;

import org.magicdgs.readtools.RTCommandLineProgramTest;
import org.magicdgs.readtools.TestResourcesUtils;

import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadToolsWalkerUnitTest extends RTCommandLineProgramTest {

    // test class
    private static class TestWalker extends ReadToolsWalker {
        private int nReads = 0;

        @Override
        protected void apply(GATKRead read) {
            nReads++;
        }

    }

    @DataProvider(name = "arguments")
    public Object[][] walkerArguments() {
        return new Object[][] {
                {Arrays.asList("-I", TestResourcesUtils.getWalkthroughDataFile("standard.single_index.SE.sam").getAbsolutePath()), 103, false},
                {Arrays.asList("-I", TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.paired_1.fq").getAbsolutePath(),
                        "-I2", TestResourcesUtils.getWalkthroughDataFile("legacy.single_index.paired_2.fq").getAbsolutePath()), 206, true}
        };
    }

    @Test(dataProvider = "arguments")
    public void testReadToolsSimpleWalker(final List<String> args, final int expectedReads,
            final boolean isPaired) throws Exception {
        final TestWalker walker = new TestWalker();
        Assert.assertNull(walker.instanceMain(injectDefaultVerbosity(args).toArray(new String[0])));
        Assert.assertEquals(walker.isPaired(), isPaired);
        Assert.assertEquals(walker.nReads, expectedReads);
    }

}