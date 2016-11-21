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

package org.magicdgs.readtools.utils.read;

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTReadUtilsUnitTest extends BaseTest {

    private static final SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeader();
    private static final List<String> twoTags = Arrays.asList("B1", "B2");


    @DataProvider(name = "barcodesData")
    public Iterator<Object[]> getBarcodeData() {
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        final List<Object[]> data = new ArrayList<>();
        for (final String barcode : new String[] {"AAAA", "TTTT", "CCCC"}) {
            final String baseReadName = UUID.randomUUID().toString() + "#" + barcode;
            read.setName(baseReadName);
            read.setAttribute(twoTags.get(0), barcode);
            data.add(new Object[] {new String[] {barcode}, read.deepCopy(),
                    twoTags.subList(0, 1)});
            for (final String barcode2 : new String[] {"ACTG", "GGGG"}) {
                read.setName(baseReadName + "-" + barcode2);
                read.setAttribute(twoTags.get(0), barcode);
                read.setAttribute(twoTags.get(1), barcode2);
                data.add(new Object[] {new String[] {barcode, barcode2}, read.deepCopy(),
                        twoTags});
                read.setAttribute(twoTags.get(0), barcode + "-" + barcode2);
                read.setAttribute(twoTags.get(1), (String) null);
                data.add(new Object[] {new String[] {barcode, barcode2}, read.deepCopy(),
                        twoTags.subList(0, 1)});
            }
        }

        return data.iterator();
    }

    @Test(dataProvider = "barcodesData")
    public void testExtractBarcodesFromReadName(final String[] barcodes, final GATKRead read,
            final List<String> tags) throws Exception {
        Assert.assertNotEquals(read.getName().indexOf('#'), -1);
        Assert.assertEquals(RTReadUtils.extractBarcodesFromReadName(read), barcodes);
        Assert.assertEquals(read.getName().indexOf('#'), -1);
    }

    @Test(dataProvider = "barcodesData")
    public void testGetBarcodesFromTags(final String[] barcodes, final GATKRead read,
            final List<String> tags) throws Exception {
        Assert.assertEquals(RTReadUtils.getBarcodesFromTags(read, tags), barcodes);
    }

    @Test(dataProvider = "barcodesData")
    public void testAddBarcodesTagToRead(final String[] barcodes, final GATKRead read,
            final List<String> tags) throws Exception {
        RTReadUtils.addBarcodesTagToRead(read, barcodes);
        Assert.assertTrue(read.hasAttribute("BC"));
        Assert.assertEquals(read.getAttributeAsString("BC"), String.join("-", barcodes));
    }

    @Test
    public void testNoBarcodeData() throws Exception {
        final String[] emptyBarcodes = new String[0];
        // this test if all the methods that returns an empty array if there is no barcode data
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        read.setName("read1");
        final String[] fromName = RTReadUtils.extractBarcodesFromReadName(read);
        Assert.assertEquals(fromName, emptyBarcodes);
        final String[] fromTags = RTReadUtils.getBarcodesFromTags(read, twoTags);
        Assert.assertEquals(fromTags, emptyBarcodes);
        // this test if all the methods does not update the read if no data is provided
        RTReadUtils.addBarcodesTagToRead(read, emptyBarcodes);
        Assert.assertNull(read.getAttributeAsString("BC"));
    }

    @Test
    public void testGetRawBarcodes() throws Exception {
        final String barcode = "ACTG";
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        read.setAttribute("BC", barcode);
        Assert.assertEquals(RTReadUtils.getRawBarcodes(read), new String[] {barcode});
    }

}