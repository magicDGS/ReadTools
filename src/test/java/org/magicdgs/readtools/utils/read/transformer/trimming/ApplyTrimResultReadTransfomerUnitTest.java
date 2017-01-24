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

package org.magicdgs.readtools.utils.read.transformer.trimming;

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.TextCigarCodec;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ApplyTrimResultReadTransfomerUnitTest extends BaseTest {

    // transformer to test
    private static final ReadTransformer transformer =
            new ApplyTrimResultReadTransfomer(false, false);

    private static final SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeader();

    private static GATKRead baseUnmappedRead() {
        return ArtificialReadUtils
                .createArtificialUnmappedRead(header,
                        new byte[] {'T', 'C', 'A', 'A', 'C', 'T'},
                        new byte[] {'!', '*', 'I', 'I', '*', '!'});
    }

    private static GATKRead baseMappedRead() {
        return ArtificialReadUtils.createArtificialRead(header,
                new byte[] {'T', 'C', 'A', 'A', 'C', 'T'},
                new byte[] {'!', '*', 'I', 'I', '*', '!'},
                "6M");
    }

    private static void testTrimmedRead(final GATKRead trimRead, final int length,
            final byte[] bases, final byte[] quals, final Cigar cigar) throws Exception {
        // check the data provider
        Assert.assertEquals(bases.length, length, "broken data provider");
        Assert.assertEquals(quals.length, length, "broken data provider");
        // check that the read is correctly trimmed
        Assert.assertEquals(trimRead.getLength(), length, "wrong length");
        Assert.assertEquals(trimRead.getBases(), bases, "wrong bases");
        Assert.assertEquals(trimRead.getBaseQualities(), quals, "wrong qualities");
        Assert.assertEquals(trimRead.getCigar(), cigar, "wrong cigar");
        // check that the tags are correctly set
        Assert.assertNull(trimRead.getAttributeAsInteger("ts"), "non-null ts");
        Assert.assertNull(trimRead.getAttributeAsInteger("te"), "non-null te");
        Assert.assertNotNull(trimRead.getAttributeAsInteger("ct"), "null ct");
    }

    @DataProvider(name = "trimmedUmapped")
    public Iterator<Object[]> getTransformedUnmappedReads() {
        final List<Object[]> data = new ArrayList<>();
        // fill in with unmapped reads
        final GATKRead read = baseUnmappedRead();
        final int length = read.getLength();
        read.setAttribute("ts", 1);
        read.setAttribute("te", length - 1);
        data.add(new Object[] {read, 1, length - 1, length - 2});
        for (int i = 1; i < length; i++) {
            final int finalLength = length - i;
            final GATKRead fivePrime = baseUnmappedRead();
            fivePrime.setAttribute("ts", i);
            data.add(new Object[] {fivePrime, i, length, finalLength});
            final GATKRead threePrime = baseUnmappedRead();
            threePrime.setAttribute("te", length - i);
            data.add(new Object[] {threePrime, 0, finalLength, finalLength});
        }
        return data.iterator();
    }

    @Test(dataProvider = "trimmedUmapped")
    public void testCutUnmappedReads(final GATKRead read, final int start, final int end,
            final int length) throws Exception {
        final byte[] bases = Arrays.copyOfRange(read.getBases(), start, end);
        final byte[] quals = Arrays.copyOfRange(read.getBaseQualities(), start, end);
        final GATKRead trimmed = transformer.apply(read);
        Assert.assertSame(trimmed, read);
        testTrimmedRead(trimmed, length, bases, quals, new Cigar());
    }

    @DataProvider(name = "trimmedMapped")
    public Iterator<Object[]> getTransformedMappedReads() {
        final List<Object[]> data = new ArrayList<>();
        // fill in with unmapped reads
        final GATKRead read = baseMappedRead();
        final int length = read.getLength();
        read.setName("trimmed_both_sides");
        read.setAttribute("ts", 1);
        read.setAttribute("te", length - 1);
        data.add(new Object[] {read, 1, length - 1, length - 2, "1H" + (length - 2) + "M1H"});
        for (int i = 1; i < length; i++) {
            final GATKRead fivePrime = baseMappedRead();
            fivePrime.setName("trimmed_5_p");
            final int finalLength = length - i;
            fivePrime.setAttribute("ts", i);
            data.add(new Object[] {fivePrime, i, length, finalLength, i + "H" + finalLength + "M"});
            final GATKRead threePrime = baseMappedRead();
            threePrime.setName("trimmed_3_p");
            threePrime.setAttribute("te", length - i);
            data.add(new Object[] {threePrime, 0, finalLength, finalLength,
                    finalLength + "M" + i + "H"});
        }
        return data.iterator();
    }

    @Test(dataProvider = "trimmedMapped")
    public void testCutMappedReads(final GATKRead read, final int start, final int end,
            final int length, final String cigarString) throws Exception {
        final byte[] bases = Arrays.copyOfRange(read.getBases(), start, end);
        final byte[] quals = Arrays.copyOfRange(read.getBaseQualities(), start, end);
        final Cigar cigar = TextCigarCodec.decode(cigarString);
        Assert.assertEquals(cigar.getReadLength(), length, "broken data provided");
        final GATKRead trimmed = transformer.apply(read);
        Assert.assertSame(trimmed, read);
        testTrimmedRead(trimmed, length, bases, quals, cigar);
    }

    @DataProvider(name = "noCutReads")
    public Object[][] getUncutReads() {
        final GATKRead completelyTrimmed1 = baseUnmappedRead();
        completelyTrimmed1.setAttribute("ct", 1);
        final GATKRead completelyTrimmed2 = baseUnmappedRead();
        completelyTrimmed2.setAttribute("ts", completelyTrimmed2.getLength());
        final GATKRead completelyTrimmed3 = baseUnmappedRead();
        completelyTrimmed3.setAttribute("te", 0);
        return new Object[][] {
                {baseUnmappedRead()}, {completelyTrimmed1}, {completelyTrimmed2},
                {completelyTrimmed3}, {baseMappedRead()}
        };
    }

    @Test(dataProvider = "noCutReads")
    public void testNoCutReads(final GATKRead read) throws Exception {
        final int length = read.getLength();
        final byte[] bases = read.getBases();
        final byte[] quals = read.getBaseQualities();
        final Cigar cigar = read.getCigar();
        final GATKRead trimRead = transformer.apply(read);
        // this should be exactly the same read, I guess
        Assert.assertSame(trimRead, read);
        testTrimmedRead(trimRead, length, bases, quals, cigar);
    }


    @DataProvider(name = "no5prime")
    public Iterator<Object[]> no5primeData() {
        final List<GATKRead> reads = Arrays.asList(
                baseMappedRead(), baseMappedRead()
        );
        return reads.stream().map(read -> {
            final int length = read.getLength();
            final int finalLength = length - 1;
            read.setAttribute("ts", 1);
            read.setAttribute("te", finalLength);
            final String cigar = (read.isUnmapped()) ? "*" : finalLength + "M1H";
            return new Object[] {read, finalLength,
                    Arrays.copyOfRange(read.getBases(), 0, finalLength),
                    Arrays.copyOfRange(read.getBaseQualities(), 0, finalLength),
                    cigar};
        }).iterator();
    }


    @Test(dataProvider = "no5prime")
    public void testSwithOff5primeMapped(final GATKRead read, final int length, final byte[] bases,
            final byte[] quals, final String cigarString) throws Exception {
        final ApplyTrimResultReadTransfomer no5prime =
                new ApplyTrimResultReadTransfomer(true, false);
        testTrimmedRead(no5prime.apply(read), length, bases, quals,
                TextCigarCodec.decode(cigarString));
    }


    @DataProvider(name = "no3prime")
    public Iterator<Object[]> no3primeData() {
        final List<GATKRead> reads = Arrays.asList(
                baseMappedRead(), baseMappedRead()
        );
        return reads.stream().map(read -> {
            final int length = read.getLength();
            final int finalLength = length - 1;
            read.setAttribute("ts", 1);
            read.setAttribute("te", finalLength);
            final String cigar = (read.isUnmapped()) ? "*" : "1H" + finalLength + "M";
            return new Object[] {read, finalLength,
                    Arrays.copyOfRange(read.getBases(), 1, length),
                    Arrays.copyOfRange(read.getBaseQualities(), 1, length),
                    cigar};
        }).iterator();
    }


    @Test(dataProvider = "no3prime")
    public void testSwitchOff3primeMapped(final GATKRead read, final int length, final byte[] bases,
            final byte[] quals, final String cigarString) throws Exception {
        final ApplyTrimResultReadTransfomer no3prime =
                new ApplyTrimResultReadTransfomer(false, true);
        testTrimmedRead(no3prime.apply(read), length, bases, quals,
                TextCigarCodec.decode(cigarString));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadArgument() throws Exception {
        new ApplyTrimResultReadTransfomer(true, true);
    }

    @Test(dataProvider = "trimmedMapped")
    public void testReadWithIndelQuals(final GATKRead read, final int start, final int end,
            final int length, final String cigarString) throws Exception {
        final byte[] bases = Arrays.copyOfRange(read.getBases(), start, end);
        final byte[] quals = Arrays.copyOfRange(read.getBaseQualities(), start, end);
        final Cigar cigar = TextCigarCodec.decode(cigarString);
        Assert.assertEquals(cigar.getReadLength(), length, "broken data provided");

        // make a copy of the read and set indel quals
        ReadUtils.setInsertionBaseQualities(read, read.getBaseQualities());

        // trimming and checking
        final GATKRead trimmed = transformer.apply(read);
        Assert.assertSame(trimmed, read);
        testTrimmedRead(trimmed, length, bases, quals, cigar);

        // assert the insertion qualities
        Assert.assertEquals(ReadUtils.getBaseInsertionQualities(read), quals);
    }

}