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
package org.magicdgs.utils.record;

import static org.magicdgs.utils.record.SequenceMatch.mismatchesCount;
import static org.magicdgs.utils.record.SequenceMatch.missingCount;
import static org.magicdgs.utils.record.SequenceMatch.sequenceContainNs;
import static org.magicdgs.utils.record.SequenceMatch.sequenceEndByNs;
import static org.magicdgs.utils.record.SequenceMatch.sequenceStartByN;

import org.junit.Assert;
import org.junit.Test;

public class SequenceMatchTest {

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
        Assert.assertEquals(2, mismatchesCount("ACTG", "ACCC"));
        Assert.assertEquals(2, mismatchesCount("ACTG", "ACCC", false));
        // test for missing bases
        Assert.assertEquals(2, mismatchesCount("ACTG", "ACNN"));
        Assert.assertEquals(2, mismatchesCount("ACNN", "ACTG"));
        Assert.assertEquals(0, mismatchesCount("ACTG", "ACNN", false));
        Assert.assertEquals(0, mismatchesCount("ACNN", "ACTG", false));
        // test for different base case
        Assert.assertEquals(0, mismatchesCount("ACTG", "actg"));
        Assert.assertEquals(2, mismatchesCount("ACTG", "accc"));
        Assert.assertEquals(2, mismatchesCount("ACTG", "acnn"));
        Assert.assertEquals(0, mismatchesCount("ACTG", "acnn", false));
    }

    @Test
    public void testNcount() throws Exception {
        // uppercase
        Assert.assertEquals(6, missingCount("NNAAANNAAANN"));
        // lowercase
        Assert.assertEquals(6, missingCount("nnaaannaaann"));
    }
}