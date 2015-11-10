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

import htsjdk.samtools.*;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.ProgressLogger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.vetmeduni.io.readers.SamReaderSanger;
import org.vetmeduni.io.writers.PairFastqWriters;
import org.vetmeduni.methods.barcodes.BarcodeDictionary;
import org.vetmeduni.methods.barcodes.BarcodeMethods;
import org.vetmeduni.tools.AbstractTool;
import org.vetmeduni.tools.CommonOptions;
import org.vetmeduni.utils.IOUtils;
import org.vetmeduni.utils.fastq.ProgressLoggerExtension;
import org.vetmeduni.utils.record.SAMRecordUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Class for converting from a Barcoded BAM to a FASTQ
 *
 * TODO: documentation
 *
 * TODO: implement splitting
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodedBamToFastq extends AbstractTool {

	private static final int DEFAULT_MISMATCHES = 0;

	@Override
	public int run(String[] args) {
		try {
			// parsing command line
			CommandLine cmd = programParser(args);
			String inputString = cmd.getOptionValue("i");
			String outputPrefix = cmd.getOptionValue("o");
			String barcodes = cmd.getOptionValue("bc");
			int[] max = getMaxMismatchesFromOption(cmd.getOptionValues("m"));
			String[] tags = cmd.getOptionValues("t");
			logger.debug("Maximum mistmaches (", max.length, "): ", max);
			logger.debug("Tags (", tags.length, "): ", tags);
			if (max.length != 1 && max.length != tags.length) {
				throw new ParseException("Number of maximum mismatches provided and number of tags does not match");
			}
			// FINISH PARSING: log the command line (not longer in the param file)
			logCmdLine(args);
			// open the barcode dictionary
			BarcodeDictionary barcodeDict = new BarcodeDictionary(new File(barcodes), tags.length);
			// and create the methods
			BarcodeMethods methods = new BarcodeMethods(barcodeDict);
			// open the bam file
			SamReader input;
			// if the format is maintained
			if (CommonOptions.isMaintained(logger, cmd)) {
				// if the format is maintained, create a default sam reader
				input = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT)
										.open(new File(inputString));
			} else {
				// if not, standardize
				input = new SamReaderSanger(new File(inputString), ValidationStringency.SILENT);
			}
			// single end processing
			if (cmd.hasOption("s")) {
				runSingle(input, outputPrefix, !(cmd.hasOption(CommonOptions.disableZippedOutput.getOpt())), methods, max, tags);
			} else {
				runPaired(input, outputPrefix, !(cmd.hasOption(CommonOptions.disableZippedOutput.getOpt())), methods, max, tags);
			}
			// close the readers and writers
			input.close();
		} catch (ParseException e) {
			// This exceptions comes from the command line parsing (I think so)
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

	private void runPaired(SamReader reader, String outputPrefix, boolean gzip, BarcodeMethods methods, int[] max,
		String[] tags) {
		PairFastqWriters writers = new PairFastqWriters(outputPrefix, gzip);
		PairFastqWriters discarded = new PairFastqWriters(String.format("%s_discarded", outputPrefix), gzip);
		SAMRecordIterator it = reader.iterator();
		ProgressLoggerExtension progress = new ProgressLoggerExtension(logger, 1000000, "Processed", "pairs");
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
				discarded
					.writePairs(SAMRecordUtils.toFastqRecord(record1, 1), SAMRecordUtils.toFastqRecord(record2, 2));
				unknown++;
			} else {
				SAMRecordUtils.addBarcodeToName(record1, best);
				SAMRecordUtils.addBarcodeToName(record2, best);
				writers.writePairs(SAMRecordUtils.toFastqRecord(record1, 1), SAMRecordUtils.toFastqRecord(record2, 2));
			}
			progress.record(record1);
		}
		logger.info(progress.numberOfVariantsProcessed());
		BarcodeDictionary dict = methods.getDictionary();
		logger.info("Found ", unknown, " pairs with unknown barcodes");
		for (int i = 0; i < dict.numberOfSamples(); i++) {
			logger.info("Found ", dict.getValueFor(i), " pairs for ", dict.getSampleNames().get(i), " (",
				dict.getCombinedBarcodesFor(i), ")");
		}
		writers.close();
		discarded.close();
	}

	private void runSingle(SamReader reader, String outputPrefix, boolean gzip, BarcodeMethods methods, int[] max,
		String[] tags) {
		FastqWriterFactory factory = new FastqWriterFactory();
		FastqWriter writer = factory.newWriter(new File(IOUtils.makeInputFastqWithDefaults(outputPrefix, gzip)));
		FastqWriter discarded = factory
			.newWriter(new File(IOUtils.makeInputFastqWithDefaults(outputPrefix + "_discarded", gzip)));
		ProgressLogger progress = new ProgressLogger(logger);
		int unknown = 0;
		for (SAMRecord record : reader) {
			String[] barcodes = getBarcodeFromTags(record, tags);
			String best = methods.getBestBarcode(max, barcodes);
			if (best.equals(BarcodeMethods.UNKNOWN_STRING)) {
				SAMRecordUtils.addBarcodeToName(record, String.join("", barcodes));
				discarded.write(SAMRecordUtils.toFastqRecord(record, null));
				unknown++;
			} else {
				SAMRecordUtils.addBarcodeToName(record, best);
				writer.write(SAMRecordUtils.toFastqRecord(record, null));
			}
			progress.record(record);
		}
		BarcodeDictionary dict = methods.getDictionary();
		logger.info("Found ", unknown, " records with unknown barcodes");
		for (int i = 0; i < dict.numberOfSamples(); i++) {
			logger.info("Found ", dict.getValueFor(i), " records for ", dict.getSampleNames().get(i), " (",
				dict.getCombinedBarcodesFor(i), ")");
		}
		writer.close();
		discarded.close();
	}

	private static String[] getBarcodeFromTags(SAMRecord record, String... tags) {
		String[] toReturn = new String[tags.length];
		for (int i = 0; i < tags.length; i++) {
			toReturn[i] = getBarcodeFromTag(record, tags[i]);
		}
		return toReturn;
	}

	private static String getBarcodeFromTag(SAMRecord record, String tag) {
		String barcode = record.getStringAttribute(tag);
		if (barcode == null) {
			throw new SAMException(tag + " not found in record " + record);
		}
		return barcode;
	}

	/**
	 * Get the maximum number of mismatches from the options
	 *
	 * @param options the options as a string
	 *
	 * @return the options converted in int
	 * @throws ParseException if the string does not contain ints
	 */
	private int[] getMaxMismatchesFromOption(String[] options) throws ParseException {
		if (options == null) {
			return new int[] {DEFAULT_MISMATCHES};
		}
		try {
			return Arrays.stream(options).mapToInt(Integer::parseInt).toArray();
		} catch (IllegalArgumentException e) {
			throw new ParseException("Maximum number of mismatches should be integer(s)");
		}
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
				+ DEFAULT_MISMATCHES + "]").hasArg().numberOfArgs(1).argName("INT").required(false).build();
		Option single = Option.builder("s").longOpt("single").desc("Switch to single-end parsing").hasArg(false)
							  .required(false).build();
		Options options = new Options();
		options.addOption(single);
		options.addOption(tag);
		options.addOption(barcodes);
		options.addOption(max);
		options.addOption(output);
		options.addOption(input);
		// add common options
		options.addOption(CommonOptions.maintainFormat); // mantain the format
		options.addOption(CommonOptions.disableZippedOutput); // disable zipped output
		// options.addOption(CommonOptions.parallel); // parallelization allowed
		return options;
	}
}
