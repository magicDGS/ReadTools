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
package org.vetmeduni.tools.implemented;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.util.FastqQualityFormat;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.vetmeduni.methods.trimming.MottAlgorithm;
import org.vetmeduni.tools.AbstractTool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.vetmeduni.utils.fastq.QualityUtils.getFastqQualityFormat;

/**
 * Class that implements the trimming algorithm from Kofler et al. 2011
 *
 * @author Daniel Gómez-Sánchez
 */
public class TrimFastq extends AbstractTool {

	/**
	 * The default quality score
	 */
	private static int DEFAULT_QUALTITY_SCORE = 20;

	/**
	 * The default minimum length
	 */
	private static int DEFAULT_MINIMUM_LENGTH = 40;

	/**
	 * The default number of threads
	 */
	private static int DEFAULT_THREADS = 1;

	@Override
	public int run(String[] args) {
		CommandLine cmd = programParser(args);
		try {
			// PARSING THE COMMAND LINE
			// The input file
			File input1 = new File(cmd.getOptionValue("input1"));
			// input file 2
			File input2 = (cmd.hasOption("input2")) ? new File(cmd.getOptionValue("input2")) : null;
			// The output prefix
			String output_prefix = cmd.getOptionValue("output");
			// qualityThreshold
			int qualThreshold;
			try {
				qualThreshold = (cmd.hasOption("quality-threshold")) ?
					Integer.parseInt(cmd.getOptionValue("quality-threshold")) :
					DEFAULT_QUALTITY_SCORE;
				if (qualThreshold < 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				throw new ParseException("Quality threshold should be a positive integer");
			}
			// minimum length
			int minLength;
			try {
				minLength = (cmd.hasOption("min-length")) ?
					Integer.parseInt(cmd.getOptionValue("min-length")) :
					DEFAULT_MINIMUM_LENGTH;
				if (minLength < 1) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				throw new ParseException("Minimum length should be a positive integer");
			}
			boolean discardRemainingNs = cmd.hasOption("discard-internal-N");
			boolean trimQuality = !cmd.hasOption("no-trim-quality");
			boolean no5ptrim = cmd.hasOption("no-5p-trim");
			boolean verbose = !cmd.hasOption("quiet");
			boolean gzip = !cmd.hasOption("disable-zipped-output");
			// multi-thread
			int multi = (cmd.hasOption("nt")) ? Integer.parseInt(cmd.getOptionValue("nt")) : DEFAULT_THREADS;
			// FINISH PARSING: log the command line (not longer in the param file)
			logCmdLine(args);
			// writing param file
			//			paramFile(input1, input2, output_prefix, qualThreshold, minLength, discardRemainingNs, trimQuality,
			//				no5ptrim, verbose, gzip, multi);
			// create the MottAlgorithm
			MottAlgorithm trimming = new MottAlgorithm(trimQuality, qualThreshold, minLength, discardRemainingNs,
				no5ptrim);
			// if input 2 process Pair end
			if (input2 != null) {
				logger.info("Found an existing file for the second read; Switching to paired-read mode");
				FastqQualityFormat encoding1 = getFastqQualityFormat(input1);
				FastqQualityFormat encoding2 = getFastqQualityFormat(input2);
				if (!encoding1.equals(encoding2)) {
					throw new SAMException("Pair-end encoding is different for both read pairs");
				} else {
					logger.info("Detected FASTQ format: ",
						(encoding1.equals(FastqQualityFormat.Standard)) ? "'sanger'" : "'illumina'");
				}
				trimming.processPE(input1, input2, output_prefix, encoding1, multi, verbose, logger, gzip);
				// if not, single end mode
			} else {
				logger.info("Did not find an existing file for the second read; Switching to single-read mode");
				FastqQualityFormat encoding = getFastqQualityFormat(input1);
				logger.info("Detected FASTQ format: ",
					(encoding.equals(FastqQualityFormat.Standard)) ? "'sanger'" : "'illumina'");
				trimming.processSE(input1, output_prefix, encoding, multi, verbose, logger, gzip);
			}
		} catch (ParseException e) {
			// This exceptions comes from the command line parsing
			printUsage(e.getMessage());
			return 1;
		} catch (IOException | SAMException e) {
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
	 * Write a param file with the inputs
	 *
	 * @param input1             the first input
	 * @param input2             the second input
	 * @param output_prefix      the output prefix
	 * @param qualThreshold      the quality threshold
	 * @param minLength          the minimum length
	 * @param discardRemainingNs discarting remaining Ns?
	 * @param trimQuality        trimming quality?
	 * @param no5ptrim           no trim 5 prime?
	 * @param verbose            should we be verbose?
	 * @param gzip               disable gzip output?
	 * @param parallel           number of threads?
	 *
	 * @throws IOException if there are some problems with the param file
	 */
	@Deprecated
	public static void paramFile(File input1, File input2, String output_prefix, int qualThreshold, int minLength,
		boolean discardRemainingNs, boolean trimQuality, boolean no5ptrim, boolean verbose, boolean gzip, int parallel)
		throws IOException {
		String param_file = output_prefix + ".params";
		PrintWriter param = new PrintWriter(new FileWriter(param_file), true);
		param.print("Using input1\t");
		param.println(input1);
		param.print("Using input2\t");
		param.println(input2);
		param.print("Using output\t");
		param.println(output_prefix);
		param.print("Using quality-threshold\t");
		param.println(qualThreshold);
		param.print("Using min-length\t");
		param.println(minLength);
		param.print("Using discard-internal-N\t");
		param.println(discardRemainingNs);
		param.print("Using trim-quality (no-trim-quality)\t");
		param.println(trimQuality);
		param.print("Using 5p-trim (no-5p-trim)\t");
		param.println(trimQuality);
		param.print("Using verbose (quiet)\t");
		param.println(verbose);
		param.print("Disable zipped output\t");
		param.println(!gzip);
		param.print("Number of threads\t");
		param.println(parallel);
		param.close();
	}

	@Override
	protected Options programOptions() {
		// Creating each options
		Option input1 = Option.builder("i1").longOpt("input1")
							  .desc("The input file, or the input file of the first read, in fastq format").hasArg()
							  .numberOfArgs(1).argName("input_1.fq").required(true).build();
		Option input2 = Option.builder("i2").longOpt("input2").desc(
			"The input file of the second read, in fastq format. In case this file is provided the software will switch to paired read mode instead of single read mode")
							  .hasArg().numberOfArgs(1).argName("input_2.fq").optionalArg(true).build();
		Option output = Option.builder("o").longOpt("output")
							  .desc("The output file prefix. Will be in fastq. Mandatory parameter").hasArg()
							  .numberOfArgs(1).argName("output_prefix").required(true).build();
		Option quality_threshold = Option.builder("q").longOpt("quality-threshold").desc(
			"Minimum average quality. A modified Mott algorithm is used for trimming, and the threshold is used for calculating a score: quality_at_base - threshold. [Default="
				+ DEFAULT_QUALTITY_SCORE + "]").hasArg().numberOfArgs(1).argName("INT").optionalArg(true).build();
		// TODO: remove this option and add one to switch off the quality conversion (by default it should be on)
		// Option fastq_type = Option.builder("fq").longOpt("fastq-type").desc(
		//	"The encoding of the quality characters; Must either be 'sanger' or 'illumina'. default=auto-detect")
		// 						  .hasArg().numberOfArgs(1).argName("illumina").optionalArg(true).build();
		Option discard_internal_N = Option.builder("N").longOpt("discard-internal-N")
										  .desc("If set reads having internal Ns will be discarded").hasArg(false)
										  .optionalArg(true).build();
		Option min_length = Option.builder("m").longOpt("minimum-length").desc(
			"The minimum length of the read after trimming. [Default=" + DEFAULT_MINIMUM_LENGTH + "]").hasArg()
								  .numberOfArgs(1).argName("INT").optionalArg(true).build();
		Option no_trim_qual = Option.builder("nq").longOpt("no-trim-quality").desc("Switch off quality trimming")
									.hasArg(false).optionalArg(false).build();
		Option no_5p_trim = Option.builder("n5p").longOpt("no-5p-trim").desc(
			"Disable 5'-trimming (quality and 'N'); May be useful for the identification of duplicates when using trimming of reads. Duplicates are usually identified by the 5' mapping position which should thus not be modified by trimming")
								  .hasArg(false).optionalArg(true).build();
		Option disable_zipped_output = Option.builder("dgz").longOpt("disable-zipped-output")
											 .desc("Dissable zipped output").hasArg(false).optionalArg(true).build();
		Option quiet = Option.builder("s").longOpt("quiet").desc("Suppress output to console").optionalArg(false)
							 .build();
		// TODO: implement maintain format
//		Option maintain_format = Option.builder("nstd").longOpt("no-standardize-output").desc(
//			"By default, the output of this program is encoding in Sanger. If you disable this behaviour, the format of the output will be the same as the input (not recommended)")
//									  .hasArg(false).optionalArg(true).build();
		//		Option parallel = Option.builder("nt").longOpt("number-of-thread")
		//								.desc("Specified the number of threads to use. [Default=" + DEFAULT_THREADS + "]")
		//								.hasArg().numberOfArgs(1).argName("INT").optionalArg(true).build();
		Options options = new Options();
		//		options.addOption(parallel);
		options.addOption(quiet);
		options.addOption(disable_zipped_output);
		options.addOption(no_5p_trim);
		options.addOption(no_trim_qual);
		options.addOption(min_length);
		options.addOption(discard_internal_N);
		// options.addOption(fastq_type);
		options.addOption(quality_threshold);
		options.addOption(output);
		options.addOption(input2);
		options.addOption(input1);
		return options;
	}
}
