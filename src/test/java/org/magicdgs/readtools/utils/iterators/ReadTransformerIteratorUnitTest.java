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

package org.magicdgs.readtools.utils.iterators;

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.CigarOperator;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadTransformerIteratorUnitTest extends BaseTest {

    private Iterator<GATKRead> makeReadsIterator() {
        return Arrays.asList(
                ArtificialReadUtils.createArtificialRead("10M"),
                ArtificialReadUtils.createArtificialRead("20M"),
                ArtificialReadUtils.createArtificialRead("60M"),
                ArtificialReadUtils.createArtificialRead("1M"),
                ArtificialReadUtils.createArtificialRead("1M")
        ).iterator();
    }

    @DataProvider(name = "iteratorTestData")
    public Object[][] getTestData() {
        return new Object[][] {
                {makeReadsIterator(),
                        (ReadTransformer) read -> {
                            read.setName("name");
                            return read;
                        },
                        (Predicate<GATKRead>) read -> read.getName().equals("name")
                },
                {makeReadsIterator(),
                        (ReadTransformer) read -> {
                            read.setFragmentLength(1000);
                            return read;
                        },
                        (Predicate<GATKRead>) read -> read.getFragmentLength() == 1000},
                {makeReadsIterator(),
                        (ReadTransformer) read -> {
                            read.setCigar("100M");
                            return read;
                        },
                        (Predicate<GATKRead>) read ->
                                read.getCigar().getReadLength() == 100
                                        && read.getCigar().getCigarElement(0).getOperator()
                                        == CigarOperator.M}
        };
    }

    @Test(dataProvider = "iteratorTestData")
    public void testIterator(final Iterator<GATKRead> readIterator,
            final ReadTransformer transformer, final Predicate<GATKRead> test) throws Exception {
        final ReadTransformerIterator iterator =
                new ReadTransformerIterator(readIterator, transformer);
        for (final GATKRead read : iterator) {
            Assert.assertTrue(test.test(read));
        }
    }
}