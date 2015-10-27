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
package org.vetmeduni.tools;

import org.vetmeduni.tools.implemented.TrimFastq;

/**
 * Enum with all the tools already developed
 *
 * @author Daniel Gómez-Sánchez
 */
public enum ToolNames {
	TrimFastq("Implementation of the trimming algorithm from Kofler et al. (2011)",
		"The script removes 'N' - characters at the beginning and the end of the provided reads. If any remaining 'N' "
			+ "characters are found the read is discarded. Quality removal is done using a modified Mott-algorithm: " +
			"for each base a score is calculated (score_base = quality_base - threshold). While scanning along the read "
			+ "a running sum of this score is calculated; If the score drops below zero the score is set to zero; The "
			+ "highest scoring region of the read is finally reported.\n\nCitation of the method: Kofler et al. (2011), " +
			"PLoS ONE 6(1), e15925, doi:10.1371/journal.pone.0015925");

	/**
	 * The short description for the tool
	 */
	public final String shortDescription;

	/**
	 * The long description for the tool
	 */
	public final String fullDescription;

	/**
	 * Constructor
	 *
	 * @param shortDescription the short description
	 * @param fullDescription  the full description
	 */
	ToolNames(String shortDescription, String fullDescription) {
		this.shortDescription = shortDescription;
		this.fullDescription = fullDescription;
	}

	/**
	 * Get the tool class from enums
	 *
	 * @param tool the tool as a String
	 *
	 * @return a new instance of a tool
	 */
	public static Tool getTool(String tool) throws IllegalArgumentException {
		switch (ToolNames.valueOf(tool)) {
			case TrimFastq:
				return new TrimFastq();
		}
		throw new RuntimeException("Unreachable code");
	}
}
