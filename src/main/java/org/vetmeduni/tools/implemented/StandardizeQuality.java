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
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.ProgressLogger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.vetmeduni.tools.AbstractTool;
import org.vetmeduni.utils.IOUtils;
import org.vetmeduni.utils.fastq.FastqLogger;
import org.vetmeduni.utils.fastq.QualityUtils;
import org.vetmeduni.utils.record.FastqRecordUtils;
import org.vetmeduni.utils.record.SAMRecordUtils;

import java.io.File;
import java.io.IOException;

/**
 * Class for converting from Illumina to Sanger encoding both FASTQ and BAM files
 *
 * TODO: document
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
			// first check the quality
			switch (QualityUtils.getFastqQualityFormat(input)) {
				case Standard:
					logger.error("File is already in Sanger formatting. No conversion will be performed");
					return 1;
				default:
					break;
			}
			if (IOUtils.isBamOrSam(input)) {
				runBam(input, output, index);
			} else {
				if (index) {
					logger.warn("Index could not be performed for FASTQ file");
				}
				runFastq(input, output, index);
			}
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

	private static void runBam(File input, File output, boolean index) throws IOException {
		// Open readers and writers
		SamReader reader = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT).open(input);
		SAMFileWriter writer = new SAMFileWriterFactory().setCreateIndex(index)
														 .makeSAMOrBAMWriter(reader.getFileHeader(),
															 reader.getFileHeader().getSortOrder()
																 == SAMFileHeader.SortOrder.coordinate, output);
		// start iterations
		ProgressLogger progress = new ProgressLogger(logger);
		for (SAMRecord record : reader) {
			SAMRecord newRecord = SAMRecordUtils.copyToSanger(record);
			writer.addAlignment(newRecord);
			progress.record(record);
		}
		// close readers and writers
		reader.close();
		writer.close();
	}

	private static void runFastq(File input, File output, boolean gzip) {
		// open readers
		FastqWriterFactory factory = new FastqWriterFactory();
		FastqReader reader = new FastqReader(input);
		FastqWriter writer = factory.newWriter(output);
		// start iterations
		FastqLogger progress = new FastqLogger(logger);
		for (FastqRecord record : reader) {
			FastqRecord newRecord = FastqRecordUtils.copyToSanger(record);
			writer.write(newRecord);
			progress.add();
		}
		// close readers and writers
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
		return options;
	}
}
