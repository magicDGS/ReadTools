/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
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
 * SOFTWARE.
 */

package org.magicdgs.readtools;

/**
 * Constants for help/documentation purposes.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class RTHelpConstants {

    // cannot be instantiated
    private RTHelpConstants() {}

    /** Software's name. */
    public static final String READTOOLS_NAME = "ReadTools";

    // the ReadTools main page hosting the code
    private final static String GITHUB_URL = "https://github.com/magicDGS/ReadTools";

    /** Issue tracker page. */
    public static final String ISSUE_TRACKER = GITHUB_URL + "/issues";

    /** Documentation main page. */
    public static final String DOCUMENTATION_PAGE = GITHUB_URL + "/wiki";

    ///////////////////////////////
    // PROGRAM GROUP NAMES

    /** Documentation name for reads manipulation. */
    public static final String DOC_CAT_READS_MANIPULATION = "Reads manipulation";
    /** Documentation description for reads manipulation. */
    public static final String DOC_CAT_READS_MANIPULATION_SUMMARY = "Tools for manipulating any supported read source (SAM/BAM/CRAM/FASTQ)";

    /** Documentation name for reads conversion. */
    public static final String DOC_CAT_READS_CONVERSION = "Reads conversion";
    /** Documentation description for reads onversion. */
    public static final String DOC_CAT_READS_CONVERSION_SUMMARY = "Tools for converting any supported read source (SAM/BAM/CRAM/FASTQ)";
}
