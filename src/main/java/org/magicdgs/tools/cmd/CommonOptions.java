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

import htsjdk.samtools.util.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.magicdgs.tools.ToolNames;

import static org.magicdgs.tools.cmd.OptionUtils.getUniqueValue;

/**
 * Class that contains static instances of common options and their checking
 *
 * @author Daniel Gómez-Sánchez
 */
public class CommonOptions {

	/**
	 * Default number of threads for multi-threaded input
	 */
	public static final int DEFAULT_THREADS = 1;

	/**
	 * Option for maintain the format instead of standardize
	 */
	public static final Option maintainFormat = Option.builder("nstd").longOpt("no-standardize-output").desc(
		"By default, the output of this program is encoding in Sanger. If you disable this behaviour, the format of the output will be the same as the input (not recommended)")
													  .hasArg(false).optionalArg(true).build();

	/**
	 * Option for disable zipped output for FASTQ outputs
	 */
	public static final Option disableZippedOutput = Option.builder("dgz").longOpt("disable-zipped-output")
														   .desc("Disable zipped output").hasArg(false)
														   .optionalArg(true).build();

	/**
	 * Opton for allow higher qualities in sanger
	 */
	public static final Option allowHigherSangerQualities = Option.builder("ahq").longOpt("allow-higher-qualities")
																.desc("Allow higher qualities for Standard encoding")
																.hasArg(false).optionalArg(true).build();

	/**
	 * Option for parallelization. Currently is not really multi-thread
	 */
	// TODO: change the description when real multi-thread
	public static final Option parallel = Option.builder("nt").longOpt("number-of-thread").desc(
		"Specified the number of threads to use. Warning: real multi-thread is not implemented; if using more than one thread the option is a switch and the number of threads depends on the number of outputs. [Default="
			+ DEFAULT_THREADS + "]").hasArg().numberOfArgs(1).argName("INT").optionalArg(true).build();

	/**
	 * Check if the command line provides the maintain format option and log into the logger
	 *
	 * @param logger the logger where output the information
	 * @param cmd    the command line where check if the option is set
	 *
	 * @return <code>true</code> if the format is maintained; <code>false</code> if it should be standardize
	 */
	public static boolean isMaintained(Log logger, CommandLine cmd) {
		if (cmd.hasOption(maintainFormat.getOpt())) {
			logger.warn("Output will not be standardize. Does not provide the option -", maintainFormat.getOpt(),
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
		if(cmd.hasOption(allowHigherSangerQualities.getOpt())) {
			logger.warn("Standard qualities higher than specifications will be allowed. Does not provide the option -",
					allowHigherSangerQualities.getOpt(), " to avoid this behaviour");
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
		return cmd.hasOption(disableZippedOutput.getOpt());
	}

	/**
	 * Get the default number of threads if the command line does not contain the parallel option; if it is contain,
	 * parse the command line and return the number of threads asked for
	 *
	 * @param cmd the command line where check if the option is set
	 *
	 * @return the number of threads to use
	 * @throws org.magicdgs.tools.ToolNames.ToolException if the option is not numeric
	 */
	public static int numberOfThreads(Log logger, CommandLine cmd) {
		try {
			int nThreads = DEFAULT_THREADS;
			if (cmd.hasOption(parallel.getOpt())) {
				nThreads = Integer.parseInt(getUniqueValue(cmd, parallel.getOpt()));
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
			throw new ToolNames.ToolException("--" + parallel.getLongOpt() + " should be a positive integer");
		}
	}
}
