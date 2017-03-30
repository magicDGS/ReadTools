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

package org.magicdgs.readtools.utils.read.transformer;

import org.magicdgs.readtools.RTBaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMUtils;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class CheckQualityReadTransformerUnitTest extends RTBaseTest {

    private static final SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeader();

    @DataProvider(name = "standardQuals")
    public Object[][] standardQualsProvider() {
        return new Object[][] {
                // Standard range
                {"!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHI"},
                // Standard range extended to quality of 60
                {"!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ["}
        };
    }

    @Test(dataProvider = "standardQuals")
    public void testCorrectQuals(final String qualities) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(header,
                Utils.dupBytes((byte) 'A', qualities.length()),
                SAMUtils.fastqToPhred(qualities)
        );
        final CheckQualityReadTransformer transformer =
                new CheckQualityReadTransformer();
        final GATKRead copy = read.deepCopy();
        Assert.assertEquals(transformer.apply(read), copy);
        transformer.currentReadCounter.set(1001);
        Assert.assertEquals(transformer.apply(read), copy);
    }

    @DataProvider(name = "badQuals")
    public Iterator<Object[]> badQualsProvider() {
        // invalid range, including some Illumina qualities
        return "^_`abcdefghijklmnopqrstuvwxyz{|}~"
                .chars()
                .mapToObj(p -> new Object[] {new String(new char[] {(char) p})})
                .iterator();
    }

    @Test(dataProvider = "badQuals")
    public void testBadQualitiesNoThrown(final String badQual) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(
                header, new byte[] {'A'}, SAMUtils.fastqToPhred(badQual));
        final CheckQualityReadTransformer transformer =
                new CheckQualityReadTransformer();
        transformer.apply(read);
    }

    @Test(dataProvider = "badQuals", expectedExceptions = UserException.MisencodedQualityScoresRead.class)
    public void testBadQualitiesCheck(final String badQual) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(
                header, new byte[] {'A'}, SAMUtils.fastqToPhred(badQual));
        final CheckQualityReadTransformer transformer =
                new CheckQualityReadTransformer();
        transformer.currentReadCounter.set(1001);
        transformer.apply(read);
        log(String.valueOf(read.getBaseQuality(0)));
    }

}