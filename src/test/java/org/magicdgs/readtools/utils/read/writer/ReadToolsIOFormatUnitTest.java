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

package org.magicdgs.readtools.utils.read.writer;

import org.magicdgs.readtools.utils.tests.BaseTest;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadToolsIOFormatUnitTest extends BaseTest {

    @DataProvider(name = "formats")
    public Object[][] getFormats() {
        return new Object[][] {
                {ReadToolsIOFormat.FastqFormat.PLAIN, ".fq"},
                {ReadToolsIOFormat.FastqFormat.GZIP, ".fq.gz"},
                {ReadToolsIOFormat.BamFormat.BAM, ".bam"},
                {ReadToolsIOFormat.BamFormat.SAM, ".sam"},
                {ReadToolsIOFormat.BamFormat.CRAM, ".cram"}
        };
    }

    @Test(dataProvider = "formats")
    public void testOutputFormats(final ReadToolsIOFormat format, final String extension)
            throws Exception {
        Assert.assertEquals(format.getExtension(), extension);
    }


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
                {"/folder/example.fastq"},
                {"/folder/example.fq"},
                {"/folder/example.fastq.gz"},
                {"/folder/example.fq.gz"},
                {"folder/example.fastq"},
                {"folder/example.fq"},
                {"folder/example.fastq.gz"},
                {"folder/example.fq.gz"},
                {"/folder/example.fastq"},
                {"file:///folder/example.fq"},
                {"file:///folder/example.fastq.gz"},
                {"file:///folder/example.fq.gz"},
        };
    }

    @Test(dataProvider = "bamFiles")
    public void testIsSamBamOrCram(final String fileName) throws Exception {
        Assert.assertTrue(ReadToolsIOFormat.isSamBamOrCram(fileName));
    }

    @Test(dataProvider = "fastqFiles")
    public void testIsFastq(final String fileName) throws Exception {
        Assert.assertTrue(ReadToolsIOFormat.isFastq(fileName));
    }

    @Test(dataProvider = "fastqFiles")
    public void testNotIsSamBamOrCram(final String fileName) throws Exception {
        Assert.assertFalse(ReadToolsIOFormat.isSamBamOrCram(fileName));
    }

    @Test(dataProvider = "bamFiles")
    public void testNotIsFastq(final String fileName) throws Exception {
        Assert.assertFalse(ReadToolsIOFormat.isFastq(fileName));
    }
}