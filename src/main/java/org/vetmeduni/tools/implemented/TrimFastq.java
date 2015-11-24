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
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.io.readers.FastqReaderInterface;
import org.vetmeduni.io.readers.paired.FastqReaderPairedInterface;
import org.vetmeduni.io.readers.single.FastqReaderSingleInterface;
import org.vetmeduni.io.writers.ReadToolsFastqWriter;
import org.vetmeduni.io.writers.SplitFastqWriter;
import org.vetmeduni.methods.barcodes.dictionary.MatcherBarcodeDictionary;
import org.vetmeduni.methods.trimming.trimmers.Trimmer;
import org.vetmeduni.tools.AbstractTool;
import org.vetmeduni.tools.cmd.CommonOptions;
import org.vetmeduni.tools.cmd.ToolWritersFactory;
import org.vetmeduni.tools.cmd.ToolsReadersFactory;
import org.vetmeduni.utils.loggers.FastqLogger;

import java.io.File;
import java.io.IOException;

import static org.vetmeduni.tools.ToolNames.ToolException;

/**
 * Class that implements the trimming algorithm from Kofler et al. 2011
 *
 * @author Daniel Gómez-Sánchez
 */
public class TrimFastq extends AbstractTool {

	/**
	 * The default quality score
	 */
	private static final int DEFAULT_QUALTITY_SCORE = 20;

	/**
	 * The default minimum length
	 */
	private static final int DEFAULT_MINIMUM_LENGTH = 40;

