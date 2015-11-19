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

import htsjdk.samtools.fastq.FastqRecord;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.io.readers.FastqReaderInterface;
import org.vetmeduni.io.readers.paired.FastqReaderPairedInterface;
import org.vetmeduni.io.readers.single.FastqReaderSingleInterface;
import org.vetmeduni.io.writers.SplitFastqWriter;
import org.vetmeduni.methods.barcodes.BarcodeDictionary;
import org.vetmeduni.methods.barcodes.BarcodeDictionaryFactory;
import org.vetmeduni.methods.barcodes.BarcodeMethods;
import org.vetmeduni.tools.AbstractTool;
import org.vetmeduni.tools.cmd.CommonOptions;
import org.vetmeduni.tools.cmd.ToolWritersFactory;
import org.vetmeduni.tools.cmd.ToolsReadersFactory;
import org.vetmeduni.utils.fastq.FastqLogger;
import org.vetmeduni.utils.record.FastqRecordUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.vetmeduni.tools.ToolNames.ToolException;

/**
 * Tool for split by barcode (in the read name) for FASTQ files
 *
 * @author Daniel Gómez-Sánchez
 */
public class FastqBarcodeDetector extends AbstractTool {

	@Override
	public int run(String[] args) {
		try {
			// PARSING THE COMMAND LINE
			CommandLine cmd = programParser(args);
			File input1 = new File(cmd.getOptionValue("input1"));
			File input2 = (cmd.hasOption("input2")) ? new File(cmd.getOptionValue("input2")) : null;
			String outputPrefix = cmd.getOptionValue("output");
			File barcodes = new File(cmd.getOptionValue("bc"));
			int max;
			try {
				max = (cmd.hasOption("m")) ?
					Integer.parseInt(cmd.getOptionValue("m")) :
					BarcodeMethods.DEFAULT_MISMATCHES;
			} catch (IllegalArgumentException e) {
				throw new ToolException("Maximum mismatches should be an integer");
			}
			int nThreads = CommonOptions.numberOfThreads(logger, cmd);
			boolean multi = nThreads != 1;
			boolean split = cmd.hasOption("x");
			// logging command line
			logCmdLine(args);
			// create the combined dictionary and the barcode method associated
			BarcodeDictionary dictionary = BarcodeDictionaryFactory.createCombinedDictionary(barcodes);
			logger.info("Loaded barcode file for ", dictionary.numberOfUniqueSamples(), " samples with ",
				dictionary.numberOfSamples(), " different barcode sets");
			BarcodeMethods methods = new BarcodeMethods(dictionary);
			// create the reader and the writer
			FastqReaderInterface reader = ToolsReadersFactory
				.getFastqReaderFromInputs(input1, input2, CommonOptions.isMaintained(logger, cmd));
			SplitFastqWriter writer = ToolWritersFactory.getFastqSplitWritersFromInput(outputPrefix, dictionary,
				cmd.hasOption(CommonOptions.disableZippedOutput.getOpt()), multi, input2 == null, split);
			// run the method
			run(reader, writer, methods, max);
		} catch (ToolException e) {
			// This exceptions comes from the command line parsing
			printUsage(e.getMessage());
			return 1;
		} catch (IOException e) {
			logger.debug(e);
			logger.error(e.getMessage());
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
	 * Run based on the reader the single or pair-end mode
	 *
	 * @param reader     the reader
	 * @param writer     the writer
	 * @param methods    the barcode methods instance
	 * @param mismatches the maximum number of mismatches
	 *
	 * @throws IOException if there are some problems with the files
	 */
	private void run(FastqReaderInterface reader, SplitFastqWriter writer, BarcodeMethods methods, int mismatches)
		throws IOException {
		FastqLogger progress = new FastqLogger(logger);
		int unknown;
		if (reader instanceof FastqReaderSingleInterface) {
			logger.debug("Running single end");
			unknown = runSingle((FastqReaderSingleInterface) reader, writer, methods, mismatches, progress);
		} else if (reader instanceof FastqReaderPairedInterface) {
			logger.debug("Running paired end");
			unknown = runPaired((FastqReaderPairedInterface) reader, writer, methods, mismatches, progress);
		} else {
			logger.debug("ERROR: FastqReaderInterface is not an instance of Single or Paired interfaces");
			throw new IllegalArgumentException("Unreachable code");
		}
		progress.logNumberOfVariantsProcessed();
		BarcodeDictionary dict = methods.getDictionary();
		logger.info("Found ", unknown, " records with unknown barcodes");
		for (int i = 0; i < dict.numberOfSamples(); i++) {
			logger.info("Found ", dict.getValueFor(i), " records for ", dict.getSampleNames().get(i), " (",
				dict.getCombinedBarcodesFor(i), ")");
		}
		writer.close();
		reader.close();
	}

	/**
	 * Run single-end mode
	 *
	 * @param reader     the reader
	 * @param writer     the writer
	 * @param methods    the barcode methods instance
	 * @param mismatches the maximum number of mismatches
	 *
	 * @return the number of unknown barcodes
	 * @throws IOException if there are some problems with the files
	 */
	private int runSingle(FastqReaderSingleInterface reader, SplitFastqWriter writer, BarcodeMethods methods,
		int mismatches, FastqLogger progress) throws IOException {
		int unknown = 0;
		Iterator<FastqRecord> it = reader.iterator();
		int[] mismatchesToPass = new int[] {mismatches};
		while (it.hasNext()) {
			FastqRecord record = it.next();
			String barcode = FastqRecordUtils.getBarcodeInName(record);
			String best = methods.getBestBarcode(mismatchesToPass, barcode);
			if (best.equals(BarcodeMethods.UNKNOWN_STRING)) {
				writer.write(best, record);
				unknown++;
			} else {
				writer.write(best, FastqRecordUtils.changeBarcodeInSingle(record, best));
			}
			progress.add();
		}
		return unknown;
	}

	/**
	 * Run pair-end mode
	 *
	 * @param reader     the reader
	 * @param writer     the writer
	 * @param methods    the barcode methods instance
	 * @param mismatches the maximum number of mismatches
	 *
	 * @return the number of unknow barcodes
	 * @throws IOException if there are some problems with the files
	 */
	private int runPaired(FastqReaderPairedInterface reader, SplitFastqWriter writer, BarcodeMethods methods,
		int mismatches, FastqLogger progress) throws IOException {
		int unknown = 0;
		Iterator<FastqPairedRecord> it = reader.iterator();
		int[] mismatchesToPass = new int[] {mismatches};
		while (it.hasNext()) {
			FastqPairedRecord record = it.next();
			String barcode = FastqRecordUtils.getBarcodeInName(record);
			String best = methods.getBestBarcode(mismatchesToPass, barcode);
			if (best.equals(BarcodeMethods.UNKNOWN_STRING)) {
				writer.write(best, record);
				unknown++;
			} else {
				writer.write(best, FastqRecordUtils.changeBarcodeInPaired(record, best));
			}
			progress.add();
		}
		return unknown;
	}

	@Override
	protected Options programOptions() {
		Option input1 = Option.builder("i1").longOpt("input1")
							  .desc("The input file, or the input file of the first read, in FASTQ format").hasArg()
							  .numberOfArgs(1).argName("input_1.fq").required(true).build();
		Option input2 = Option.builder("i2").longOpt("input2").desc(
			"The FASTQ input file of the second read. In case this file is provided the software will switch to paired read mode instead of single read mode")
							  .hasArg().numberOfArgs(1).argName("input_2.fq").optionalArg(true).build();
		Option output = Option.builder("o").longOpt("output").desc("The output file prefix").hasArg().numberOfArgs(1)
							  .argName("output_prefix").required(true).build();
		Option barcodes = Option.builder("bc").longOpt("barcodes").desc(
			"Tab-delimited file with the first column with the sample name and the following containing the barcodes (1 or 2 depending on the barcoding method; if two, they will be concatenated and assumed as 1).")
								.hasArg().numberOfArgs(1).argName("BARCODES.tab").required().build();
		// TODO: add to description: "It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file."
		// TODO: only if it could be implemented
		Option max = Option.builder("m").longOpt("maximum-mismatches").desc(
			"Maximum number of mismatches alowwed for a matched barcode.  [Default=" + BarcodeMethods.DEFAULT_MISMATCHES
				+ "]").hasArg().numberOfArgs(1).argName("INT").required(false).build();
		Option split = Option.builder("x").longOpt("split")
							 .desc("Split each sample from the barcode dictionary in a different file.").hasArg(false)
							 .required(false).build();
		//		// THIS ARE PREVIOUS OPTIONS IN THE METHOD THAT I DEVELOP OUTSIDE THIS TOOL: not longer supported!
		//		// this option was to allow a regular expression in the barcode name
		//		Option re = Option.builder("sx").longOpt("suffix")
		//			.desc("Regular expression for the suffix in the barcode. For instance, if the barcode is BARCODE_SEQUENCE, the regular expression should be \"_.*\" [default=null]")
		//			.hasArg().numberOfArgs(1).argName("REGEXP").required(false).build();
		//		// this option was because the pattern was only considering ATCGN in the barcode sequence. Now it it more flexible
		//		Option symbol = Option.builder("s").longOpt("symbol")
		//			.desc("The barcode contains symbols instead of only a sequence with ATCGN")
		//			.hasArg(false).numberOfArgs(1).required(false).build();
		// create the options
		Options options = new Options();
		// add the options
		options.addOption(input1);
		options.addOption(input2);
		options.addOption(output);
		options.addOption(barcodes);
		options.addOption(max);
		options.addOption(split);
		// default options
		// add common options
		options.addOption(CommonOptions.maintainFormat); // maintain the format
		options.addOption(CommonOptions.disableZippedOutput); // disable zipped output
		options.addOption(CommonOptions.parallel); // allow parallel output
		return options;
	}
}
