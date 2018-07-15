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

package org.magicdgs.readtools;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.Log;
import org.apache.commons.io.output.NullOutputStream;
import org.broadinstitute.hellbender.utils.LoggingUtils;
import org.broadinstitute.hellbender.utils.test.BaseTest;
import org.broadinstitute.hellbender.utils.text.XReadLines;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * All tests for ReadTools should extend this class. It contains utilities for log results, and
 * return information about the tested class.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTBaseTest extends BaseTest {

    /** Print stream used for tests which requires it, such as CLP parsing. */
    public static final PrintStream NULL_PRINT_STREAM = new PrintStream(new NullOutputStream());

    /** All the tests will have only the error verbosity. */
    @BeforeSuite
    @Override
    public void setTestVerbosity() {
        LoggingUtils.setLoggingLevel(Log.LogLevel.ERROR);
    }

    /** Gets the file in the class test directory. */
    public final File getClassTestDirectory() {
        return TestResourcesUtils.getReadToolsTestResource(
                getClass().getPackage().getName().replace(".", "/") + "/" + getTestedClassName());
    }

    @Override
    public final String getToolTestDataDir() {
        return getClassTestDirectory().toString();
    }

    /** Asserts that a file (compressed or not) is empty. */
    public static void assertFileIsEmpty(final File expectedEmptyFile) {
        try (final XReadLines lines = new XReadLines(expectedEmptyFile)) {
            if (lines.hasNext()) {
                Assert.fail("File is not empty: " + expectedEmptyFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Assert.fail("Failed with IO error: " + e.getMessage());
        }
    }

    /** Asserts that a SAM/BAM file is empty (may contain a header). */
    public static void assertEmptySamFile(final File samFile) {
        try (final SamReader reader = SamReaderFactory.makeDefault().open(samFile)) {
            Assert.assertFalse(reader.iterator().hasNext());
        } catch (final IOException e) {
            Assert.fail("Failed with IO error: " + e.getMessage());
        }
    }
}
