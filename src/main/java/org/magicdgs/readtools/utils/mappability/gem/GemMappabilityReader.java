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

import htsjdk.samtools.util.RuntimeIOException;
import org.apache.commons.lang3.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GemMappabilityReader implements Iterator<GemMappabilityRecord> {

    private static final char HEADER_PREFIX = '~';

    private static final String KMER_LENGTH_HEADER_TEXT = "K-MER LENGTH";
    private static final String APPROX_THRESHOLD_HEADER_TEXT = "APPROXIMATION THRESHOLD";
    private static final String MAX_MISMATCH_HEADER_TEXT = "MAX MISMATCHES";
    private static final String MAX_ERRORS_HEADER_TEXT = "MAX ERRORS";
    private static final String MAX_INDEL_LENGTH_HEADER_TEXT = "MAX BIG INDEL LENGTH";
    private static final String MIN_MATCH_HEADER_TEXT = "MIN MATCHED BASES";
    private static final String STRATA_AFTER_BEST_HEADER_TEXT = "STRATA AFTER BEST";
    private static final String ENCODING_HEADER_TEXT = "ENCODING";

    private static final Pattern ENCODING_PATTERN = Pattern.compile("'(.)'~\\[(\\d)+-(\\d)+]");


    private final BufferedReader reader;
    private final GemMappabilityHeader header;
    private String currentSequence;
    private long currentSequencePosition;

    public GemMappabilityReader(final Path path) throws IOException {
        this.reader = Files.newBufferedReader(path);
        this.header = readHeader(reader);
        this.currentSequence = advanceSequence();
        this.currentSequencePosition = 0;
    }

    public GemMappabilityHeader getHeader() {
        return header;
    }

    @Override
    public boolean hasNext() {
        // TODO: should be implemented
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public GemMappabilityRecord next() {
        try {
            // changing to next sequence
            if (isSequenceHeader(reader)) {
                this.currentSequence = advanceSequence();
                this.currentSequencePosition = 0;
            }
            currentSequencePosition++;
            return new GemMappabilityRecord(this.currentSequence,
                    this.currentSequencePosition,
                    header.getEncodedValues((byte) reader.read()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String advanceSequence() throws IOException {
        if (isSequenceHeader(reader)) {
            // remove the first character
            return reader.readLine().substring(1);
        }
        throw new IllegalStateException("Should not be called now");
    }

    //////////////////////////
    // METADATA RELATED-METHODS

    public static GemMappabilityHeader readHeader(final BufferedReader reader) throws IOException {
        return new GemMappabilityHeader(
                readIntHeader(reader, KMER_LENGTH_HEADER_TEXT),
                readIntHeader(reader, APPROX_THRESHOLD_HEADER_TEXT),
                readIntHeader(reader, MAX_MISMATCH_HEADER_TEXT),
                readIntHeader(reader, MAX_ERRORS_HEADER_TEXT),
                readIntHeader(reader, MAX_INDEL_LENGTH_HEADER_TEXT),
                readIntHeader(reader, MIN_MATCH_HEADER_TEXT),
                readIntHeader(reader, STRATA_AFTER_BEST_HEADER_TEXT),
                readEncoding(reader));
    }


    private static int readIntHeader(final BufferedReader reader, final String expected) throws IOException {
        skipMetadataLine(reader, expected);
        return Integer.parseInt(reader.readLine());
    }

    private static Map<Byte, Range<Integer>> readEncoding(final BufferedReader reader) throws IOException {
        skipMetadataLine(reader, ENCODING_HEADER_TEXT);
        final Map<Byte, Range<Integer>> map = new LinkedHashMap<>();

        while(!isSequenceHeader(reader)) {
            final String line = reader.readLine();
            final Matcher matcher = ENCODING_PATTERN.matcher(line);
            if (!matcher.find()) {
                throw new IOException(String.format("Wrong encoding line: %s (expected mathicg %s)", line,
                        ENCODING_PATTERN));
            }
            map.put(Byte.parseByte(matcher.group(1)), Range.between(
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))));

        }
        return map;
    }

    private static void skipMetadataLine(final BufferedReader reader, final String text) throws IOException {
        final String headerLine = reader.readLine();
        if (headerLine.charAt(0) == HEADER_PREFIX && headerLine.charAt(1) == HEADER_PREFIX && headerLine.endsWith(text)) {
            return;
        }
        throw new IOException(String.format("Wrong header line: %s (expected %s)", headerLine, text));
    }

    ///////////////////////
    // HELPER METHODS

    private static boolean isSequenceHeader(final BufferedReader reader) throws IOException{
        reader.mark(0);
        final boolean res = reader.read() == HEADER_PREFIX;
        reader.reset();
        return res;
    }
}
