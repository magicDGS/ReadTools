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
import org.broadinstitute.hellbender.exceptions.UserException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class Main extends org.broadinstitute.hellbender.Main {

    /** Only includes the org.magicdgs.readtools.tools package */
    protected List<String> getPackageList() {
        final List<String> packageList = new ArrayList<>();
        packageList.add("org.magicdgs.readtools.tools");
        return packageList;
    }

    /** Note: currently no single class is included. */
    protected List<Class<? extends CommandLineProgram>> getClassList() {
        // TODO: explore other tools from the GATK4 framework that may be useful for our toolkit
        return Collections.emptyList();
    }

    /**
     * Entry point for ReadTools.
     *
     * Note: several things change from the GATK4 framework main method.
     * 1) The returned result is printed without anything else (if QUIET, only the
     * `Object.toString()` method will be used.
     * 2) No stack-trace is printed for UserExceptions.
     * 3) If a no UserException is found, output a short notice to contact the developer.
     */
    public static void main(final String[] args) {
        try {
            Object result = new Main().instanceMain(args);
            if (result != null) {
                // TODO: print something else apart of the result
                System.out.println(result);
            }
        } catch (final UserException.CommandLineException e) {
            // the GATK framework should prints the error already
            System.exit(1);
        } catch (final UserException e) {
            // this prints the error for user exceptions
            CommandLineProgram.printDecoratedUserExceptionMessage(System.err, e);
            System.exit(2);
        } catch (final Exception e) {
            printUnexpectedError(e);
            System.exit(3);
        }
    }

    /**
     * Run the Main class from the GATK4 framework using our custom packages and classes.
     */
    public Object instanceMain(final String[] args) {
        return instanceMain(args, getPackageList(), getClassList(), "java -jar ReadTools.jar");
    }

    /** Prints in {@link System#err} log information for unexpected error. */
    private static void printUnexpectedError(final Exception e) {
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
