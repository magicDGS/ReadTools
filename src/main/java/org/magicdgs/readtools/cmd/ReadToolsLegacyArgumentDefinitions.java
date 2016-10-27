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
package org.magicdgs.readtools.cmd;

import static org.magicdgs.readtools.tools.cmd.OptionUtils.getUniqueValue;

import org.magicdgs.readtools.tools.ToolNames;

import htsjdk.samtools.util.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * Class that contains static instances of common options and their checking
 *
 * @author Daniel Gómez-Sánchez
 */
public class ReadToolsLegacyArgumentDefinitions {

    public static final String INPUT_LONG_NAME = "input";
    public static final String INPUT_SHORT_NAME = "i";
    public static final String OUTPUT_LONG_NAME = "output";
    public static final String OUTPUT_SHORT_NAME = "o";

    // Option for maintain the format instead of standardize
    public static final String MAINTAIN_FORMAT_LONG_NAME = "non-standardize-output";
    public static final String MAINTAIN_FORMAT_SHORT_NAME = "nstd";
    public static final String MAINTAIN_FORMAT_DOC =
            "By default, the output of this program is encoding in Sanger. If you disable this behaviour, the format of the output will be the same as the input (not recommended)";

    // Option for disable zipped output

    public static final String DISABLE_ZIPPED_OUTPUT_LONG_NAME = "disable-zipped-output";
    public static final String DISABLE_ZIPPED_OUTPUT_SHORT_NAME = "dgz";
    public static final String DISABLE_ZIPPED_OUTPUT_DOC = "Disable zipped output";

    // Option for allow higher qualities in sanger
    public static final String ALLOW_HIGHER_SANGER_QUALITIES_LONG_NAME = "allow-higher-qualities";
    public static final String ALLOW_HIGHER_SANGER_QUALITIES_SHORT_NAME = "ahq";
    public static final String ALLOW_HIGHER_SANGER_QUALITIES_DOC =
            "Allow higher qualities for Standard encoding";

    /**
     * Default number of threads for multi-threaded input
     */
    public static final int DEFAULT_THREADS = 1;

    public static final String PARALLEL_LONG_NAME = "number-of-threads";
    public static final String PARALLEL_SHORT_NAME = "nt";
    public static final String PARALLEL_DOC =
            "Specified the number of threads to use. Warning: real multi-thread is not implemented; if using more than one thread the option is a switch and the number of threads depends on the number of outputs.";


    // legacy options for ReadTools read groups
    public static final String RG_ID_LONG_NAME = "run-id";
    public static final String RG_ID_SHORT_NAME = "run";
    public static final String RG_ID_DOC =
            "Run name to add to the ID in the read group information.";
    public static final String RG_PLATFORM_LONG_NAME = "platform";
    public static final String RG_PLATFORM_SHORT_NAME = "pl";
    public static final String RG_PLATFORM_DOC = "Platform to add to the Read Group information";
    public static final String RG_UNIT_LONG_NAME = "platform-unit";
    public static final String RG_UNIT_SHORT_NAME = "pu";
    public static final String RG_UNIT_DOC = "Platform Unit to add to the Read Group information.";

    /**
     * Option for maintain the format instead of standardize
     */
    public static final Option maintainFormat =
            Option.builder(MAINTAIN_FORMAT_SHORT_NAME)
                    .longOpt(MAINTAIN_FORMAT_LONG_NAME)
                    .desc(MAINTAIN_FORMAT_DOC)
                    .hasArg(false).optionalArg(true).build();

    /**
     * Option for disable zipped output for FASTQ outputs
     */
    public static final Option disableZippedOutput =
            Option.builder(DISABLE_ZIPPED_OUTPUT_SHORT_NAME)
                    .longOpt(DISABLE_ZIPPED_OUTPUT_LONG_NAME)
                    .desc(DISABLE_ZIPPED_OUTPUT_DOC)
                    .hasArg(false).optionalArg(true).build();

