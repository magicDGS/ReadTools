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

import java.io.File;

/**
 * Class for utilities for retrieve test resources.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class TestResourcesUtils {

    // current directory for the tests
    private static final String CURRENT_DIRECTORY = System.getProperty("user.dir");

    /** The root file directory for main resources files. */
    public static final String READTOOLS_MAIN_RESOURCES_DIRECTORY =
            new File(CURRENT_DIRECTORY, "src/main/resources").getAbsolutePath() + "/";

    /** The root file directory for test resource files. */
    public static final String READTOOLS_TEST_ROOT_FILE_DIRECTORY =
            new File(CURRENT_DIRECTORY, "src/test/resources").getAbsolutePath() + "/";

    /**
     * Directory for example data used in the Walkthrough. This data belongs to the documentation
     * and cannot be change easily except in case of data corruption and/or bump in version to solve
     * a bug.
     *
     * <p>The tools and engine should work on this data without any modification, because they are
     * suppose to represent the same data. If a modification its required, that means one of the
     * following and it should be handled as following:
     * <ul>
     * <li>
     * ReadTools framework cannot roundtrip the files due to lossy formats. The files were
     * created to be aware of this flaw of formats, in the minimum expression, so modification due
     * to this issue should be done only if it is strictly necessary. Otherwise, tests should
     * require a different dataset to test this functionality.
     * For example, the SAM header in FASTQ cannot be encoded, and thus output files from the same
     * input with different headers will fail validation. Thus, all the headers are simple in the
     * SAM-formatted files.
     * </li>
     * <li>
     * Corrupted data. Some of the data may be outdated or may have problems with further
     * development. If that is the case, this represents a backwards incompatible change and the
     * tests involving this files should be disabled in favor of new tests files. With version
     * updates, the tests files may be modified and tests re-enabled.
     * For instance, if a SAM-formatted file requires a 'queryname' ordering and the files in the
     * current folder are 'unsorted', this will break functionality.
     * </li>
     * <li>
     * Functionality is broken and requires a workaround in the tests files until it is fixed. This
     * is completely discouraged and this kind of change in the test data may be accepted only in
     * extremealy rare cases.
     * </li>
     * </ul>
     */
    public static final String READTOOLS_WALKTHROUGH_DATA_DIRECTORY =
            new File(CURRENT_DIRECTORY, "/docs/walkthrough/data").getAbsolutePath() + "/";

    /**
     * The directory for example data.
     *
     * @deprecated use {@link #READTOOLS_WALKTHROUGH_DATA_DIRECTORY}, which contains the new common
     * data.
     */
    @Deprecated
    public static final String READTOOLS_EXAMPLE_DATA_DIRECTORY =
            new File(CURRENT_DIRECTORY, "/testdata").getAbsolutePath() + "/";

    /**
     * Gets the test resource as a file in the test source directory.
     */
    public static File getReadToolsTestResource(final String fileName) {
        return new File(READTOOLS_TEST_ROOT_FILE_DIRECTORY, fileName);
    }

    /**
     * Gets a file in the test data folder. This data represents formats handled in ReadTools.
     *
     * @deprecated use {@link #getWalkthroughDataFile(String)} instead.
     */
    @Deprecated
    public static File getExampleDataFile(final String fileName) {
        return new File(READTOOLS_EXAMPLE_DATA_DIRECTORY, fileName);
    }

    /**
     * Gets a file in the example data folder (used in the Walkthrough). See the documentation
     * of {@link #READTOOLS_WALKTHROUGH_DATA_DIRECTORY} for information about this test data.
     */
    public static File getWalkthroughDataFile(final String fileName) {
        return new File(READTOOLS_WALKTHROUGH_DATA_DIRECTORY, fileName);
    }

}
