/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.tools.barcodes.dictionary.decoder;

import org.magicdgs.readtools.utils.tests.BaseTest;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeMatchUnitTest extends BaseTest {

    private final static Set<String> ALL_BARCODES = new LinkedHashSet<>(
            Arrays.asList("AAAA", "TTTT", "CCCC", "AATC"));

    @DataProvider
    public Iterator<Object[]> bestBarcodes() throws Exception {
        final List<Object[]> data = new ArrayList<>(ALL_BARCODES.size());
        // test perfect match
        ALL_BARCODES.forEach(b -> {
            // first index with Ns as mismatches
            data.add(new Object[] {1, b, true, b, 0, false, true});
            // second index without Ns as mismatches
            data.add(new Object[] {2, b, false, b, 0, false, true});
            // cutting the last base
            data.add(new Object[] {1, b + "T", true, b, 0, false, true});
        });

        // test with one N at the end for the ones repeated
        ALL_BARCODES.forEach(b -> {
            final String withFirstN = "N" + b.substring(1);
            // first index with Ns as mismatches
            data.add(new Object[] {1, withFirstN, true, b, 1, false, true});
            // second index without Ns as mismatches
            data.add(new Object[] {2, withFirstN, false, b, 0, false, true});
        });

        // test ambiguous
        data.add(new Object[] {1, "AANN", true, "AAAA", 2, true, false});
        data.add(new Object[] {1, "AANN", false, "AAAA", 0, true, false});

        // test not possible to match
        data.add(new Object[] {1, "GGGG", true, "UNKNOWN", 4, true, false});
        data.add(new Object[] {1, "GGGGG", true, "UNKNOWN", 5, true, false});

        return data.iterator();
    }

    @Test(dataProvider = "bestBarcodes")
    public void testGetBestBarcodeMatch(final int index, final String toMatch,
            final boolean nAsMismatch,
            final String expectedBarcode, final int expectedMismatches, final boolean ambiguous,
            final boolean isAssignableFor2) throws Exception {
        final BarcodeMatch barcodeMatch = BarcodeMatch
                .getBestBarcodeMatch(index, toMatch, ALL_BARCODES, nAsMismatch);
        Assert.assertEquals(barcodeMatch.getIndexNumber(), index, "wrong index");
        Assert.assertEquals(barcodeMatch.getBarcode(), expectedBarcode, "wrong barcode");
        Assert.assertEquals(barcodeMatch.getMismatches(), expectedMismatches, "wrong # mismatch");
        Assert.assertEquals(barcodeMatch.getNumberOfNs(), StringUtils.countMatches(toMatch, 'N'),
                "wrong # Ns");
        Assert.assertEquals(barcodeMatch.isMatch(),
                !expectedBarcode.equals(BarcodeMatch.UNKNOWN_STRING), "wrong isMatch");
        Assert.assertEquals(barcodeMatch.isAmbiguous(), ambiguous, "wrong ambiguous");
        Assert.assertEquals(barcodeMatch.isAssignable(2), isAssignableFor2, "wrong isAssignable");
    }

}