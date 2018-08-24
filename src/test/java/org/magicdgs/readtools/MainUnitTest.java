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

import htsjdk.samtools.util.Log;
import org.broadinstitute.hellbender.utils.LoggingUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class MainUnitTest extends RTBaseTest {

    @DataProvider(name = "resultsToHandle")
    public Object[][] getResults() {
        return new Object[][] {
                // no result
                {null, ""},
                // string results
                {"exampleResult", "exampleResult\n"},
                {"null", "null\n"},
                // numeric results
                {1, "1\n"}
        };
    }

    @Test(dataProvider = "resultsToHandle")
    public void testHandleResult(final Object result, final String printedResult) throws Exception {
        // capture the standard output
        final String actualOutput = captureStdout(() -> new Main().handleResult(result));
        // check that it is the same
        Assert.assertEquals(actualOutput, printedResult);
    }

    @Test
    public void testHandleUserException() throws Exception {
        final String message = "this is a test user exception";
        // capture the standard error
        final String exceptionOutput = captureStderr(
                () -> new Main().handleUserException(new RuntimeException(message)));

        // should contain the following strings
        Assert.assertTrue(exceptionOutput.contains("A USER ERROR has occurred: "));
        Assert.assertTrue((exceptionOutput.contains(message)));
    }

    @Test
    public void testHandleNonUserException() throws Exception {
        final String message = "this is a test exception";
        // capture the standard error
        final String exceptionOutput = captureStderr(
                () -> new Main().handleNonUserException(new RuntimeException(message))
        );
        // should contain the following strings
        Assert.assertTrue(exceptionOutput.contains("UNEXPECTED ERROR"));
        Assert.assertTrue(exceptionOutput.contains(message));
        Assert.assertTrue(exceptionOutput.contains("issue tracker"));
        // should contain also the stacktrace:
        // - the fully qualified name for the exception itself
        Assert.assertTrue(exceptionOutput.contains(RuntimeException.class.getCanonicalName()));
        // - the fully qualified name for the Main class
        Assert.assertTrue(exceptionOutput.contains(Main.class.getCanonicalName()));
    }

    @DataProvider(name = "printVersionArgs")
    public Object[][] getArgumentsForPrintOnlyVersionTest() {
        return new Object[][] {
                {new String[] {"--version"}, true},
                {new String[] {"-v"}, true},
                {new String[] {"ToolName"}, false},
                {new String[] {"--version", "--other"}, false},
                {new String[] {"-v", "--other"}, false},
                {new String[] {"ToolName", "--version"}, false}
        };
    }

    @Test(dataProvider = "printVersionArgs")
    public void testPrintOnlyVersion(final String[] args, final boolean printed) throws Exception {
        Assert.assertEquals(new Main().printOnlyVersion(args), printed);
    }


    @Test(singleThreaded = true)
    public void testPrintStackTrace() throws Exception {
        try {
            // set to debug mode an try to print the stack-trace
            LoggingUtils.setLoggingLevel(Log.LogLevel.DEBUG);
            // capture the standard error
            final String stdError = captureStderr(
                    () -> new Main().printStackTrace(new RuntimeException()));
            // assert non-empty stack-trace message
            Assert.assertFalse(stdError.isEmpty(), "empty stderr");
        } finally {
            // set back to normal verbosity
            setTestVerbosity();
        }
    }
}
