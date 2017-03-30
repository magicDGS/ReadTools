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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for command line program testing.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class CommandLineProgramTest extends BaseTest implements CommandLineProgramTester {

    /** Test FASTQ file (pair 1). */
    public static final File SMALL_FASTQ_1 = getInputDataFile("SRR1931701_1.fq");
    /** Test FASTQ file (pair 2). */
    public static final File SMALL_FASTQ_2 = getInputDataFile("SRR1931701_2.fq");
    /** Test BAM file (paired). */
    public static final File PAIRED_BAM_FILE = getInputDataFile("SRR1931701.tagged.sam");
    /** Test BAM file (single). */
    public static final File SINGLE_BAM_FILE = getInputDataFile("SRR1931701.single.tagged.sam");

    /**
     * Gets input data in the test directory.
     *
     * @deprecated use directly {@link TestResourcesUtils#getReadToolsTestResource(String)}.
     */
    @Deprecated
    public static File getInputDataFile(final String fileName) {
        return TestResourcesUtils.getReadToolsTestResource("org/magicdgs/readtools/data/" + fileName);
    }

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
}
