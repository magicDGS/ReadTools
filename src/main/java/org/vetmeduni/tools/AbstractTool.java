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
import org.apache.commons.cli.*;
import org.vetmeduni.readtools.Main;
import org.vetmeduni.readtools.ProjectProperties;

import java.io.PrintWriter;
import java.util.Arrays;

import static org.vetmeduni.tools.ToolNames.ToolException;

/**
 * Abstract tool that provides default help and parsing functions
 *
 * @author Daniel Gómez-Sánchez
 */
public abstract class AbstractTool implements Tool {

	/**
	 * Function that returns the options for the program
	 *
	 * @return the program options
	 */
	protected abstract Options programOptions();

	/**
	 * Help option (common for all the tools)
	 */
	private static final Options helpOption = new Options().addOption(Option.builder("h").
		longOpt("help").desc("Print this message and exit").build());

	/**
	 * The logger to use in the tools
	 */
	protected final Log logger;

	/**
	 * Constructor for the abstract tool that start the logger
	 */
	public AbstractTool() {
		logger = Log.getInstance(this.getClass());
	}

	/**
	 * Log the command line
	 *
	 * @param args the arguments passed to the tool
	 */
	protected void logCmdLine(String[] args) {
		String cmdLine = getCommandArguments(args);
		logger.info("Running ", this.getClass().getSimpleName(), " with arguments: ", cmdLine);
	}

	/**
	 * Get the command line arguments separated by space
	 *
	 * @param args the arguments passed to the tool
	 *
	 * @return the formatted string
	 */
	protected String getCommandArguments(String[] args) {
		StringBuilder cmdLine = new StringBuilder();
		for (String ar : args) {
			cmdLine.append(ar);
			cmdLine.append(" ");
		}
		return cmdLine.toString();
	}

	/**
	 * Output to System.err the help for this tool (with the full description)
	 */
	protected void help() {
		ToolNames tool = ToolNames.valueOf(this.getClass().getSimpleName());
		HelpFormatter formatter = new HelpFormatter();
		PrintWriter writer = new PrintWriter(System.err);
		Main.printProgramHeader(writer);
		writer.println();
		writer.println(String.format("%s: %s", tool, tool.shortDescription));
		writer.println("---");
		formatter.printWrapped(writer, formatter.getWidth(), tool.fullDescription);
		writer.println("---\n");
		formatter.printHelp(writer, formatter.getWidth(), usage(), "\n", programOptions(), formatter.getLeftPadding(),
			formatter.getDescPadding(), "", true);
		writer.close();
	}

	/**
	 * Get the usage for this tool (without arguments)
	 *
	 * @return the usage for Main following by the tool name
	 */
	protected String usage() {
		return Main.usageMain() + " " + this.getClass().getSimpleName();
	}

	/**
	 * Print the usage with an error message
	 *
	 * @param error the error message
	 */
	protected void printUsage(String error) {
		HelpFormatter formatter = new HelpFormatter();
		PrintWriter writer = new PrintWriter(System.err);
		formatter.printUsage(writer, formatter.getWidth(), usage(), programOptions());
		writer.println("error: " + error);
		writer.close();
	}

	/**
	 * Get an int array from the string formatted version retrived from the command line
	 *
	 * @param options      the options retrived from the command line
	 * @param defaultValue the default value(s)
	 *
	 * @return the default values if the options retrieved are <code>null</code>; the formatted int array from the
	 * string one
	 * @throws ToolException if some of the options cannot be parsed to an int
	 */
	public int[] getIntArrayOptions(String[] options, int... defaultValue) throws ToolException {
		if (options == null) {
			return defaultValue;
		}
		try {
			return Arrays.stream(options).mapToInt(Integer::parseInt).toArray();
		} catch (IllegalArgumentException e) {
			logger
				.debug("Trying to obtain integer(s) from the following parameters provided ", Arrays.toString(options));
			throw new ToolException("This option should be an integer");
		}
	}

	/**
	 * Parse the command line to obtain the arguments
	 *
	 * @param args the arguments passed to the tool
	 *
	 * @return the command line representation
	 */
	public CommandLine programParser(String[] args) {
		try {
			CommandLine hasHelp = new DefaultParser().parse(helpOption, args, true);
			if (hasHelp.hasOption("h")) {
				help();
			}
			CommandLine command = new DefaultParser().parse(programOptions(), args);
			logger.info(ProjectProperties.getFormattedNameWithVersion());
			return command;
		} catch (ParseException exp) {
			printUsage(exp.getMessage());
		}
		return null;
	}
}
