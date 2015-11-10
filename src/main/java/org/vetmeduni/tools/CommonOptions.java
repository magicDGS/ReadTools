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
package org.vetmeduni.tools;

import htsjdk.samtools.util.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * Class that contains static instances of common options and their checking
 *
 * @author Daniel Gómez-Sánchez
 */
public class CommonOptions {

	public static final int DEFAULT_THREADS = 1;

	/**
	 * Option for maintain the format instead of standardize
	 */
	public static Option maintainFormat = Option.builder("nstd").longOpt("no-standardize-output").desc(
		"By default, the output of this program is encoding in Sanger. If you disable this behaviour, the format of the output will be the same as the input (not recommended)")
												.hasArg(false).optionalArg(true).build();

	/**
	 * Option for disable zipped output for FATSQ outputs
	 */
	public static Option disableZippedOutput = Option.builder("dgz").longOpt("disable-zipped-output")
													 .desc("Dissable zipped output").hasArg(false).optionalArg(true)
													 .build();

	/**
	 * Option for parallelization
	 */
	public static Option parallel = Option.builder("nt").longOpt("number-of-thread").desc(
		"Specified the number of threads to use. [Default=" + DEFAULT_THREADS + "]").hasArg().numberOfArgs(1)
										  .argName("INT").optionalArg(true).build();

	/**
	 * Check if the command line provides the maintain format option and log into the logger
	 *
	 * @param logger the logger where ouptut the information
	 * @param cmd    the command line where check if it the option is set
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
	 * Get the default number of threads if the command line does not contain the parallel option; if it is contain,
	 * parse the command line and return the number of threads asked for
	 *
	 * @param cmd the command line where check if it the option is set
	 *
	 * @return the number of threads to use
	 */
	public static int numberOfThreads(CommandLine cmd) {
		return (cmd.hasOption(parallel.getOpt())) ?
			Integer.parseInt(cmd.getOptionValue(parallel.getOpt())) :
			DEFAULT_THREADS;
	}
}