	@Override
	public int run(String[] args) {
		try {
			// PARSING THE COMMAND LINE
			CommandLine cmd = programParser(args);
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
				throw new ToolException("Quality threshold should be a positive integer");
			}
			// minimum length
			int minLength;
			try {
				minLength = (cmd.hasOption("m")) ? Integer.parseInt(cmd.getOptionValue("m")) : DEFAULT_MINIMUM_LENGTH;
				if (minLength < 1) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				throw new ToolException("Minimum length should be a positive integer");
			}
			boolean discardRemainingNs = cmd.hasOption("discard-internal-N");
			boolean trimQuality = !cmd.hasOption("no-trim-quality");
			boolean no5ptrim = cmd.hasOption("no-5p-trim");
			// TODO: add again verbose for something?
			// boolean verbose = !cmd.hasOption("quiet");
			// FINISH PARSING: log the command line (not longer in the param file)
			logCmdLine(args);
			// save the gzip option
			boolean dgzip = CommonOptions.isZipDisable(cmd);
			// save the keep_discard option
			boolean keepDiscard = cmd.hasOption("k");
			// start the new factory
			// multi-thread?
			int nThreads = CommonOptions.numberOfThreads(logger, cmd);
			boolean multi = (nThreads != 1);
			// save the maintained format option
			boolean isMaintained = CommonOptions.isMaintained(logger, cmd);
			// open the reader
			FastqReaderInterface reader = ToolsReadersFactory.getFastqReaderFromInputs(input1, input2, isMaintained);
			boolean single = !(reader instanceof FastqReaderPairedInterface);
			// open the writer
			ReadToolsFastqWriter writer = (keepDiscard) ?
				ToolWritersFactory.getFastqSplitWritersFromInput(output_prefix, null, dgzip, multi, single) :
				ToolWritersFactory.getSingleOrPairWriter(output_prefix, dgzip, multi, single);
			// create the trimmer
			// create the MottAlgorithm
			Trimmer trimmer = Trimmer
				.getTrimmer(trimQuality, qualThreshold, minLength, discardRemainingNs, no5ptrim, single);
			// run it!
			process(trimmer, reader, writer, new File(output_prefix + ".metrics"));
		} catch (ToolException e) {
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
	 * Process the data depending on the reader status (if it is for single or pair-end
	 *
	 * @param trimmer     the algorithm with the provided settings
	 * @param reader      the reader, either for pairs or single end
	 * @param writer      the writer for the pairs
	 * @param metricsFile the file to output the metrics for the trimming
	 *
	 * @throws IOException if there are problems with the files
	 */
	private void process(Trimmer trimmer, FastqReaderInterface reader, ReadToolsFastqWriter writer, File metricsFile)
		throws IOException {
		FastqLogger progress;
		if (reader instanceof FastqReaderSingleInterface) {
			logger.debug("Running single end");
			progress = new FastqLogger(logger, 1000000, "Processed", "read-pairs");
			processSE(trimmer, (FastqReaderSingleInterface) reader, writer, progress);
		} else if (reader instanceof FastqReaderPairedInterface) {
			logger.debug("Running paired end");
			progress = new FastqLogger(logger);
			processPE(trimmer, (FastqReaderPairedInterface) reader, writer, progress);
		} else {
			logger.debug("ERROR: FastqReaderInterface is not an instance of Single or Paired interfaces");
			throw new IllegalArgumentException("Unreachable code");
		}
		// final line of progress
		progress.logNumberOfVariantsProcessed();
		// print the metrics file
		trimmer.printTrimmerMetrics(metricsFile);
		// close the readers
		reader.close();
		writer.close();
	}

	/**
	 * Process the files in pair-end mode
	 *
	 * @param trimmer the algorithm with the provided settings
	 * @param reader  the reader for the pairs
	 * @param writer  the writer for the pairs (instance of PairFastqWriters)
	 *
	 * @throws IOException if there are problems with the files
	 */
	private static void processPE(Trimmer trimmer, FastqReaderPairedInterface reader, ReadToolsFastqWriter writer,
		FastqLogger progress) throws IOException {
		boolean keep = (writer instanceof SplitFastqWriter);
		while (reader.hasNext()) {
			FastqPairedRecord record = reader.next();
			FastqPairedRecord newRecord = trimmer.trimFastqPairedRecord(record, reader.getFastqQuality());
			if (newRecord.isComplete()) {
				writer.write(newRecord);
			} else if (newRecord.containRecords()) {
				if (newRecord.getRecord1() == null) {
					writer.write(newRecord.getRecord2());
					if (keep) {
						((SplitFastqWriter) writer).write(MatcherBarcodeDictionary.UNKNOWN_STRING, record.getRecord1());
					}
				} else {
					writer.write(newRecord.getRecord1());
					if (keep) {
						((SplitFastqWriter) writer).write(MatcherBarcodeDictionary.UNKNOWN_STRING, record.getRecord2());
					}
				}
			} else {
				if (keep) {
					((SplitFastqWriter) writer).write(MatcherBarcodeDictionary.UNKNOWN_STRING, record);
				}
			}
			progress.add();
		}
	}

	/**
	 * Process the files in single-end mode
	 *
	 * @param trimmer the algorithm with the provided settings
	 * @param reader  the reader for the single end file
	 * @param writer  the writer for the single end file * @param metricsFile
	 *
	 * @throws IOException if there are problems with the files
	 */
	private static void processSE(Trimmer trimmer, FastqReaderSingleInterface reader, ReadToolsFastqWriter writer,
		FastqLogger progress) throws IOException {
		boolean keep = (writer instanceof SplitFastqWriter);
		while (reader.hasNext()) {
			FastqRecord record = reader.next();
			FastqRecord newRecord = trimmer.trimFastqRecord(record, reader.getFastqQuality());
			if (newRecord != null) {
				writer.write(newRecord);
			} else {
				if (keep) {
					((SplitFastqWriter) writer).write(MatcherBarcodeDictionary.UNKNOWN_STRING, record);
				}
			}
			progress.add();
		}
	}

	@Override
	protected Options programOptions() {
		// TODO: add option for store discarded
		// Creating each options
		Option input1 = Option.builder("i1").longOpt("input1")
							  .desc("The FASTQ input file, or the input file of the first read").hasArg()
							  .numberOfArgs(1).argName("input_1.fq").required(true).build();
		Option input2 = Option.builder("i2").longOpt("input2").desc(
			"The FASTQ input file of the second read. In case this file is provided the software will switch to paired read mode instead of single read mode")
							  .hasArg().numberOfArgs(1).argName("input_2.fq").optionalArg(true).build();
		Option output = Option.builder("o").longOpt("output")
							  .desc("The output file prefix. Will be in fastq. Mandatory parameter").hasArg()
							  .numberOfArgs(1).argName("output_prefix").required(true).build();
		Option quality_threshold = Option.builder("q").longOpt("quality-threshold").desc(
			"Minimum average quality. A modified Mott algorithm is used for trimming, and the threshold is used for calculating a score: quality_at_base - threshold. [Default="
				+ DEFAULT_QUALTITY_SCORE + "]").hasArg().numberOfArgs(1).argName("INT").optionalArg(true).build();
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
		Option keep_discard = Option.builder("k").longOpt("keep-discarded").desc(
			"NOT IMPLEMENTED YET: Keep the reads completely trimmed or that does not pass the thresholds in a discarded file (original reads stored)")
									.hasArg(false).optionalArg(true).build();
		// REMOVED THE QUIET OPTION
		// Option quiet = Option.builder("s").longOpt("quiet").desc("Suppress output to console").optionalArg(false)
		//					 .build();
		Options options = new Options();
		// options.addOption(quiet);
		options.addOption(keep_discard);
		options.addOption(no_5p_trim);
		options.addOption(no_trim_qual);
		options.addOption(min_length);
		options.addOption(discard_internal_N);
		options.addOption(quality_threshold);
		options.addOption(output);
		options.addOption(input2);
		options.addOption(input1);
		// adding common options
		options.addOption(CommonOptions.maintainFormat); // maintain the format
		options.addOption(CommonOptions.disableZippedOutput); // disable the zipped output
		options.addOption(CommonOptions.parallel); // parallelization allowed
		return options;
	}
}
