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

package org.magicdgs.readtools.utils.read.transformer.barcodes;

import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FixReadNameBarcodesReadTransformerUnitTest extends RTBaseTest {

    private static final GATKRead READ = ArtificialReadUtils.createArtificialUnmappedRead(
            ArtificialReadUtils.createArtificialSamHeader(),
            new byte[0], new byte[0]
    );

    static {
        READ.setName("read1");
    }

    private static final ReadTransformer TRANSFORMER = new FixReadNameBarcodesReadTransformer();

    @Test
    public void testUpdatedRead() {
        final GATKRead toTest = READ.deepCopy();
        final String barcode = "ACTG";
        toTest.setName(READ.getName() + "#" + barcode);
        TRANSFORMER.apply(toTest);
        Assert.assertNotEquals(toTest.getAttributeAsString("BC"), READ.getAttributeAsString("BC"));
        Assert.assertEquals(toTest.getAttributeAsString("BC"), barcode);
        // this is a check to be aware that this is removing the barcode from the name
        Assert.assertEquals(toTest.getName(), READ.getName());
    }

    @Test
    public void testNotUpdatedRead() {
        final GATKRead toTest = READ.deepCopy();
        TRANSFORMER.apply(toTest);
        // this should represent exactly the same
        Assert.assertEquals(toTest.convertToSAMRecord(null), READ.convertToSAMRecord(null));
    }

}