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
package org.vetmeduni.readtools;

import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.StringUtil;
import org.vetmeduni.tools.Tool;
import org.vetmeduni.tools.ToolNames;
import org.vetmeduni.tools.implemented.TrimFastq;
import org.vetmeduni.utils.TimeWatch;

import java.io.PrintWriter;
import java.util.Arrays;

import static org.vetmeduni.tools.ToolNames.ToolException;

/**
 * Class that contain the caller for the main functions
 *
 * @author Daniel Gómez-Sánchez
 */
public class Main {

	// the logger for this class
	public static Log logger = Log.getInstance(Main.class);

	/**
	 * Main method
	 *
	 * @param args the args for the command line
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			generalHelp("");
		} else if (args[0].equals("--debug")) {
			Log.setGlobalLogLevel(Log.LogLevel.DEBUG);
			logger.debug("DEBUG mode on");
			if (args.length == 1) {
				logger.debug("Debug mode only works with a tool");
				System.exit(1);
			}
			args = Arrays.copyOfRange(args, 1, args.length);
		} else {
			Log.setGlobalLogLevel(Log.LogLevel.INFO);
		}
		try {
			Tool toRun = ToolNames.getTool(args[0]);
			TimeWatch elapsed = TimeWatch.start();
			int exitStatus = toRun.run(Arrays.copyOfRange(args, 1, args.length));
			switch (exitStatus) {
				case 0:
					logger.info("Elapsed time for ", toRun.getClass().getSimpleName(), ": ", elapsed);
					break;
				case 1:
					logger.info("Finishing with errors");
					logger.debug("Elapsed time to error: ", elapsed);
					break;
				default:
					logger.error("Unexpected error. Please contact with ", ProjectProperties.getContact());
			}
			System.exit(exitStatus);
		} catch (ToolException e) {
			logger.debug(e.getMessage());
			logger.debug(e);
			generalHelp("Tool '" + args[0] + "' does not exists");
		}
	}

	/**
	 * Print the program header to the standard error
	 */
	public static void printProgramHeader() {
		String header = String.format("%s (compiled on %s)", ProjectProperties.getFormattedNameWithVersion(),
			ProjectProperties.getTimestamp());
		System.err.println(header);
		System.err.println(StringUtil.repeatCharNTimes('=', header.length()));
	}

	/**
	 * Print the program header to this print writer
	 */
	public static void printProgramHeader(PrintWriter writer) {
		String header = String.format("%s (compiled on %s)", ProjectProperties.getFormattedNameWithVersion(),
			ProjectProperties.getTimestamp());
		writer.println(header);
		writer.println(StringUtil.repeatCharNTimes('=', header.length()));
	}

	/**
	 * Get the usage of the main jar
	 *
	 * @return formatted usage
	 */
	public static String usageMain() {
		return String.format("Usage: java -jar %s.jar", ProjectProperties.getName());
	}

	/**
	 * Print the general help in the standard error
	 *
	 * @param error the standard error
	 */
	public static void generalHelp(String error) {
		printProgramHeader();
		System.err.println();
		System.err.println(usageMain() + " <tool> [options]");
		System.err.println();
		System.err.println("Tools:");
		for (ToolNames name : ToolNames.values()) {
			System.err.println("\t" + name + ":\t" + name.shortDescription);
		}
		if (!error.equals("")) {
			System.err.println();
			System.err.println("error: " + error);
		}
		System.err.println();
		System.exit(1);
	}
}
