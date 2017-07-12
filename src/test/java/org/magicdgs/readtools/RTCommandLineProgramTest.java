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

import org.broadinstitute.hellbender.utils.test.CommandLineProgramTester;
import org.broadinstitute.hellbender.utils.text.XReadLines;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for command line program testing.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class RTCommandLineProgramTest extends RTBaseTest implements CommandLineProgramTester {

    /** @return {@link #getTestedClassName()} */
    @Override
    public String getTestedToolName() {
        return getTestedClassName();
    }

    /** Use our main class. */
    @Override
    public Object runCommandLine(final List<String> args) {
        return new Main().instanceMain(makeCommandLineArgs(args));
    }

    /** Includes also setting QUIET=true if not present. */
    @Override
    public List<String> injectDefaultVerbosity(final List<String> args) {
        // call the super
        final List<String> verbArgs = CommandLineProgramTester.super.injectDefaultVerbosity(args);
        for (String arg : verbArgs) {
            if ("--QUIET".equals(arg)) {
                return verbArgs;
            }
        }
        final List<String> quietArgs = new ArrayList<>(verbArgs);
        quietArgs.add("--QUIET");
        return quietArgs;
    }

    // classes names for ReadTools are sited in this package
    private static final String METRIC_CLASS_HEADER = "# org.magicdgs.readtools";
    // this is the header for the date of analysis
    private static final String METRIC_START_HEADER = "# Started on:";

    /**
     * Tests concordance of metrics files. It handles the following special cases:
     *
     * - Header lines containing ReadTools class names at the beginning.
     * - Date of start of command line program.
     */
    public static void metricsFileConcordance(final File resultFile, final File expectedFile)
            throws IOException {
        try (final XReadLines actualReader = new XReadLines(resultFile);
                final XReadLines expectedReader = new XReadLines(expectedFile)) {
            final List<String> actualLines = actualReader.readLines();
            final List<String> expectedLines = expectedReader.readLines();
            //For ease of debugging, we look at the lines first and only then check their counts
            final int minLen = Math.min(actualLines.size(), expectedLines.size());
            for (int i = 0; i < minLen; i++) {
                final String actual = actualLines.get(i);
                final String expected = expectedLines.get(i);
                // handle the CMD line
                if (expected.startsWith(METRIC_CLASS_HEADER)) {
                    final int indexOfSpace = expected.indexOf(" ", METRIC_CLASS_HEADER.length());
                    final String expectedClassLine = (indexOfSpace == -1)
                            ? expected : expected.substring(0, indexOfSpace + 1);
                    Assert.assertTrue(actual.indexOf(expectedClassLine) != 1, String.format(
                            "Wrong class header at line %s: expected (partial) %s vs. %s",
                            i, expectedClassLine, actual));
                } else if (expected.startsWith(METRIC_START_HEADER)) {
                    Assert.assertTrue(actual.startsWith(METRIC_START_HEADER), String.format(
                            "Missing starting time header at line %s: expected %s vs. %s",
                            i, expected, actual));
                } else {
                    Assert.assertEquals(actualLines.get(i), expectedLines.get(i),
                            "Line number " + i + " (no special header line)");
                }
            }
            Assert.assertEquals(actualLines.size(), expectedLines.size(), "line counts");
        }
    }
}
