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
package org.vetmeduni.tools.cmd;

import htsjdk.samtools.util.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionary;
import org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionaryFactory;
import org.vetmeduni.methods.barcodes.dictionary.decoder.BarcodeDecoder;
import org.vetmeduni.tools.ToolNames;

import java.io.File;
import java.io.IOException;

import static org.vetmeduni.tools.cmd.OptionUtils.getIntArrayOptions;
import static org.vetmeduni.tools.cmd.OptionUtils.getUniqueValue;

/**
 * Default options for barcode detectors
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeOptions {

	/**
	 * Default option for barcodes file (it is required)
	 */
	public static Option barcodes = Option.builder("bc").longOpt("barcodes").desc(
		"Tab-delimited file with the first column with the sample name and the following containing the barcodes (1 or 2 depending on the barcoding method)")
										  .hasArg().numberOfArgs(1).argName("BARCODES.tab").required().build();

	/**
	 * Option for maximum number of mismatches
	 */
	public static Option max = Option.builder("m").longOpt("maximum-mismatches").desc(
		"Maximum number of mismatches alowwed for a matched barcode. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file. [Default="
			+ BarcodeDecoder.DEFAULT_MAXIMUM_MISMATCHES + "]").hasArg().numberOfArgs(1).argName("INT").required(false)
									 .build();

	/**
	 * Option for minimum distance between matches in barcodes
	 */
	// TODO: add this option
	public static Option dist = Option.builder("d").longOpt("minimum-distance").desc(
		"Minimum distance between the best match and the second to consider a match. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file. [Default="
			+ BarcodeDecoder.DEFAULT_MIN_DIFFERENCE_WITH_SECOND + "]").hasArg().numberOfArgs(1).argName("INT")
									  .required(false).build();

	/**
	 * Option for does not count N as mismatches
	 */
	// TODO: add this option
	public static Option nNoMismatch = Option.builder("n").longOpt("n-no-mismatch").desc("Do not count Ns as mismatch")
											 .hasArg(false).required(false).build();

	/**
	 * Option for split the output by barcode
	 */
	public static Option split = Option.builder("x").longOpt("split")
									   .desc("Split each sample from the barcode dictionary in a different file.")
									   .hasArg(false).required(false).build();

	/**
	 * Get the barcode dictionary option using the command line
	 *
	 * @param logger the logger to log results
	 * @param cmd    the command line already parsed
	 * @param length the expected number of barcodes; <code>null</code> if combined
	 *
	 * @return the combined barcode
	 */
	public static BarcodeDictionary getBarcodeDictionaryFromOption(Log logger, CommandLine cmd, Integer length)
		throws IOException {
		final File inputFile = new File(getUniqueValue(cmd, barcodes.getOpt()));
		final BarcodeDictionary dictionary;
		if (length == null) {
			dictionary = BarcodeDictionaryFactory.createCombinedDictionary(inputFile);
			logger.info("Loaded barcode file for ", dictionary.numberOfUniqueSamples(), " samples with ",
				dictionary.numberOfSamples(), " different barcode sets");
		} else {
			dictionary = BarcodeDictionaryFactory.createDefaultDictionary(inputFile, length);
		}
		return dictionary;
	}

	/**
	 * Get a barcode decoder from the command line
	 *
	 * @param logger the logger to log results
	 * @param cmd    the command line already parsed
	 * @param length the expected number of barcodes; <code>null</code> if combined
	 *
	 * @return the barcode decoder
	 */
	public static BarcodeDecoder getBarcodeDecoderFromOption(Log logger, CommandLine cmd, Integer length)
		throws IOException, ToolNames.ToolException {
		try {
			BarcodeDictionary dictionary = getBarcodeDictionaryFromOption(logger, cmd, length);
			int[] mismatches = getIntArrayOptions(cmd, max.getOpt());
			int[] minDist = getIntArrayOptions(cmd, dist.getOpt());
			return new BarcodeDecoder(dictionary, !cmd.hasOption(nNoMismatch.getOpt()), mismatches, minDist);
		} catch (IllegalArgumentException e) {
			throw new ToolNames.ToolException("Number of barcodes and thresholds does not match");
		}
	}

	/**
	 * Check if the split output is set
	 *
	 * @return <code>true</code> if split; <code>false</code> otherwise
	 */
	public static boolean isSplit(Log logger, CommandLine cmd) {
		if (cmd.hasOption(split.getOpt())) {
			logger.info("Output will be splitted");
			return true;
		}
		return false;
	}
}
