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

package org.magicdgs.readtools.utils.tests;

import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.testng.Reporter;

import java.io.File;

/**
 * All tests for ReadTools should extend this class. It contains utilities for log results, and
 * return information about the tested class.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BaseTest {

    /** Log this message so that it shows up inline during output as well as in html reports. */
    public static void log(final String message) {
        Reporter.log(message, true);
    }

    /**
     * Returns the name of the class (ClassName) being tested following the conventions:
     *
     * - ClassNameIntegrationTest
     * - ClassNameUnitTest
     * - ClassNameTest
     */
    public final String getTestedClassName() {
        if (getClass().getSimpleName().contains("IntegrationTest")) {
            return getClass().getSimpleName().replaceAll("IntegrationTest$", "");
        } else if (getClass().getSimpleName().contains("UnitTest")) {
            return getClass().getSimpleName().replaceAll("UnitTest$", "");
        } else {
            return getClass().getSimpleName().replaceAll("Test$", "");
        }
    }

    /** Gets the file in the class test directory. */
    public final File getClassTestDirectory() {
        return TestResourcesUtils.getReadToolsTestResource(
                getClass().getPackage().getName().replace(".", "/") + "/" + getTestedClassName());
    }

    /**
     * Creates a temp directory for tests, deleting recursively on exit.
     *
     * @param prefix the prefix for the test directory.
     *
     * @return temp directory file.
     */
    public static File createTestTempDir(final String prefix) {
        final File dir = IOUtils.tempDir(prefix, "");
        IOUtils.deleteRecursivelyOnExit(dir);
        return dir;
    }

    // TODO: include utility methods for assertions

}
