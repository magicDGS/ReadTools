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
 * SOFTWARE.
 */

package org.magicdgs.readtools.tools.quality;

import org.magicdgs.readtools.utils.tests.CommandLineProgramTest;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class StandardizeQualityIntegrationTest extends CommandLineProgramTest {

    private final static File TEST_TEMP_DIR =
            createTestTempDir(StandardizeQualityIntegrationTest.class.getSimpleName());

    @Test(expectedExceptions = UserException.BadArgumentValue.class)
    public void testBadArgument() throws Exception {
        runCommandLine(new ArgumentsBuilder()
                .addArgument("input", getInputDataFile("small.illumina.sam").getAbsolutePath())
                .addArgument("output", "bad.fastq")
                .addBooleanArgument("non-standardize-output", true));
    }

    @Test(expectedExceptions = UserException.BadInput.class)
    public void testBadFile() throws Exception {
        runCommandLine(
                Arrays.asList("-i", SMALL_FASTQ_1.getAbsolutePath(), "-o", "bad.fastq"));
    }

    @Test
    public void testStandardizeQualityBam() throws Exception {
        final File input = getInputDataFile("small.illumina.sam");
        final File tempOutput = new File(TEST_TEMP_DIR, "test.bam");
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("input", input.getAbsolutePath())
                .addArgument("output", tempOutput.getAbsolutePath());
        // run the command line
        runCommandLine(args);
        final SamReader actual = SamReaderFactory.makeDefault().open(tempOutput);
        final SAMRecordIterator expected =
                SamReaderFactory.makeDefault().open(SINGLE_BAM_FILE).iterator();
        for (final SAMRecord record : actual) {
            final SAMRecord expectedRecord = expected.next();
            // check just the read names and qualities
            Assert.assertEquals(record.getReadName(), expectedRecord.getReadName(),
                    "broken test files");
            Assert.assertEquals(record.getBaseQualities(), expectedRecord.getBaseQualities());
        }
        actual.close();
        expected.close();
    }

    @Test
    public void testStandardizeQualityFastq() throws Exception {
        final File input = getInputDataFile("small.illumina.fq");
        final File tempOutput = new File(TEST_TEMP_DIR, "test.fq");
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("input", input.getAbsolutePath())
                .addArgument("output", tempOutput.getAbsolutePath());
        // run the command line
        runCommandLine(args);
        final FastqReader actual = new FastqReader(tempOutput);
        final FastqReader expected = new FastqReader(SMALL_FASTQ_1);
        for (final FastqRecord record : actual) {
            final FastqRecord expectedRecord = expected.next();
            Assert.assertEquals(record, expectedRecord);
        }
        actual.close();
        expected.close();
    }

}
