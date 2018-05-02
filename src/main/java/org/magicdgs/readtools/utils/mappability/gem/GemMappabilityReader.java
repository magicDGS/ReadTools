/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.utils.mappability.gem;

import com.google.common.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GemMappabilityReader {

    private static final String RECORD_PREFIX = "~";
    private static final String HEADER_PREFIX = "~~";

    private static final String KMER_LENGTH_HEADER = HEADER_PREFIX + "K-MER LENGTH";
    private static final String APPROX_THRESHOLD_HEADER = HEADER_PREFIX + "APPROXIMATION THRESHOLD";
    private static final String MAX_MISMATCH_HEADER = HEADER_PREFIX + "MAX MISMATCHES";
    private static final String MAX_ERRORS_HEADER = HEADER_PREFIX + "MAX ERRORS";
    private static final String MAX_INDEL_LENGTH_HEADER = HEADER_PREFIX + "MAX BIG INDEL LENGTH";
    private static final String MIN_MATCH_HEADER = HEADER_PREFIX + "MIN MATCHED BASES";
    private static final String STRATA_AFTER_BEST_HEADER = HEADER_PREFIX + "STRATA AFTER BEST";
    private static final String ENCODING_HEADER = HEADER_PREFIX + "ENCODING";

    @VisibleForTesting
    static final Pattern ENCODING_EXPRESSION = Pattern.compile("'(.)'~\\[(\\d)+-(\\d)+]");

    private BufferedReader reader;

    private GemMappabilityHeader header;

    private GemMappabilityHeader readHeader() throws IOException {
        return new GemMappabilityHeader(
                readIntHeader(KMER_LENGTH_HEADER),
                readIntHeader(APPROX_THRESHOLD_HEADER),
                readIntHeader(MAX_MISMATCH_HEADER),
                readIntHeader(MAX_ERRORS_HEADER),
                readIntHeader(MAX_INDEL_LENGTH_HEADER),
                readIntHeader(MIN_MATCH_HEADER),
                readIntHeader(STRATA_AFTER_BEST_HEADER),
                readEncoding());
    }

    private Map<Character, int[]> readEncoding() throws IOException {
        final String encoding = reader.readLine();
        if (encoding.equals(ENCODING_HEADER)) {
            // TODO: better exception
            throw new IOException(String.format("Wrong header line: expected '%s' but found '%s'",
                    ENCODING_HEADER, encoding));
        }


        // TODO: fill in the header
        return null;
    }

    private int readIntHeader(final String expected) throws IOException {
        final String descLine = reader.readLine();
        if (!descLine.equals(expected)) {
            // TODO: better exception
            throw new IOException(String.format("Wrong header line: expected '%s' but found '%s'",
                    expected, descLine));
        }
        // TODO: handle exception
        return Integer.parseInt(reader.readLine());
    }

}
