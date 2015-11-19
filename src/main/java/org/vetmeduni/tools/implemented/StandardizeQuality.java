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
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.vetmeduni.io.readers.SamReaderSanger;
import org.vetmeduni.io.readers.single.FastqReaderSingleInterface;
import org.vetmeduni.io.readers.single.FastqReaderSingleSanger;
import org.vetmeduni.tools.AbstractTool;
import org.vetmeduni.tools.cmd.CommonOptions;
import org.vetmeduni.utils.IOUtils;
import org.vetmeduni.utils.fastq.FastqLogger;
import org.vetmeduni.utils.fastq.ProgressLoggerExtension;
import org.vetmeduni.utils.fastq.QualityUtils;

import java.io.File;
import java.io.IOException;

import static org.vetmeduni.tools.ToolNames.ToolException;

/**
 * Class for converting from Illumina to Sanger encoding both FASTQ and BAM files
 *
 * @author Daniel Gómez-Sánchez
 */
public class StandardizeQuality extends AbstractTool {

	@Override
	public int run(String[] args) {
		try {
			CommandLine cmd = programParser(args);
			File input = new File(cmd.getOptionValue("i"));
			File output = new File(cmd.getOptionValue("o"));
			boolean index = cmd.hasOption("ind");
			logCmdLine(args);
			// first check the quality
			switch (QualityUtils.getFastqQualityFormat(input)) {
				case Standard:
					logger.error("File is already in Sanger formatting. No conversion will be performed");
					return 1;
				default:
					break;
			}
			int nThreads = CommonOptions.numberOfThreads(logger, cmd);
			boolean multi = nThreads != 1;
			if (IOUtils.isBamOrSam(input)) {
				runBam(input, output, index, multi);
			} else {
				if (index) {
					logger.warn("Index could not be performed for FASTQ file");
				}
				runFastq(input, output, multi);
			}
		} catch (ToolException e) {
			// This exceptions comes from the command line parsing
			printUsage(e.getMessage());
			return 1;
		} catch (IOException e) {
			logger.info(e.getMessage());
			logger.debug(e);
			return 1;
		} catch (Exception e) {
			logger.debug(e);
			return 2;
		}
		return 0;
	}

	/**
	 * Change the format in a Fastq file
	 *
	 * @param input  the input file
	 * @param output the output file
	 * @param multi  <code>true</code> if multi-thread output
	 *
	 * @throws IOException if there is some problem with the files
	 */
	private void runFastq(File input, File output, boolean multi) throws IOException {
		// open reader (directly converting)
		FastqReaderSingleInterface reader = new FastqReaderSingleSanger(input);
		// open factory for writer
		FastqWriterFactory factory = new FastqWriterFactory();
		factory.setUseAsyncIo(multi);
		// open writer
		FastqWriter writer = factory.newWriter(output);
		// start iterations
		FastqLogger progress = new FastqLogger(logger);
		for (FastqRecord record : reader) {
			writer.write(record);
			progress.add();
		}
		logger.info(progress.numberOfVariantsProcessed());
		reader.close();
		writer.close();
	}

	/**
	 * Change the format in a BAM file
	 *
	 * @param input  the input file
	 * @param output the output file
	 * @param index  <code>true</code> if index on the fly is requested
	 * @param multi  <code>true</code> if multi-thread output
	 *
	 * @throws IOException if there is some problem with the files
	 */
	private void runBam(File input, File output, boolean index, boolean multi) throws IOException {
		SamReader reader = new SamReaderSanger(input, ValidationStringency.SILENT);
		SAMFileWriter writer = new SAMFileWriterFactory().setCreateIndex(index).setUseAsyncIo(multi)
														 .makeSAMOrBAMWriter(reader.getFileHeader(),
															 SAMFileHeader.SortOrder.coordinate
																 .equals(reader.getFileHeader().getSortOrder()),
															 output);
		// start iterations
		ProgressLoggerExtension progress = new ProgressLoggerExtension(logger);
		for (SAMRecord record : reader) {
			writer.addAlignment(record);
			progress.record(record);
		}
		logger.info(progress.numberOfVariantsProcessed());
		reader.close();
		writer.close();
	}

	@Override
	protected Options programOptions() {
		Option input = Option.builder("i").longOpt("input").desc("Input BAM/FASTQ to standardize the quality").hasArg()
							 .numberOfArgs(1).argName("INPUT").required().build();
		Option output = Option.builder("o").longOpt("output").desc(
			"Output for the coverted file. The extension determine the format SAM/BAM or FASTQ/GZIP").hasArg()
							  .numberOfArgs(1).argName("OUTPUT").required().build();
		Option index = Option.builder("ind").longOpt("index").desc("If the output is a BAM file, index it")
							 .hasArg(false).required(false).build();
		Options options = new Options();
		options.addOption(input);
		options.addOption(output);
		options.addOption(index);
		// commmon options
		options.addOption(CommonOptions.parallel);
		return options;
	}
}
