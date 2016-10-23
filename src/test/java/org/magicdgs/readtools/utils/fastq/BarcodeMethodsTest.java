/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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
 */
package org.magicdgs.readtools.utils.fastq;

import static org.magicdgs.readtools.utils.fastq.BarcodeMethods.getNameWithoutBarcode;
import static org.magicdgs.readtools.utils.fastq.BarcodeMethods.getOnlyBarcodeFromName;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BarcodeMethodsTest {

    static final String basename = "Record1";

    static final String barcode = "AAATTT";

    String nameWithBarcode;

    String nameWithBarcodeAndSeparator;

    String nameWithoutBarcode;

    @BeforeMethod
    public void setUp() throws Exception {
        nameWithBarcode = basename + "#" + barcode;
        nameWithBarcodeAndSeparator = basename + "#" + barcode + "/1";
        nameWithoutBarcode = basename;
    }

    @Test
    public void testGetOnlyBarcodeFromName() throws Exception {
        Assert.assertEquals(getOnlyBarcodeFromName(nameWithBarcode), barcode);
        Assert.assertEquals(getOnlyBarcodeFromName(nameWithBarcodeAndSeparator), barcode);
        Assert.assertNull(getOnlyBarcodeFromName(nameWithoutBarcode));
    }

    @Test
    public void testGetNameWithoutBarcode() throws Exception {
        Assert.assertEquals(getNameWithoutBarcode(nameWithBarcode), basename);
        Assert.assertEquals(getNameWithoutBarcode(nameWithBarcodeAndSeparator), basename);
        Assert.assertEquals(getNameWithoutBarcode(nameWithoutBarcode), basename);
    }
}