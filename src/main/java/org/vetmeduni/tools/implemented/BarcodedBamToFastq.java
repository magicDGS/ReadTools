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
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.io.writers.SplitFastqWriter;
import org.vetmeduni.methods.barcodes.BarcodeDictionary;
import org.vetmeduni.methods.barcodes.BarcodeDictionaryFactory;
import org.vetmeduni.methods.barcodes.BarcodeMethods;
import org.vetmeduni.tools.AbstractTool;
import org.vetmeduni.tools.cmd.CommonOptions;
import org.vetmeduni.tools.cmd.ToolWritersFactory;
import org.vetmeduni.tools.cmd.ToolsReadersFactory;
import org.vetmeduni.utils.loggers.ProgressLoggerExtension;
import org.vetmeduni.utils.record.SAMRecordUtils;

import java.io.File;
import java.io.IOException;

import static org.vetmeduni.tools.ToolNames.ToolException;

/**
 * Class for converting from a Barcoded BAM to a FASTQ
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodedBamToFastq extends AbstractTool {

	@Override
	public int run(String[] args) {
		try {
			// parsing command line
			CommandLine cmd = programParser(args);
			String inputString = cmd.getOptionValue("i");
			String outputPrefix = cmd.getOptionValue("o");
			String barcodes = cmd.getOptionValue("bc");
			int[] max = getIntArrayOptions(cmd.getOptionValues("m"), BarcodeMethods.DEFAULT_MISMATCHES);
			String[] tags = cmd.getOptionValues("t");
			logger.debug("Maximum mistmaches (", max.length, "): ", max);
			logger.debug("Tags (", tags.length, "): ", tags);
			if (max.length != 1 && max.length != tags.length) {
				throw new ToolException("Number of maximum mismatches provided and number of tags does not match");
			}
			int nThreads = CommonOptions.numberOfThreads(logger, cmd);
			boolean multi = nThreads != 1;
			// FINISH PARSING: log the command line (not longer in the param file)
			logCmdLine(args);
			// open the barcode dictionary
			BarcodeDictionary barcodeDict = BarcodeDictionaryFactory
				.createDefaultDictionary(new File(barcodes), tags.length);
			logger.info("Loaded barcode file for ", barcodeDict.numberOfUniqueSamples(), " samples with ",
				barcodeDict.numberOfSamples(), " different barcode sets");
			BarcodeMethods methods = new BarcodeMethods(barcodeDict);
			// open the bam file
			SamReader input = ToolsReadersFactory
				.getSamReaderFromInput(new File(inputString), CommonOptions.isMaintained(logger, cmd));
			// Create the writer factory
			SplitFastqWriter writer = ToolWritersFactory.getFastqSplitWritersFromInput(outputPrefix, barcodeDict,
				cmd.hasOption(CommonOptions.disableZippedOutput.getOpt()), cmd.hasOption("s"), cmd.hasOption("x"),
				multi);
			// run it!
			run(input, writer, methods, max, tags, cmd.hasOption("s"));
			// close the readers and writers
			input.close();
			writer.close();
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
			// unknow exception
			logger.debug(e);
			return 2;
		}
		return 0;
	}

	/**
	 * Run with single or paired end
	 *
	 * @param reader  the input reader
	 * @param writer  the output
	 * @param methods the methods to use to split
	 * @param max     the maximum number of mismatches
	 * @param tags    the tags where the barcodes are
	 * @param single  it is single end?
	 */
	private void run(SamReader reader, SplitFastqWriter writer, BarcodeMethods methods, int[] max, String[] tags,
		boolean single) {
		ProgressLoggerExtension progress;
		int unknown;
		// single end processing
		if (single) {
			progress = new ProgressLoggerExtension(logger, 1000000, "Processed", "records");
			unknown = runSingle(reader, writer, methods, max, tags, progress);
		} else {
			progress = new ProgressLoggerExtension(logger, 1000000, "Processed", "pairs");
			unknown = runPaired(reader, writer, methods, max, tags, progress);
		}
		progress.logNumberOfVariantsProcessed();
		BarcodeDictionary dict = methods.getDictionary();
		logger.info("Found ", unknown, " records with unknown barcodes");
		for (int i = 0; i < dict.numberOfSamples(); i++) {
			logger.info("Found ", dict.getValueFor(i), " records for ", dict.getSampleNames().get(i), " (",
				dict.getCombinedBarcodesFor(i), ")");
		}
	}

	/**
	 * Run the pair-end mode
	 *
	 * @param reader  the input reader
	 * @param writer  the output
	 * @param methods the methods to use to split
	 * @param max     the maximum number of mismatches
	 * @param tags    the tags where the barcodes are
	 *
	 * @return the number of unknown barcodes
	 */
	private int runPaired(SamReader reader, SplitFastqWriter writer, BarcodeMethods methods, int[] max, String[] tags,
		ProgressLoggerExtension progress) {
		SAMRecordIterator it = reader.iterator();
		int unknown = 0;
		while (it.hasNext()) {
			SAMRecord record1 = it.next();
			if (!it.hasNext()) {
				throw new SAMException("Truncated interleaved BAM file");
			}
			SAMRecord record2 = it.next();
			String[] barcodes = getBarcodeFromTags(record1, tags);
			String best = methods.getBestBarcode(max, barcodes);
			if (best.equals(BarcodeMethods.UNKNOWN_STRING)) {
				SAMRecordUtils.addBarcodeToName(record1, String.join("", barcodes));
				SAMRecordUtils.addBarcodeToName(record2, String.join("", barcodes));
				unknown++;
			} else {
				SAMRecordUtils.addBarcodeToName(record1, best);
				SAMRecordUtils.addBarcodeToName(record2, best);
			}
			FastqPairedRecord outputRecord = new FastqPairedRecord(SAMRecordUtils.toFastqRecord(record1, 1),
				SAMRecordUtils.toFastqRecord(record2, 2));
			writer.write(best, outputRecord);
			progress.record(record1);
		}
		return unknown;
	}

	/**
	 * Run the single-end mode
	 *
	 * @param reader  the input reader
	 * @param writer  the output
	 * @param methods the methods to use to split
	 * @param max     the maximum number of mismatches
	 * @param tags    the tags where the barcodes are
	 *
	 * @return the number of unknown barcodes
	 */
	private int runSingle(SamReader reader, SplitFastqWriter writer, BarcodeMethods methods, int[] max, String[] tags,
		ProgressLoggerExtension progress) {
		int unknown = 0;
		for (SAMRecord record : reader) {
			String[] barcodes = getBarcodeFromTags(record, tags);
			String best = methods.getBestBarcode(max, barcodes);
			if (best.equals(BarcodeMethods.UNKNOWN_STRING)) {
				SAMRecordUtils.addBarcodeToName(record, String.join("", barcodes));
				unknown++;
			} else {
				SAMRecordUtils.addBarcodeToName(record, best);
			}
			writer.write(best, SAMRecordUtils.toFastqRecord(record, null));
			progress.record(record);
		}
		return unknown;
	}

	/**
	 * Get the all the barcodes from the provided tags
	 *
	 * @param record the record to extract the barcodes from
	 * @param tags   the tags where the barcodes are
	 *
	 * @return the barcodes in the order of the tags
	 */
	private static String[] getBarcodeFromTags(SAMRecord record, String... tags) {
		String[] toReturn = new String[tags.length];
		for (int i = 0; i < tags.length; i++) {
			toReturn[i] = getBarcodeFromTag(record, tags[i]);
		}
		return toReturn;
	}

	/**
	 * Get a barcode from an unique tag
	 *
	 * @param record the record to extract the barcodes from
	 * @param tag    the tag where the requested barcode is
	 *
	 * @return the barcode for this tag
	 * @throws htsjdk.samtools.SAMException if the barcode is not found
	 */
	private static String getBarcodeFromTag(SAMRecord record, String tag) {
		String barcode = record.getStringAttribute(tag);
		if (barcode == null) {
			throw new SAMException(tag + " not found in record " + record);
		}
		return barcode;
	}

	@Override
	protected Options programOptions() {
		Option input = Option.builder("i").longOpt("input")
							 .desc("Input BAM/SAM file. If pair-end, it should be interleaved").hasArg()
							 .argName("INPUT.bam").numberOfArgs(1).required().build();
		Option output = Option.builder("o").longOpt("output").desc("FASTQ output prefix").hasArg()
							  .argName("OUTPUT_PREFIX").numberOfArgs(1).required().build();
		Option tag = Option.builder("t").longOpt("tag").desc(
			"Tag in the BAM file for the stored barcodes. It should be provided the same number of times as barcodes provided in the file.")
						   .hasArg().numberOfArgs(1).argName("TAG").required().build();
		Option barcodes = Option.builder("bc").longOpt("barcodes").desc(
			"Tab-delimited file with the first column with the sample name and the following containing the barcodes (1 or 2 depending on the barcoding method)")
								.hasArg().numberOfArgs(1).argName("BARCODES.tab").required().build();
		Option max = Option.builder("m").longOpt("maximum-mismatches").desc(
			"Maximum number of mismatches alowwed for a matched barcode. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file. [Default="
				+ BarcodeMethods.DEFAULT_MISMATCHES + "]").hasArg().numberOfArgs(1).argName("INT").required(false)
						   .build();
		Option single = Option.builder("s").longOpt("single").desc("Switch to single-end parsing").hasArg(false)
							  .required(false).build();
		Option split = Option.builder("x").longOpt("split")
							 .desc("Split each sample from the barcode dictionary in a different file.").hasArg(false)
							 .required(false).build();
		Options options = new Options();
		options.addOption(single);
		options.addOption(tag);
		options.addOption(barcodes);
		options.addOption(max);
		options.addOption(output);
		options.addOption(input);
		options.addOption(split);
		// add common options
		options.addOption(CommonOptions.maintainFormat); // mantain the format
		options.addOption(CommonOptions.disableZippedOutput); // disable zipped output
		options.addOption(CommonOptions.parallel);
		return options;
	}
}
