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
package org.vetmeduni.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;

/**
 * Static formats for output times and numbers
 *
 * @author Daniel Gómez-Sánchez
 */
public class Formats {

	/**
	 * Format for times
	 */
	public final static DecimalFormat timeFmt = new DecimalFormat("00");

	/**
	 * Format for big numbers with commas each 3 numbers
	 */
	public final static NumberFormat commaFmt = new DecimalFormat("#,###");

	/**
	 * Format for decimal numbers rounded to 7
	 */
	public final static DecimalFormat roundToSevenFmt = new DecimalFormat("#.#######");

	/**
	 * Get a rounded format with certain number of significant digits
	 *
	 * @param digits the numer of digits
	 *
	 * @return the number formatted as a String
	 */
	public static DecimalFormat getRoundFormat(int digits) {
		return new DecimalFormat(String.format("#.%s", String.join("", Collections.nCopies(digits, "#"))));
	}
}
