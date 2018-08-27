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

import org.magicdgs.readtools.cmd.RTDeprecatedToolsRegistry;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.util.Log;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;

import java.io.PrintStream;
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
public final class Main extends org.broadinstitute.hellbender.Main {

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
    @Override
    protected String getCommandLineName() {
        return "java -jar ReadTools.jar";
    }

    /** Command line entry point. */
    public static void main(final String[] args) {
        final Main main = new Main();
        // if only the --version / -v is requested, exit directly
        if (main.printOnlyVersion(args)) {
            System.exit(0);
        }
        new Main().mainEntry(args);
    }

    /**
     * Returns {@code true} if only --version / -v is requested after printing the version;
     * {@code false} otherwise.
     */
    @VisibleForTesting
    protected boolean printOnlyVersion(final String[] args) {
        if (args.length == 1 && ("--version".equals(args[0]) || "-v".equals(args[0]))) {
            handleResult(getClass().getPackage().getImplementationVersion());
            return true;
        }
        return false;
    }

    /** Prints the result to the standard output directly. */
    @Override
    protected void handleResult(final Object result) {
        // TODO: print something else and/or handle metrics?
        if (result != null) {
            System.out.println(result);
        }
    }

    /**
     * Prints in {@link System#err} the decorated exception as an user error.
     * In addition, prints the stack-trace if the debug mode is enabled.
     */
    @Override
    protected void handleUserException(Exception e) {
        printDecoratedExceptionMessage(System.err, e, "A USER ERROR has occurred: ");
        printStackTrace(e);
    }

    /**
     * Prints in {@link System#err} the decorated exception as an unexpected error.
     *
     * In addition, it adds a note pointing to the issue tracker
     * ({@link RTHelpConstants#ISSUE_TRACKER}) and prints the stack-trace if the debug mode is
     * enabled.
     */
    @Override
    protected void handleNonUserException(final Exception e) {
        printDecoratedExceptionMessage(System.err, e, "UNEXPECTED ERROR: ");
        e.printStackTrace();
        System.err.println("Please, search for this error in our issue tracker or post a new one:");
        System.err.println("\t" + RTHelpConstants.ISSUE_TRACKER);
    }

    /**
     * Prints the stack-trace into {@link System#err} only if
     * {@link htsjdk.samtools.util.Log.LogLevel#DEBUG} is enabled.
     */
    @VisibleForTesting
    protected final void printStackTrace(final Exception e) {
        if (Log.isEnabled(Log.LogLevel.DEBUG)) {
            e.printStackTrace();
        }
    }

    /**
     * Get deprecation message for a tool.
     *
     * @param toolName command specified by the user
     * @return deprecation message string, or null if none
     */
    @Override
    public String getToolDeprecationMessage(final String toolName) {
        return RTDeprecatedToolsRegistry.getToolDeprecationInfo(toolName);
    }
}
