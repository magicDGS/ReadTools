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

package org.magicdgs.readtools.utils.read.transformer.barcodes;

import org.magicdgs.readtools.utils.tests.BaseTest;

import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FixRawBarcodeTagsReadTransformerUnitTest extends BaseTest {

    private static final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(
            ArtificialReadUtils.createArtificialSamHeader(),
            new byte[0], new byte[0]
    );

    static {
        read.setName("read1");
    }

    @DataProvider
    public Object[][] wrongBasicConstructor() {
        return new Object[][]{{null}, {Collections.emptyList()}};
    }

    @Test(dataProvider = "wrongBasicConstructor", expectedExceptions = IllegalArgumentException.class)
    public void testWrongBasicConstructor(final List<String> tags) {
        new FixRawBarcodeTagsReadTransformer(tags);
    }

    @DataProvider
    public Object[][] wrongConstructorWithQuals() {
        return new Object[][]{
                {null, null},
                {Arrays.asList("RT"), null},
                {Collections.emptyList(), null},
                {Arrays.asList("RT"), Collections.emptyList()},
                {Arrays.asList("BC", "B2"), Arrays.asList("QT")},
                {Arrays.asList("BC"), Arrays.asList("QT", "Q2")}
        };
    }

    @Test(dataProvider = "wrongConstructorWithQuals", expectedExceptions = IllegalArgumentException.class)
    public void testWrongConstructorWithQuals(final List<String> tags, final List<String> quals) {
        new FixRawBarcodeTagsReadTransformer(tags, quals);
    }

    @Test
    public void testOnlyBcUpdate() {
        final String rtTag = "RT";
        final ReadTransformer transformer = new FixRawBarcodeTagsReadTransformer(Arrays.asList(rtTag));
        final GATKRead toTest = read.deepCopy();
        final String barcode = "ACTG";
        toTest.setAttribute(rtTag, barcode);
        Assert.assertNull(toTest.getAttributeAsString("BC"));
        transformer.apply(toTest);
        // updated barcode
        Assert.assertEquals(toTest.getAttributeAsString("BC"), barcode);
        // null previous
        Assert.assertNull(toTest.getAttributeAsString(rtTag));
    }

    @Test
    public void testWithQualitiesUpdate() {
        final String b2Tag = "B2";
        final String q2Tag = "Q2";
        final ReadTransformer transformer =
                new FixRawBarcodeTagsReadTransformer(Arrays.asList(b2Tag), Arrays.asList(q2Tag));
        final GATKRead toTest = read.deepCopy();
        final String barcode = "ACTG";
        final String quality = "####";
        toTest.setAttribute(b2Tag, barcode);
        toTest.setAttribute(q2Tag, quality);
        Assert.assertNull(toTest.getAttributeAsString("BC"));
        Assert.assertNull(toTest.getAttributeAsString("QT"));
        transformer.apply(toTest);
        // updated barcode
        Assert.assertEquals(toTest.getAttributeAsString("BC"), barcode);
        Assert.assertEquals(toTest.getAttributeAsString("QT"), quality);
        // null previous
        Assert.assertNull(toTest.getAttributeAsString(b2Tag));
        Assert.assertNull(toTest.getAttributeAsString(q2Tag));
    }

}