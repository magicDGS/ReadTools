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

package org.magicdgs.readtools.utils.tests;

import java.io.File;

/**
 * Class for utilities for retrieve test resources.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class TestResourcesUtils {

    // current directory for the tests
    private static final String CURRENT_DIRECTORY = System.getProperty("user.dir");

    /** The root file directory for resource files. */
    public static final String READTOOLS_TEST_ROOT_FILE_DIRECTORY =
            new File(CURRENT_DIRECTORY, "src/test/resources").getAbsolutePath() + "/";

    /**
     * Gets the test resource as a file in the test source directory.
     */
    public static File getReadToolsTestResource(final String fileName) {
        return new File(READTOOLS_TEST_ROOT_FILE_DIRECTORY, fileName);
    }

}
