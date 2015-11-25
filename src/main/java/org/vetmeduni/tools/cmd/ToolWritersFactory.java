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

import org.vetmeduni.io.writers.fastq.ReadToolsFastqWriter;
import org.vetmeduni.io.writers.fastq.ReadToolsFastqWriterFactory;
import org.vetmeduni.io.writers.fastq.SplitFastqWriter;
import org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionary;

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
			return factory.newSplitAssingUnknownBarcodeWriter(prefix, !single);
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
}
