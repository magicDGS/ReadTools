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

package org.magicdgs.readtools.utils.misc;

import org.magicdgs.readtools.utils.tests.BaseTest;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class IOUtilsUnitTest extends BaseTest {

    @DataProvider(name = "bamFiles")
    public Object[][] bamFileNames() {
        return new Object[][] {
                {"example.bam"},
                {"example.sam"},
                {"example.cram"},
                {"/folder/example.bam"},
                {"/folder/example.sam"},
                {"/folder/example.cram"},
                {"folder/example.bam"},
                {"folder/example.sam"},
                {"folder/example.cram"},
                {"file:///folder/example.bam"},
                {"file:///folder/example.sam"},
                {"file:///folder/example.cram"}
        };
    }

    @DataProvider(name = "fastqFiles")
    public Object[][] fastqFileNames() {
        return new Object[][] {
                {"example.fastq"},
                {"example.fq"},
                {"example.fastq.gz"},
                {"example.fq.gz"},
                {"example.bfq"},
                {"/folder/example.fastq"},
                {"/folder/example.fq"},
                {"/folder/example.fastq.gz"},
                {"/folder/example.fq.gz"},
                {"/folder/example.bfq"},
                {"folder/example.fastq"},
                {"folder/example.fq"},
                {"folder/example.fastq.gz"},
                {"folder/example.fq.gz"},
                {"folder/example.bfq"},
                {"/folder/example.fastq"},
                {"file:///folder/example.fq"},
                {"file:///folder/example.fastq.gz"},
                {"file:///folder/example.fq.gz"},
                {"file:///folder/example.bfq"},
        };
    }

    @Test(dataProvider = "bamFiles")
    public void testIsSamBamOrCram(final String fileName) throws Exception {
        Assert.assertTrue(IOUtils.isSamBamOrCram(fileName));
    }

    @Test(dataProvider = "fastqFiles")
    public void testIsFastq(final String fileName) throws Exception {
        Assert.assertTrue(IOUtils.isFastq(fileName));
    }

    @Test(dataProvider = "fastqFiles")
    public void testNotIsSamBamOrCram(final String fileName) throws Exception {
        Assert.assertFalse(IOUtils.isSamBamOrCram(fileName));
    }

    @Test(dataProvider = "bamFiles")
    public void testNotIsFastq(final String fileName) throws Exception {
        Assert.assertFalse(IOUtils.isFastq(fileName));
    }

    @Test(dataProvider = "bamFiles")
    public void testIsSamBamOrCramPath(final String fileName) throws Exception {
        final Path path = org.broadinstitute.hellbender.utils.io.IOUtils.getPath(fileName);
        Assert.assertTrue(IOUtils.isSamBamOrCram(path));
    }

    @Test(dataProvider = "fastqFiles")
    public void testNotIsSamBamOrCramPath(final String fileName) throws Exception {
        final Path path = org.broadinstitute.hellbender.utils.io.IOUtils.getPath(fileName);
        Assert.assertFalse(IOUtils.isSamBamOrCram(path));
    }

}