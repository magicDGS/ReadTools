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

import org.vetmeduni.io.readers.FastqReaderInterface;
import org.vetmeduni.io.readers.paired.FastqReaderPairedImpl;
import org.vetmeduni.io.readers.paired.FastqReaderPairedSanger;
import org.vetmeduni.io.readers.single.FastqReaderSingleSanger;
import org.vetmeduni.io.readers.single.FastqReaderWrapper;

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
	 *
	 * @return the reader for the file(s)
	 */
	public static FastqReaderInterface getFastqReaderFromInputs(File input1, File input2, boolean isMaintained) {
		FastqReaderInterface toReturn;
		if (input2 == null) {
			toReturn = (isMaintained) ? new FastqReaderWrapper(input1) : new FastqReaderSingleSanger(input1);
		} else {
			toReturn = (isMaintained) ?
				new FastqReaderPairedImpl(input1, input2) :
				new FastqReaderPairedSanger(input1, input2);
		}
		return toReturn;
	}
}
