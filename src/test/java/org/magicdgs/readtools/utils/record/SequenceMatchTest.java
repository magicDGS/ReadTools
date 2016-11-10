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
package org.magicdgs.readtools.utils.record;

import static org.magicdgs.readtools.utils.record.SequenceMatch.mismatchesCount;
import static org.magicdgs.readtools.utils.record.SequenceMatch.missingCount;
import static org.magicdgs.readtools.utils.record.SequenceMatch.sequenceContainNs;
import static org.magicdgs.readtools.utils.record.SequenceMatch.sequenceEndByNs;
import static org.magicdgs.readtools.utils.record.SequenceMatch.sequenceStartByN;

import org.magicdgs.readtools.utils.tests.BaseTest;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SequenceMatchTest extends BaseTest {

    @Test
    public void testSequenceStartByN() throws Exception {
        // uppercase
        Assert.assertTrue(sequenceStartByN("NNNAAAAA"));
        Assert.assertFalse(sequenceStartByN("AAAANNNN"));
        Assert.assertFalse(sequenceStartByN("AANNNNAAA"));
        // lowercase
        Assert.assertTrue(sequenceStartByN("nnnaaaaa"));
        Assert.assertFalse(sequenceStartByN("aaaannnn"));
        Assert.assertFalse(sequenceStartByN("annnnnna"));
    }

    @Test
    public void testSequenceEndByNs() throws Exception {
        // uppercase
        Assert.assertTrue(sequenceEndByNs("AAAANNNN"));
        Assert.assertFalse(sequenceEndByNs("NNNAAAAA"));
        Assert.assertFalse(sequenceEndByNs("AANNNNAAA"));
        // lowercase
        Assert.assertTrue(sequenceEndByNs("aaaannnn"));
        Assert.assertFalse(sequenceEndByNs("nnnaaaaa"));
        Assert.assertFalse(sequenceEndByNs("annnnnna"));
    }

    @Test
    public void testSequenceContainNs() throws Exception {
        // uppercase
        Assert.assertTrue(sequenceContainNs("AAAANNNN"));
        Assert.assertTrue(sequenceContainNs("NNNAAAAA"));
        Assert.assertTrue(sequenceContainNs("AANNNNAAA"));
        // lowercase
        Assert.assertTrue(sequenceContainNs("nnnaaaaa"));
        Assert.assertTrue(sequenceContainNs("aaaannnn"));
        Assert.assertTrue(sequenceContainNs("annnnnna"));
    }

    @Test
    public void testMismatchesCount() throws Exception {
        // test for real bases mismatches
        Assert.assertEquals(mismatchesCount("ACTG", "ACCC"), 2);
        Assert.assertEquals(mismatchesCount("ACTG", "ACCC", false), 2);
        // test for missing bases
        Assert.assertEquals(mismatchesCount("ACTG", "ACNN"), 2);
        Assert.assertEquals(mismatchesCount("ACNN", "ACTG"), 2);
        Assert.assertEquals(mismatchesCount("ACTG", "ACNN", false), 0);
        Assert.assertEquals(mismatchesCount("ACNN", "ACTG", false), 0);
        // test for different base case
        Assert.assertEquals(mismatchesCount("ACTG", "actg"), 0);
        Assert.assertEquals(mismatchesCount("ACTG", "accc"), 2);
        Assert.assertEquals(mismatchesCount("ACTG", "acnn"), 2);
        Assert.assertEquals(mismatchesCount("ACTG", "acnn", false), 0);
    }

    @Test
    public void testNcount() throws Exception {
        // uppercase
        Assert.assertEquals(missingCount("NNAAANNAAANN"), 6);
        // lowercase
        Assert.assertEquals(missingCount("nnaaannaaann"), 6);
    }
}