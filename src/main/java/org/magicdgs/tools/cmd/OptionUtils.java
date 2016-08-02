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
 */
package org.magicdgs.tools.cmd;

import org.magicdgs.tools.ToolNames;

import org.apache.commons.cli.CommandLine;

import java.util.Arrays;

/**
 * Utils for parse options
 *
 * @author Daniel Gómez-Sánchez
 */
public class OptionUtils {

    /**
     * Parse a single Integer option (only one value is allowed)
     *
     * @param cmd    the already parsed command line with the programParser
     * @param option the option to retrieve
     *
     * @return the value for that option; <code>null</code> if the option is not provided in the cmd
     *
     * @throws org.magicdgs.tools.ToolNames.ToolException if the argument was passed more than one
     *                                                    time
     */
    public static int[] getIntArrayOptions(CommandLine cmd, String option)
            throws ToolNames.ToolException {
        final String[] options = cmd.getOptionValues(option);
        if (options == null) {
            return null;
        }
        try {
            return Arrays.stream(options).mapToInt(Integer::parseInt).toArray();
        } catch (IllegalArgumentException e) {
            throw new ToolNames.ToolException("Option --" + option + " should be an integer");
        }
    }

    /**
     * Parse a single Integer option (only one value is allowed)
     *
     * @param cmd          the already parsed command line with the programParser
     * @param option       the option to retrieve
     * @param defaultValue default value if the option is not provided in the cmd
     *
     * @return the value for that option; or defaultValue if the option is not provided in the cmd
     *
     * @throws org.magicdgs.tools.ToolNames.ToolException if the argument was passed more than one
     *                                                    time
     */
    public static int[] getIntArrayOptions(CommandLine cmd, String option, int... defaultValue)
            throws ToolNames.ToolException {
        final int[] toReturn = getIntArrayOptions(cmd, option);
        return (toReturn == null) ? defaultValue : toReturn;
    }

    /**
     * Parse a single Integer option (only one value is allowed)
     *
     * @param cmd    the already parsed command line with the programParser
     * @param option the option to retrieve
     *
     * @return the value for that option; <code>null</code> if the option is not provided in the cmd
     *
     * @throws org.magicdgs.tools.ToolNames.ToolException if the argument was passed more than one
     *                                                    time
     */
    public static Integer getUniqueIntOption(CommandLine cmd, String option)
            throws ToolNames.ToolException {
        String value = getUniqueValue(cmd, option);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ToolNames.ToolException("Option --" + option + "should be an integer");
        }
    }

    /**
     * Parse a single option (only one value is allowed)
     *
     * @param cmd    the already parsed command line with the programParser
     * @param option the option to retrieve
     *
     * @return the value for that option; <code>null</code> if the option is not provided in the cmd
     *
     * @throws org.magicdgs.tools.ToolNames.ToolException if the argument was passed more than one
     *                                                    time
     */
    public static String getUniqueValue(CommandLine cmd, String option)
            throws ToolNames.ToolException {
        String[] toReturn = cmd.getOptionValues(option);
        if (toReturn == null) {
            return null;
        } else if (toReturn.length == 1) {
            return toReturn[0];
        }
        throw new ToolNames.ToolException("Option --" + option + " provided more than one time");
    }
}
