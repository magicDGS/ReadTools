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
package org.magicdgs.tools.cmd;

import htsjdk.samtools.SAMFileHeader;
import org.magicdgs.io.writers.bam.ReadToolsSAMFileWriterFactory;
import org.magicdgs.io.writers.bam.SplitSAMFileWriter;
import org.magicdgs.io.writers.fastq.ReadToolsFastqWriter;
import org.magicdgs.io.writers.fastq.ReadToolsFastqWriterFactory;
import org.magicdgs.io.writers.fastq.SplitFastqWriter;
import org.magicdgs.methods.barcodes.dictionary.BarcodeDictionary;

import java.io.IOException;

/**
 * Factory for the writers of the tools
 *
 * @author Daniel Gómez-Sánchez
 */
public class ToolWritersFactory {

	/**
	 * Get FASTQ split writers for the input, either spliting by barcodes or not
	 *
	 * @param prefix     the output prefix
	 * @param dictionary the barcode dictionary; if <code>null</code>, it won't split by barcode
	 * @param dgzip      disable gzip?
	 * @param multi      multi-thread output?
	 * @param single     single end?
	 *
	 * @return the writer for splitting
	 */
	public static SplitFastqWriter getFastqSplitWritersFromInput(String prefix, BarcodeDictionary dictionary,
		boolean dgzip, boolean multi, boolean single) throws IOException {
		ReadToolsFastqWriterFactory factory = new ReadToolsFastqWriterFactory();
		factory.setGzipOutput(!dgzip);
		factory.setUseAsyncIo(multi);
		if (dictionary != null) {
			return factory.newSplitByBarcodeWriter(prefix, dictionary, !single);
		} else {
			return factory.newSplitAssignUnknownBarcodeWriter(prefix, !single);
		}
	}

	/**
	 * Get a FASTQ writer either single or pair for the input
	 *
	 * @param dgzip  disable gzip?
	 * @param multi  multi-thread output?
	 * @param single single end?
	 *
	 * @return FastqWriter for single; PairFastqWriter for paired end
	 */
	public static ReadToolsFastqWriter getSingleOrPairWriter(String prefix, boolean dgzip, boolean multi,
		boolean single) throws IOException {
		ReadToolsFastqWriterFactory factory = new ReadToolsFastqWriterFactory();
		factory.setGzipOutput(!dgzip);
		factory.setUseAsyncIo(multi);
		if (single) {
			return factory.newWriter(prefix);
		} else {
			return factory.newPairWriter(prefix);
		}
	}

	/**
	 * Get a SAMFileWriter for adding records with RG from the barcode dictionary (split or not)
	 *
	 * @param prefix     the output prefix
	 * @param header     header with read group information (not the original)
	 * @param dictionary the barcode dictionary; if <code>null</code>, it won't split by barcode
	 * @param bam        should be the output a bam file?
	 * @param index      should the output be indexed?
	 * @param multi      multi-thread output?
	 *
	 * @return the writer for splitting or not
	 */
	public static SplitSAMFileWriter getBamWriterOrSplitWriterFromInput(String prefix, SAMFileHeader header,
		BarcodeDictionary dictionary, boolean bam, boolean index, boolean multi) throws IOException {
		ReadToolsSAMFileWriterFactory factory = new ReadToolsSAMFileWriterFactory().setUseAsyncIo(multi)
																				   .setCreateIndex(index);
		if (dictionary != null) {
			return factory.makeSplitByBarcodeWriter(header, prefix, bam, dictionary);
		} else {
			return factory.makeSplitAssignUnknownBarcodeWriter(header, prefix, bam);
		}
	}
}
