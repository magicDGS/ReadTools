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

import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import org.vetmeduni.io.readers.bam.SamReaderImpl;
import org.vetmeduni.io.readers.bam.SamReaderSanger;
import org.vetmeduni.io.readers.fastq.FastqReaderInterface;
import org.vetmeduni.io.readers.fastq.paired.FastqReaderPairedImpl;
import org.vetmeduni.io.readers.fastq.paired.FastqReaderPairedSanger;
import org.vetmeduni.io.readers.fastq.single.FastqReaderSingleImpl;
import org.vetmeduni.io.readers.fastq.single.FastqReaderSingleSanger;

import java.io.File;

/**
 * Readers factories for tools
 *
 * @author Daniel Gómez-Sánchez
 */
public class ToolsReadersFactory {

	/**
	 * Get a FastqReader for single end (if input2 is <code>null</code>) or pair-end (if input2 is not
	 * <code>null</code>, both in standardize format (isMaintained <code>false</code>) or in the same format
	 * (isMaintained <code>true</code>)
	 *
	 * @param input1       the input for the first pair
	 * @param input2       the input for the second pair; <code>null</code> if it is single end processing
	 * @param isMaintained should be the format maintained or standardize?
	 * @param allowHigherQualities higher qualities does not thrown an error?
	 *
	 * @return the reader for the file(s)
	 */
	public static FastqReaderInterface getFastqReaderFromInputs(File input1, File input2, boolean isMaintained, boolean allowHigherQualities) {
		FastqReaderInterface toReturn;
		if (input2 == null) {
			toReturn = (isMaintained) ? new FastqReaderSingleImpl(input1, allowHigherQualities) : new FastqReaderSingleSanger(input1, allowHigherQualities);
		} else {
			toReturn = (isMaintained) ?
				new FastqReaderPairedImpl(input1, input2, allowHigherQualities) :
				new FastqReaderPairedSanger(input1, input2, allowHigherQualities);
		}
		return toReturn;
	}

	/**
	 * Get the SamReader for the input, maintaining or not the format
	 *
	 * @param input        the input BAM/SAM file
	 * @param isMaintained should be the format maintained or standardize?
	 * @param allowHigherQualities higher qualities does not thrown an error?
	 *
	 * @return the reader for the file
	 */
	public static SamReader getSamReaderFromInput(File input, boolean isMaintained, boolean allowHigherQualities) {
		if (isMaintained) {
			// if the format is maintained, create a default sam reader
			return new SamReaderImpl(input, ValidationStringency.SILENT, allowHigherQualities);
		} else {
			// if not, standardize
			return new SamReaderSanger(input, ValidationStringency.SILENT, allowHigherQualities);
		}
	}
}
