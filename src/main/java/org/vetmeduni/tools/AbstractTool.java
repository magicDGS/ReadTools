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

import htsjdk.samtools.SAMException;
import htsjdk.samtools.util.Log;
import org.apache.commons.cli.*;
import org.vetmeduni.readtools.Main;
import org.vetmeduni.readtools.ProjectProperties;

import java.io.IOException;
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
	 * Implementation that use {@link #runThrowingExceptions(org.apache.commons.cli.CommandLine)} and handle or the know
	 * exceptions: {@link org.vetmeduni.tools.ToolNames.ToolException} if there is some problem parsing the command
	 * line, {@link java.io.IOException} if there are problems with the files
	 *
	 * @param args arguments that comes directly from the command line and need to be parsed
	 *
	 * @return 0 if there is no exception, 1 if there is a known exception, 2 otherwise
	 */
	@Override
	public int run(String[] args) {
		try {
			final CommandLine cmd = programParser(args);
			runThrowingExceptions(cmd);
		} catch (ToolException e) {
			// This exceptions comes from the command line parsing
			printUsage(e.getMessage());
			return 1;
		} catch (SAMException | IOException e) {
			// this are expected errors: IO if the user provides bad inputs, SAM if there are problems in the files
			logger.error(e.getMessage());
			logger.debug(e);
			return 1;
		} catch (Exception e) {
			// unexpected exceptions return a different error code
			logger.debug(e);
			logger.error(e.getMessage());
			return 2;
		}
		return 0;
	}

	/**
	 * Run the current tool and throw exceptions that will be handle by {@link #run(String[])}. See {@link
	 * #run(String[])} for the exceptions that are handle
	 *
	 * @param cmd the already parsed command line with the programParser
	 *
	 * @throws Exception
	 */
	protected abstract void runThrowingExceptions(final CommandLine cmd) throws Exception;

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
	@Deprecated
	protected void logCmdLine(String[] args) {
		String cmdLine = getCommandArguments(args);
		logger.info("Running ", this.getClass().getSimpleName(), " with arguments: ", cmdLine);
	}

	/**
	 * Log the command line (with the known options)
	 *
	 * @param cmd the already parsed command line with the programParser
	 */
	protected void logCmdLine(CommandLine cmd) {
		StringBuilder builder = new StringBuilder("");
		for (Option opt : cmd.getOptions()) {
			builder.append("--");
			builder.append(opt.getLongOpt());
			builder.append(" ");
			if(opt.hasArg()) {
				builder.append((opt.getValues().length == 1) ? opt.getValue() : Arrays.toString(opt.getValues()));
			}
		}
		logger.info("Running ", this.getClass().getSimpleName(), " with arguments: ", builder.toString());
	}

	/**
	 * Get the command line arguments separated by space
	 *
	 * @param args the arguments passed to the tool
	 *
	 * @return the formatted string
	 */
	@Deprecated
	protected String getCommandArguments(String[] args) {
		StringBuilder cmdLine = new StringBuilder();
		for (String ar : args) {
			cmdLine.append(ar);
			cmdLine.append(" ");
		}
		return cmdLine.toString();
	}

	/**
	 * Parse a single option (only one value is allowed)
	 *
	 * @param cmd    the already parsed command line with the programParser
	 * @param option the option to retrieve
	 *
	 * @return the value for that option; <code>null</code> if the option is not provided in the cmd
	 * @throws ToolException if the argument was passed more than one time
	 */
	protected static String getUniqueValue(CommandLine cmd, String option) throws ToolException {
		String[] toReturn = cmd.getOptionValues(option);
		if (toReturn == null) {
			return null;
		} else if (toReturn.length == 1) {
			return toReturn[0];
		}
		throw new ToolException("Option --" + option + " provided more than one time");
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
