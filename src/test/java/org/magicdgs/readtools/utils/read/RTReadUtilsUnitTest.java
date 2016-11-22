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
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTReadUtilsUnitTest extends BaseTest {

    private static final SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeader();
    private static final List<String> twoTags = Arrays.asList("B1", "B2");
    private static final List<String> twoQualTags = Arrays.asList("Q1", "Q2");

    @DataProvider(name = "barcodesData")
    public Iterator<Object[]> getBarcodeData() {
        final String quality1 = "####";
        final String quality2 = "!#01";
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        final List<Object[]> data = new ArrayList<>();
        for (final String barcode : new String[] {"AAAA", "TTTT", "CCCC"}) {
            final String baseReadName = UUID.randomUUID().toString() + "#" + barcode;
            read.setName(baseReadName);
            read.setAttribute(twoTags.get(0), barcode);
            read.setAttribute(twoQualTags.get(0), quality1);
            data.add(new Object[] {new String[] {barcode}, read.deepCopy(),
                    twoTags.subList(0, 1), new String[]{quality1}, twoQualTags.subList(0, 1)});
            for (final String barcode2 : new String[] {"ACTG", "GGGG"}) {
                read.setName(baseReadName + "-" + barcode2);
                read.setAttribute(twoTags.get(0), barcode);
                read.setAttribute(twoTags.get(1), barcode2);
                read.setAttribute(twoQualTags.get(0), quality1);
                read.setAttribute(twoQualTags.get(1), quality2);
                data.add(new Object[] {new String[] {barcode, barcode2}, read.deepCopy(),
                        twoTags, new String[]{quality1, quality2}, twoQualTags});
                read.setAttribute(twoTags.get(0), barcode + "-" + barcode2);
                read.setAttribute(twoTags.get(1), (String) null);
                read.setAttribute(twoQualTags.get(0), quality1 + "-" + quality2);
                read.setAttribute(twoQualTags.get(1), (String) null);
                data.add(new Object[] {new String[] {barcode, barcode2}, read.deepCopy(),
                        twoTags.subList(0, 1), new String[]{quality1, quality2}, twoQualTags.subList(0, 1)});
            }
        }

        return data.iterator();
    }

    @Test(dataProvider = "barcodesData")
    public void testExtractBarcodesFromReadName(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags) throws Exception {
        Assert.assertNotEquals(read.getName().indexOf('#'), -1);
        Assert.assertEquals(RTReadUtils.extractBarcodesFromReadName(read), barcodes);
        Assert.assertEquals(read.getName().indexOf('#'), -1);
    }

    @Test(dataProvider = "barcodesData")
    public void testGetBarcodesFromTags(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags) throws Exception {
        Assert.assertEquals(RTReadUtils.getBarcodesFromTags(read, tags), barcodes);
    }

    @Test(dataProvider = "barcodesData")
    public void testGetBarcodesAndQualitiesFromTags(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags) throws Exception {
        final Pair<String[], String[]> result = RTReadUtils.getBarcodesAndQualitiesFromTags(read, tags, qualityTags);
        Assert.assertEquals(result.getLeft(), barcodes);
        Assert.assertEquals(result.getRight(), qualities);
    }

    @Test(dataProvider = "barcodesData")
    public void testAddBarcodesTagToRead(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags) throws Exception {
        RTReadUtils.addBarcodesTagToRead(read, barcodes);
        Assert.assertTrue(read.hasAttribute("BC"));
        Assert.assertEquals(read.getAttributeAsString("BC"), String.join("-", barcodes));
    }

    @Test(dataProvider = "barcodesData")
    public void testAddBarcodeWithQualitiesTagsToRead(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags) throws Exception {
        RTReadUtils.addBarcodeWithQualitiesTagsToRead(read, barcodes, qualities);
        Assert.assertTrue(read.hasAttribute("BC"));
        Assert.assertEquals(read.getAttributeAsString("BC"), String.join("-", barcodes));
        Assert.assertTrue(read.hasAttribute("QT"));
        Assert.assertEquals(read.getAttributeAsString("QT"), String.join("-", qualities));
    }

    @Test
    public void testNoBarcodeData() throws Exception {
        final String[] emptyArray = new String[0];
        // this test if all the methods that returns an empty array if there is no barcode data
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        read.setName("read1");
        final String[] fromName = RTReadUtils.extractBarcodesFromReadName(read);
        Assert.assertEquals(fromName, emptyArray);
        final String[] fromTags = RTReadUtils.getBarcodesFromTags(read, twoTags);
        Assert.assertEquals(fromTags, emptyArray);
        final Pair<String[], String[]> withQuals = RTReadUtils.getBarcodesAndQualitiesFromTags(read, twoTags, twoQualTags);
        Assert.assertEquals(withQuals.getLeft(), emptyArray);
        Assert.assertEquals(withQuals.getRight(), emptyArray);
        // this test if all the methods does not update the read if no data is provided
        RTReadUtils.addBarcodesTagToRead(read, emptyArray);
        Assert.assertNull(read.getAttributeAsString("BC"));
        RTReadUtils.addBarcodeWithQualitiesTagsToRead(read, emptyArray, emptyArray);
        Assert.assertNull(read.getAttributeAsString("BC"));
        Assert.assertNull(read.getAttributeAsString("QT"));
    }

    @Test
    public void testGetRawBarcodes() throws Exception {
        final String barcode = "ACTG";
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        read.setAttribute("BC", barcode);
        Assert.assertEquals(RTReadUtils.getRawBarcodes(read), new String[] {barcode});
    }

    @Test
    public void testBarcodeWithoutQuality() throws Exception {
        final String barcode = "ACTG";
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        read.setAttribute(twoTags.get(0), barcode);
        final Pair<String[], String[]> result = RTReadUtils.getBarcodesAndQualitiesFromTags(read, twoTags, twoQualTags);
        Assert.assertEquals(result.getLeft(), new String[]{barcode});
        Assert.assertEquals(result.getRight(),new String[]{ "!!!!"});
    }

    @DataProvider(name = "badParamsBarcodesAndQualitiesFromTags")
    public Object[][] getBadParamsBarcodesAndQualitiesFromTags() {
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        read.setAttribute(twoTags.get(0), "ACTG-TTTT");
        read.setAttribute(twoQualTags.get(0), "ACT");
        read.setAttribute(twoQualTags.get(1), "ACTG_TTTT");
        return new Object[][]{
                {null, null, null},
                {read, null, null},
                {read, Collections.emptyList(), null},
                {read, twoTags, null},
                {read, twoTags, Collections.emptyList()},
                {read, twoTags, Collections.singletonList("QT")},
                {read, Collections.singletonList("RT"), twoQualTags},
                // this fails while detecting barcodes
                {read, twoTags.subList(0, 1), twoQualTags.subList(1, 2)},
                {read, twoTags.subList(0, 1), twoQualTags.subList(0, 1)},
        };
    }

    @Test(dataProvider = "badParamsBarcodesAndQualitiesFromTags", expectedExceptions = IllegalArgumentException.class)
    public void testGetBarcodesAndQualitiesFromTagsBadParams(final GATKRead read,
            final List<String> tags, final List<String> qualTags) throws Exception {
        log(Arrays.toString(RTReadUtils.getBarcodesAndQualitiesFromTags(read, tags, qualTags).getValue()));
    }

    @DataProvider(name = "badParamsAdddBarcodeWithQualitiesTagsToRead")
    public Object[][] getBadParamsAdddBarcodeWithQualitiesTagsToRead() {
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        return new Object[][]{
                {null, null, null},
                {read, null, null},
                {read, new String[0], null},
                {read, new String[0], new String[1]},
                {read, new String[1], new String[0]},
                {read, new String[]{"A"}, new String[]{"AA"}}
        };
    }


    @Test(dataProvider = "badParamsAdddBarcodeWithQualitiesTagsToRead", expectedExceptions = IllegalArgumentException.class)
    public void testAddBarcodeWithQualitiesTagsToReadBadParams(final GATKRead read,
            final String[] barcodes, final String[] qualities) throws Exception {
        RTReadUtils.addBarcodeWithQualitiesTagsToRead(read, barcodes, qualities);
    }
}