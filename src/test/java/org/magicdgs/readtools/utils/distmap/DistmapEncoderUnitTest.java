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

package org.magicdgs.readtools.utils.distmap;

import org.magicdgs.readtools.RTBaseTest;

import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.Tuple2;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DistmapEncoderUnitTest extends RTBaseTest {

    @DataProvider(name = "singleEnd")
    public Object[][] getDistmapSinleReadStrings() {
        return new Object[][] {
                // without barcode
                {"@readName\tACT\t!!!!"},
                // with barcode
                {"@readName#TTT\tNNACTA\tBBBBBB"}
        };
    }

    @Test(dataProvider = "singleEnd")
    public void testIsPairedFalse(final String singleEndDistmapString) {
        Assert.assertFalse(DistmapEncoder.isPaired(singleEndDistmapString));
    }

    @Test(dataProvider = "singleEnd")
    public void testSingleReadRoundtrip(final String distmapString) {
        // decode the read, encode it again, and re-decoded
        final GATKRead decodedRead = DistmapEncoder.decodeSingle(distmapString);
        final String encodedString = DistmapEncoder.encode(decodedRead);
        final GATKRead reDecodedRead = DistmapEncoder.decodeSingle(encodedString);

        Assert.assertEquals(encodedString, distmapString,
                "String roundtrip error: " + distmapString + " vs. " + encodedString);
        Assert.assertEquals(reDecodedRead.getSAMString(), decodedRead.getSAMString(),
                "Read roundtrip error: " + decodedRead + " vs. " + reDecodedRead);
    }

    @Test(dataProvider = "singleEnd")
    public void testSingleEndFlags(final String distmapString) {
        final GATKRead decodedRead = DistmapEncoder.decodeSingle(distmapString);
        Assert.assertFalse(decodedRead.isPaired());
    }

    @DataProvider(name = "pairEnd")
    public Object[][] getDistmapPairedReadStrings() {
        return new Object[][] {
                // without barcode
                {"@readName\tACT\t!!!!\tTTTTTT\tBBBBBB"},
                // with barcode
                {"@readName#TTT\tNNACTA\tBBBBBB\tACT\t!!!"}
        };
    }

    @Test(dataProvider = "pairEnd")
    public void testIsPairedTrue(final String singleEndDistmapString) {
        Assert.assertTrue(DistmapEncoder.isPaired(singleEndDistmapString));
    }

    @Test(dataProvider = "pairEnd")
    public void testPairedReadRountrip(final String distmapString) {
        // decode the read, encode it again, and re-decoded
        final Tuple2<GATKRead, GATKRead> decodedPair =
                DistmapEncoder.decodePaired(distmapString);
        final String encodedString = DistmapEncoder.encode(decodedPair);
        final Tuple2<GATKRead, GATKRead> reDecodedPair =
                DistmapEncoder.decodePaired(encodedString);

        Assert.assertEquals(encodedString, distmapString,
                "String roundtrip error: " + distmapString + " vs. " + encodedString);
        Assert.assertEquals(reDecodedPair._1.getSAMString(), decodedPair._1.getSAMString(),
                "Pair-read (1) roundtrip error: " + decodedPair._1 + " vs. " + reDecodedPair._1);
        Assert.assertEquals(reDecodedPair._2.getSAMString(), decodedPair._2.getSAMString(),
                "Pair-read (2) roundtrip error: " + decodedPair._2 + " vs. " + reDecodedPair._2);
    }

    @Test(dataProvider = "pairEnd")
    public void testPairEndFlags(final String distmapString) {
        final Tuple2<GATKRead, GATKRead> pair = DistmapEncoder.decodePaired(distmapString);

        Assert.assertTrue(pair._1.getName().equals(pair._2.getName()));

        Assert.assertTrue(pair._1.isPaired());
        Assert.assertTrue(pair._1.isFirstOfPair());

        Assert.assertTrue(pair._2.isPaired());
        Assert.assertTrue(pair._2.isSecondOfPair());
    }

    @Test(dataProvider = "pairEnd", expectedExceptions = DistmapException.class)
    public void testWrongSingleEnd(final String pairedDistmapString) {
        DistmapEncoder.decodeSingle(pairedDistmapString);
    }

    @Test(dataProvider = "singleEnd", expectedExceptions = DistmapException.class)
    public void testWrongPairedEnd(final String singleDistmapString) {
        DistmapEncoder.decodePaired(singleDistmapString);
    }

    @DataProvider(name = "wrongStrings")
    public Object[][] getWrongDistmapString() {
        return new Object[][] {
                // incomplete records
                {"@readName"},
                {"@readName\tACTG"},
                {"@readName\tACTG\t!!!!\tACTG"},
                // extra fields
                {"@readName\tACT\t!!!!\tTTTTTT\tBBBBBB\tACTG"},
                // not @ symbol at the begining
                {"readName\tACT\t!!!!\tTTTTTT\tBBBBBB"}
        };
    }

    @Test(dataProvider = "wrongStrings")
    public void testWrongEncoding(final String wrongDistmapString) {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> DistmapEncoder.decodeSingle(wrongDistmapString));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> DistmapEncoder.decodePaired(wrongDistmapString));
    }

    @Test(dataProvider = "pairEnd", expectedExceptions = DistmapException.class)
    public void testEncodeDifferentNamesThrows(final String distmapString) {
        final Tuple2<GATKRead, GATKRead> pair = DistmapEncoder.decodePaired(distmapString);
        pair._1.setName("read1");
        pair._2.setName("read2");
        DistmapEncoder.encode(pair);
    }
}