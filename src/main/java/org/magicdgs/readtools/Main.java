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

package org.magicdgs.readtools;

import htsjdk.samtools.util.Log;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main class for ReadTools, with several differences from GATK4:
 *
 * - Only includes tools in org.magicdgs.readtools.tools package.
 * - Command line for help shows "java -jar ReadTools.jar".
 * - Result for tools are printed directly, without decoration.
 * - If a non-user exception occurs, the program prints an unexpected error to contact developer
 * (and the stack trace if DEBUG is enabled).
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class Main extends org.broadinstitute.hellbender.Main {

    /** Only includes the org.magicdgs.readtools.tools package. */
    @Override
    protected List<String> getPackageList() {
        final List<String> packageList = new ArrayList<>();
        packageList.add("org.magicdgs.readtools.tools");
        return packageList;
    }

    /** Note: currently no single class is included. */
    @Override
    protected List<Class<? extends CommandLineProgram>> getClassList() {
        // TODO: explore other tools from the GATK4 framework that may be useful for our toolkit
        return Collections.emptyList();
    }

    /** Returns the command line that will appear in the usage. */
    protected String getCommandLineName() {
        return "java -jar ReadTools.jar";
    }

    /** Command line entry point. */
    public static void main(final String[] args) {
        new Main().mainEntry(args);
    }

    /** Prints the result to the standard output directly. */
    @Override
    protected void handleResult(final Object result) {
        // TODO: print something else and/or handle metrics?
        if (result != null) {
            System.out.println(result);
        }
    }

    /** Prints in {@link System#err} log information for unexpected error. */
    @Override
    protected void handleNonUserException(final Exception e) {
        System.err
                .println("***********************************************************************");
        System.err.println();
        System.err.println("UNEXPECTED ERROR: " + e.getMessage());
        System.err.println("Please, contact " + ProjectProperties.getContact());
        System.err.println();
        System.err
                .println("***********************************************************************");
        // only log the stack-trace for DEBUG mode
        if (Log.isEnabled(Log.LogLevel.DEBUG)) {
            e.printStackTrace();
        }
    }

}
