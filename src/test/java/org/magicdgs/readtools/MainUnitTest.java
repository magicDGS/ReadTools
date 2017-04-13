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
        final Main main = new Main();
        try(final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final PrintStream printStream = new PrintStream(outputStream)) {
            main.resultOutput = printStream;
            main.handleResult(result);
            Assert.assertEquals(outputStream.toString(), printedResult);
        }
    }

    @Test
    public void testHandleNonUserException() throws Exception {
        final String message = "this is a test exception";
        final Main main = new Main();
        try(final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final PrintStream printStream = new PrintStream(outputStream)) {
            main.exceptionOutput = printStream;
            main.handleNonUserException(new RuntimeException(message));
            final String exceptionOutput = outputStream.toString();
            // should contain the following strings
            Assert.assertTrue(exceptionOutput.contains("UNEXPECTED ERROR"));
            Assert.assertTrue(exceptionOutput.contains(message));
            Assert.assertTrue(exceptionOutput.contains("issue tracker"));
        }
    }

    @DataProvider(name = "printVersionArgs")
    public Object[][] getArgumentsForPrintOnlyVersionTest() {
        return new Object[][] {
                {new String[]{"--version"}, true},
                {new String[]{"-v"}, true},
                {new String[]{"ToolName"}, false},
                {new String[]{"--version", "--other"}, false},
                {new String[]{"-v", "--other"}, false},
                {new String[]{"ToolName", "--version"}, false}
        };
    }

    @Test(dataProvider = "printVersionArgs")
    public void testPrintOnlyVersion(final String[] args, final boolean printed) throws Exception {
        final Main main = new Main();
        main.exceptionOutput = NULL_PRINT_STREAM;
        Assert.assertEquals(main.printOnlyVersion(args), printed);
    }
}