    /**
     * Opton for allow higher qualities in sanger
     */
    public static final Option allowHigherSangerQualities =
            Option.builder(ALLOW_HIGHER_SANGER_QUALITIES_SHORT_NAME)
                    .longOpt(ALLOW_HIGHER_SANGER_QUALITIES_LONG_NAME)
                    .desc(ALLOW_HIGHER_SANGER_QUALITIES_DOC)
                    .hasArg(false).optionalArg(true).build();

    /**
     * Option for parallelization. Currently is not really multi-thread
     */
    // TODO: change the description when real multi-thread
    public static final Option parallel = Option.builder(PARALLEL_SHORT_NAME)
            .longOpt(PARALLEL_LONG_NAME)
            .desc(PARALLEL_DOC + " [Default=" + DEFAULT_THREADS + "]")
            .hasArg().numberOfArgs(1).argName("INT").optionalArg(true).build();

    /**
     * Check if the command line provides the maintain format option and log into the logger
     *
     * @param logger the logger where output the information
     * @param cmd    the command line where check if the option is set
     *
     * @return <code>true</code> if the format is maintained; <code>false</code> if it should be
     * standardize
     */
    public static boolean isMaintained(Log logger, CommandLine cmd) {
        if (cmd.hasOption(MAINTAIN_FORMAT_SHORT_NAME)) {
            logger.warn("Output will not be standardize. Does not provide the option -",
                    MAINTAIN_FORMAT_SHORT_NAME,
                    " to avoid this behaviour");
            return true;
        } else {
            logger.info("Output will be in Sanger format independently of the input format");
            return false;
        }
    }

    /**
     * Check if the command line provides the higher qualities option and log into the logger
     *
     * @param logger the logger where output the information
     * @param cmd    the command line where check if the option is set
     *
     * @return <code>true</code> if higher qualities are allowed; <code>false</code> otherwise
     */
    public static boolean allowHigherQualities(final Log logger, final CommandLine cmd) {
        if (cmd.hasOption(ALLOW_HIGHER_SANGER_QUALITIES_SHORT_NAME)) {
            logger.warn(
                    "Standard qualities higher than specifications will be allowed. Does not provide the option -",
                    ALLOW_HIGHER_SANGER_QUALITIES_SHORT_NAME, " to avoid this behaviour");
            return true;
        }
        return false;
    }

    /**
     * Check if the command line provides an option for disable zipping
     *
     * @param cmd the command line where check if the option is set
     *
     * @return <code>true</code> if gzip is disable; <code>false</code> otherwise
     */
    public static boolean isZipDisable(CommandLine cmd) {
        return cmd.hasOption(DISABLE_ZIPPED_OUTPUT_SHORT_NAME);
    }

    /**
     * Get the default number of threads if the command line does not contain the parallel option;
     * if it is contain,
     * parse the command line and return the number of threads asked for
     *
     * @param cmd the command line where check if the option is set
     *
     * @return the number of threads to use
     *
     * @throws org.magicdgs.readtools.tools.ToolNames.ToolException if the option is not numeric
     */
    public static int numberOfThreads(Log logger, CommandLine cmd) {
        try {
            int nThreads = DEFAULT_THREADS;
            if (cmd.hasOption(PARALLEL_SHORT_NAME)) {
                nThreads = Integer.parseInt(getUniqueValue(cmd, PARALLEL_SHORT_NAME));
                if (nThreads != 1) {
                    // TODO: change when real multi-thread is implemented
                    logger.warn(
                            "Currently multi-threads does not control the number of threads in use, depends on the number of outputs");
                } else if (nThreads < 0) {
                    throw new NumberFormatException();
                }
            }
            return nThreads;
        } catch (NumberFormatException e) {
            throw new ToolNames.ToolException(
                    "--" + parallel.getLongOpt() + " should be a positive integer");
        }
    }
}
