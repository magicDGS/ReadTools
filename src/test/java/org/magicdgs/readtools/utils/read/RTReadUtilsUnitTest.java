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

package org.magicdgs.readtools.utils.read;

import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.SAMFileHeader;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.hellbender.utils.Utils;
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
public class RTReadUtilsUnitTest extends RTBaseTest {

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
                    twoTags.subList(0, 1), new String[] {quality1}, twoQualTags.subList(0, 1)});
            for (final String barcode2 : new String[] {"ACTG", "GGGG"}) {
                read.setName(baseReadName + "-" + barcode2);
                read.setAttribute(twoTags.get(0), barcode);
                read.setAttribute(twoTags.get(1), barcode2);
                read.setAttribute(twoQualTags.get(0), quality1);
                read.setAttribute(twoQualTags.get(1), quality2);
                data.add(new Object[] {new String[] {barcode, barcode2}, read.deepCopy(),
                        twoTags, new String[] {quality1, quality2}, twoQualTags});
                read.setAttribute(twoTags.get(0), barcode + "-" + barcode2);
                read.setAttribute(twoTags.get(1), (String) null);
                read.setAttribute(twoQualTags.get(0), quality1 + " " + quality2);
                read.setAttribute(twoQualTags.get(1), (String) null);
                data.add(new Object[] {new String[] {barcode, barcode2}, read.deepCopy(),
                        twoTags.subList(0, 1), new String[] {quality1, quality2},
                        twoQualTags.subList(0, 1)});
            }
        }

        return data.iterator();
    }

    @Test(dataProvider = "barcodesData")
    public void testExtractBarcodesFromReadName(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags)
            throws Exception {
        Assert.assertNotEquals(read.getName().indexOf('#'), -1);
        Assert.assertEquals(RTReadUtils.extractBarcodesFromReadName(read), barcodes);
        Assert.assertEquals(read.getName().indexOf('#'), -1);
    }

    @Test(dataProvider = "barcodesData")
    public void testGetBarcodesFromTags(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags)
            throws Exception {
        Assert.assertEquals(RTReadUtils.getBarcodesFromTags(read, tags), barcodes);
    }

    @Test(dataProvider = "barcodesData")
    public void testGetBarcodesAndQualitiesFromTags(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags)
            throws Exception {
        final Pair<String[], String[]> result =
                RTReadUtils.getBarcodesAndQualitiesFromTags(read, tags, qualityTags);
        Assert.assertEquals(result.getLeft(), barcodes);
        Assert.assertEquals(result.getRight(), qualities);
    }

    @Test(dataProvider = "barcodesData")
    public void testAddBarcodesTagToRead(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags)
            throws Exception {
        RTReadUtils.addBarcodesTagToRead(read, barcodes);
        Assert.assertTrue(read.hasAttribute("BC"));
        Assert.assertEquals(read.getAttributeAsString("BC"), String.join("-", barcodes));
    }

    @Test(dataProvider = "barcodesData")
    public void testAddBarcodeWithQualitiesTagsToRead(final String[] barcodes, final GATKRead read,
            final List<String> tags, final String[] qualities, final List<String> qualityTags)
            throws Exception {
        RTReadUtils.addBarcodeWithQualitiesTagsToRead(read, barcodes, qualities);
        Assert.assertTrue(read.hasAttribute("BC"));
        Assert.assertEquals(read.getAttributeAsString("BC"), String.join("-", barcodes));
        Assert.assertTrue(read.hasAttribute("QT"));
        Assert.assertEquals(read.getAttributeAsString("QT"), String.join(" ", qualities));
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
        final Pair<String[], String[]> withQuals =
                RTReadUtils.getBarcodesAndQualitiesFromTags(read, twoTags, twoQualTags);
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
        final Pair<String[], String[]> result =
                RTReadUtils.getBarcodesAndQualitiesFromTags(read, twoTags, twoQualTags);
        Assert.assertEquals(result.getLeft(), new String[] {barcode});
        Assert.assertEquals(result.getRight(), new String[] {"!!!!"});
    }

    @DataProvider(name = "badParamsBarcodesAndQualitiesFromTags")
    public Object[][] getBadParamsBarcodesAndQualitiesFromTags() {
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        read.setAttribute(twoTags.get(0), "ACTG-TTTT");
        read.setAttribute(twoQualTags.get(0), "ACT");
        read.setAttribute(twoQualTags.get(1), "ACTG_TTTT");
        return new Object[][] {
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
        log(Arrays.toString(
                RTReadUtils.getBarcodesAndQualitiesFromTags(read, tags, qualTags).getValue()));
    }

    @DataProvider(name = "badParamsAddBarcodeWithQualitiesTagsToRead")
    public Object[][] getBadParamsAddBarcodeWithQualitiesTagsToRead() {
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[] {'A', 'T'}, new byte[] {40, 40});
        return new Object[][] {
                {null, null, null},
                {read, null, null},
                {read, new String[0], null},
                {read, new String[0], new String[1]},
                {read, new String[1], new String[0]},
                {read, new String[] {"A"}, new String[] {"AA"}}
        };
    }


    @Test(dataProvider = "badParamsAddBarcodeWithQualitiesTagsToRead", expectedExceptions = IllegalArgumentException.class)
    public void testAddBarcodeWithQualitiesTagsToReadBadParams(final GATKRead read,
            final String[] barcodes, final String[] qualities) throws Exception {
        RTReadUtils.addBarcodeWithQualitiesTagsToRead(read, barcodes, qualities);
    }

    private static List<Object[]> generateTrimmingData() {
        final List<Object[]> data = new ArrayList<>();
        final int readLength = 50;
        final byte[] bases = Utils.repeatBytes((byte) 'A', readLength);
        final byte[] quals = Utils.repeatBytes((byte) 'I', readLength);
        // all combinations with te, ts, tc set for this read length
        for (int ts = 0; ts <= readLength; ts++) {
            for (int te = readLength; te >= 0; te--) {
                final GATKRead read = ArtificialReadUtils
                        .createArtificialUnmappedRead(header, bases, quals);
                final int ct;
                //= te == 0 || ts == readLength || ts >= te || te <= ts;
                if (ts == readLength) {
                    ct = 1;
                } else if (te == 0) {
                    ct = 2;
                } else if (ts >= te) {
                    ct = 3;
                } else {
                    ct = 0;
                }
                data.add(new Object[] {read, ts, te, ct});
            }
        }
        return data;
    }

    @DataProvider(name = "trimmingPointSetters")
    public Iterator<Object[]> trimmingPointSetters() {
        return generateTrimmingData().iterator();
    }

    @DataProvider(name = "trimmingPointGetters")
    public Iterator<Object[]> trimmingPointGetters() {
        final List<Object[]> data = generateTrimmingData();
        data.forEach(o -> {
            final GATKRead read = (GATKRead) o[0];
            read.setAttribute("ts", (int) o[1]);
            read.setAttribute("te", (int) o[2]);
            read.setAttribute("ct", (int) o[3]);
            o[3] = (int) o[3] != 0;
        });
        // add case without information
        data.add(new Object[] {
                ArtificialReadUtils
                        .createArtificialUnmappedRead(header, new byte[100], new byte[100]),
                0, 100, false
        });
        return data.iterator();
    }

    @Test(dataProvider = "trimmingPointGetters")
    public void testTrimmingPointGetters(final GATKRead read, final int ts, final int te,
            final boolean ct) throws Exception {
        Assert.assertEquals(RTReadUtils.getTrimmingStartPoint(read), ts);
        Assert.assertEquals(RTReadUtils.getTrimmingEndPoint(read), te);
        Assert.assertEquals(RTReadUtils.isCompletelyTrimRead(read), ct);
    }

    @Test(dataProvider = "trimmingPointSetters")
    public void testSingleValueTrimmingPointSetters(final GATKRead read, final int ts, final int te,
            final int ct) throws Exception {
        // start tag
        Assert.assertNull(read.getAttributeAsInteger("ts"));
        RTReadUtils.updateTrimmingStartPointTag(read, ts);
        Assert.assertEquals(read.getAttributeAsInteger("ts").intValue(), ts);
        // end tag
        Assert.assertNull(read.getAttributeAsInteger("te"));
        RTReadUtils.updateTrimmingEndPointTag(read, te);
        Assert.assertEquals(read.getAttributeAsInteger("te").intValue(), te);
        // completely trimmed
        Assert.assertNull(read.getAttributeAsInteger("ct"));
        Assert.assertEquals(RTReadUtils.updateCompletelyTrimReadFlag(read), ct != 0);
        Assert.assertEquals(read.getAttributeAsInteger("ct").intValue(), ct);
        // now clear the attributes
        RTReadUtils.clearTrimmingPointTags(read);
        Assert.assertNull(read.getAttributeAsInteger("ts"));
        Assert.assertNull(read.getAttributeAsInteger("te"));
        Assert.assertEquals(read.getAttributeAsInteger("ct").intValue(), ct);
    }

    @Test(dataProvider = "trimmingPointSetters")
    public void testUpdateAllTrimmingPoints(final GATKRead read, final int ts, final int te,
            final int ct) throws Exception {
        Assert.assertNull(read.getAttributeAsInteger("ts"));
        Assert.assertNull(read.getAttributeAsInteger("te"));
        Assert.assertNull(read.getAttributeAsInteger("ct"));
        RTReadUtils.updateTrimmingPointTags(read, ts, te);
        Assert.assertEquals(read.getAttributeAsInteger("ts").intValue(), ts);
        Assert.assertEquals(read.getAttributeAsInteger("te").intValue(), te);
        Assert.assertEquals(read.getAttributeAsInteger("ct").intValue(), ct);
    }

    @DataProvider(name = "badArgumentsTrimming")
    public Object[][] badArgumentsFOrSingleValueTrimmingPointSetters() {
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[0], new byte[0]);
        return new Object[][] {
                {null, 10},
                {read, -1},
                {read, -10}
        };
    }

    @Test(dataProvider = "badArgumentsTrimming", expectedExceptions = IllegalArgumentException.class)
    public void testBadUpdateTrimmingStartPointTag(final GATKRead read, final int value)
            throws Exception {
        RTReadUtils.updateTrimmingStartPointTag(read, value);
    }

    @Test(dataProvider = "badArgumentsTrimming", expectedExceptions = IllegalArgumentException.class)
    public void testBadUpdateTrimmingEndPointTag(final GATKRead read, final int value)
            throws Exception {
        RTReadUtils.updateTrimmingEndPointTag(read, value);
    }

    @Test
    public void testUpdateCompletelyTrimReadFlagAlreadySet() throws Exception {
        final GATKRead read = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[0], new byte[0]);
        read.setAttribute("ct", 1);
        Assert.assertTrue(RTReadUtils.updateCompletelyTrimReadFlag(read));
    }

    @DataProvider(name = "fixPairTagData")
    public Object[][] getFixPairTagData() throws Exception {
        final GATKRead read1 = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[0], new byte[0]);
        read1.setAttribute("R1", "true");
        read1.setAttribute("ST", "1");
        final GATKRead read2 = ArtificialReadUtils
                .createArtificialUnmappedRead(header, new byte[0], new byte[0]);
        read2.setAttribute("R2", "true");
        read2.setAttribute("ST", "2");
        return new Object[][] {
                // change one or the other
                {"R1", read1, read2, "true", "true"},
                {"R2", read1, read2, "true", "true"},
                // no change
                {"ST", read1, read2, "1", "2"},
                // both null
                {"NL", read1, read2, null, null}
        };
    }

    @Test(dataProvider = "fixPairTagData")
    public void testFixPairTag(final String tag, final GATKRead read1, final GATKRead read2,
            final String expectedTagValue1, final String expectedTagValue2) throws Exception {
        RTReadUtils.fixPairTag(tag, read1, read2);
        Assert.assertEquals(read1.getAttributeAsString(tag), expectedTagValue1);
        Assert.assertEquals(read2.getAttributeAsString(tag), expectedTagValue2);
    }

    @DataProvider(name = "illuminaNames")
    public Object[][] getReadWithIlluminaNames() {
        final GATKRead readWithBarcodes = ArtificialReadUtils.createArtificialRead("1M");
        readWithBarcodes.setName("readWithBarcodes");
        readWithBarcodes.setAttribute("BC", "ACTG-ACCC");
        final GATKRead readWithNoBarcodes = ArtificialReadUtils.createArtificialRead("1M");
        readWithNoBarcodes.setName("readWithNoBarcodes");
        return new Object[][] {
                {readWithBarcodes, "readWithBarcodes#ACTG-ACCC"},
                {readWithNoBarcodes, readWithNoBarcodes.getName()}
        };
    }

    @Test(dataProvider = "illuminaNames")
    public void testGetReadNameWithIlluminaBarcode(final GATKRead read, final String expectedName) {
        Assert.assertEquals(RTReadUtils.getReadNameWithIlluminaBarcode(read), expectedName);
    }
}