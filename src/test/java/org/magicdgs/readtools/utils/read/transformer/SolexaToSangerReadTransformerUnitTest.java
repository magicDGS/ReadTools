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

import org.magicdgs.readtools.BaseTest;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMUtils;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class SolexaToSangerReadTransformerUnitTest extends BaseTest {

    private static final SolexaToSangerReadTransformer transformer =
            new SolexaToSangerReadTransformer();

    private static final SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeader();

    @DataProvider(name = "solexaQuals")
    public Object[][] solexaQualsProvider() {
        return new Object[][] {
                // full range in both directions
                {";<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
                        "!!!!!$%%&&'()*++,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_"},
                {"~}|{zyxwvutsrqponmlkjihgfedcba`_^]\\[ZYXWVUTSRQPONMLKJIHGFEDCBA@?>=<;",
                        "_^]\\[ZYXWVUTSRQPONMLKJIHGFEDCBA@?>=<;:9876543210/.-,++*)('&&%%$!!!!!"}
        };
    }

    @Test(dataProvider = "solexaQuals")
    public void testConvertQualities(final String solexaQualityString,
            final String expectedSolexaQuals) throws Exception {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(header,
                Utils.dupBytes((byte) 'A', solexaQualityString.length()),
                SAMUtils.fastqToPhred(solexaQualityString)
        );
        Assert.assertEquals(ReadUtils.getBaseQualityString(transformer.apply(read)),
                expectedSolexaQuals);
    }

    @DataProvider(name = "badQuals")
    public Iterator<Object[]> solexaBadQualsProvider() {
        // sanger range not included in Solexa
        return "!\"#$%&'()*+,-./0123456789:"
                .chars()
                .mapToObj(p -> new Object[] {new String(new char[] {(char) p})})
                .iterator();
    }

    @Test(dataProvider = "badQuals", expectedExceptions = UserException.BadInput.class)
    public void testBadQualities(final String badQual) {
        final GATKRead read = ArtificialReadUtils.createArtificialUnmappedRead(
                header, new byte[] {'A'}, SAMUtils.fastqToPhred(badQual));
        transformer.apply(read);
        log(ReadUtils.getBaseQualityString(read));
    }
}