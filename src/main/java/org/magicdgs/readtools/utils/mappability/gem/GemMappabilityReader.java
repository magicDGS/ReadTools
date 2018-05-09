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
import htsjdk.samtools.util.BufferedLineReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.RuntimeIOException;
import org.apache.commons.lang3.Range;
import org.broadinstitute.hellbender.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Iterator over gem-mappability number of mappings.
 *
 * <p>The GEM-mappability FASTA-like format consists on:
 * <ul>
 *     <li>Header with metadata. Includes information about the parameters used and the encoding for
 *     the values of mappability. See {@link GemMappabilityHeader} for more information</li>
 *     <li>Per-base range of values. Encoded as a char, representing a range of number of mappings.</li>
 * </ul>
 *
 * <p>This class iterates over the file and retrieves the per-base range of values as a
 * {@link GemMappabilityRecord}, which contains the sequence name, position and range of values.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class GemMappabilityReader implements CloseableIterator<GemMappabilityRecord> {

    // HEADER PREFIX (for sequence name and meta-data)
    private static final char HEADER_PREFIX = '~';

    ///////////////////////////
    // HEADER LINES - prefixed by two HEADER_PREFIX

    private static final String KMER_LENGTH_HEADER_TEXT = "K-MER LENGTH";
    private static final String APPROX_THRESHOLD_HEADER_TEXT = "APPROXIMATION THRESHOLD";
    private static final String MAX_MISMATCH_HEADER_TEXT = "MAX MISMATCHES";
    private static final String MAX_ERRORS_HEADER_TEXT = "MAX ERRORS";
    private static final String MAX_INDEL_LENGTH_HEADER_TEXT = "MAX BIG INDEL LENGTH";
    private static final String MIN_MATCH_HEADER_TEXT = "MIN MATCHED BASES";
    private static final String STRATA_AFTER_BEST_HEADER_TEXT = "STRATA AFTER BEST";
    private static final String ENCODING_HEADER_TEXT = "ENCODING";

    // pattern for the encoding (char to value range)
    protected static final Pattern ENCODING_PATTERN = Pattern.compile("'(.)'~\\[(\\d)+-(\\d)+]");

    // stored for error message and get the identity of the path
    private final Path path;
    // reader (BLR allows to peek and track the line number for error messages)
    private final BufferedLineReader reader;
    // header from the file, read on construction
    private final GemMappabilityHeader header;

    // cached values to keep track of the current sequence
    private String currentSequence = null;
    private int currentSequencePosition = -1;

    /**
     * Loads a gem-mappability formatted file.
     *
     * @param path path where the file is located.
     * @throws IOException if the reader cannot be open.
     */
    public GemMappabilityReader(final Path path) throws IOException {
        this.path = Utils.nonNull(path);
        this.reader = new BufferedLineReader(Files.newInputStream(this.path));
        this.header = readHeader();
    }

    @VisibleForTesting
    protected GemMappabilityReader(final BufferedLineReader reader, final GemMappabilityHeader header) {
        this.reader = reader;
        this.path = null;
        this.header = header;
    }

    /**
     * Gets the header associated with this file.
     *
     * @return gem-mappability header.
     */
    public GemMappabilityHeader getHeader() {
        return header;
    }

    @Override
    public boolean hasNext() {
        return peekWrappingException() != -1;
    }

    @Override
    public GemMappabilityRecord next() {
        // throw if there is no next
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        // changing to next sequence
        if (isAtHeaderLine()) {
            this.currentSequence = readLineWrappingException().substring(1);
            this.currentSequencePosition = 0;
        }
        // advance the position one and generate the record
        return new GemMappabilityRecord(
                this.currentSequence,
                ++this.currentSequencePosition,
                header.getEncodedValues(readByteWrappingException()));
    }

    @Override
    public void close() {
        // throws RuntimeIOException if IO
        reader.close();
    }

    //////////////////////////
    // METADATA RELATED-METHODS

    public GemMappabilityHeader readHeader() {
        return new GemMappabilityHeader(
                readIntHeader(KMER_LENGTH_HEADER_TEXT),
                readIntHeader(APPROX_THRESHOLD_HEADER_TEXT),
                readIntHeader(MAX_MISMATCH_HEADER_TEXT),
                readIntHeader(MAX_ERRORS_HEADER_TEXT),
                readIntHeader(MAX_INDEL_LENGTH_HEADER_TEXT),
                readIntHeader(MIN_MATCH_HEADER_TEXT),
                readIntHeader(STRATA_AFTER_BEST_HEADER_TEXT),
                readEncoding());
    }


    private int readIntHeader(final String expected) {
        skipMetadataLine(expected);
        final String line = readLineWrappingException();
        try {
            return Integer.parseInt(line);
        } catch (final NumberFormatException e) {
            throw new GemMappabilityException(path,
                    reader.getLineNumber() - line.length(),
                    String.format("integer expected for %s header (found %s)", expected, line));
        }
    }

    private Map<Byte, Range<Integer>> readEncoding() {
        skipMetadataLine(ENCODING_HEADER_TEXT);
        final Map<Byte, Range<Integer>> map = new LinkedHashMap<>();

        while (!isAtHeaderLine()) {
            final String line = readLineWrappingException();
            final Matcher matcher = ENCODING_PATTERN.matcher(line);
            if (!matcher.find()) {
                throw new GemMappabilityException(path,
                        reader.getLineNumber() - line.length(),
                        String.format("encoding should match %s (found %s)", ENCODING_PATTERN, line));
            }
            // this should not fail because the pattern matches.
            map.put(Byte.parseByte(matcher.group(1)), Range.between(
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))));

        }
        return map;
    }

    private void skipMetadataLine(final String text) {
        final String headerLine = readLineWrappingException();
        if (headerLine.charAt(0) == HEADER_PREFIX && headerLine.charAt(1) == HEADER_PREFIX
                && headerLine.endsWith(text)) {
            return;
        }
        // invalid format
        throw new GemMappabilityException(path,
                reader.getLineNumber() - headerLine.length(),
                String.format("expected header is %s (found %s)", headerLine, text));
    }

    private boolean isAtHeaderLine() {
        return peekWrappingException() == HEADER_PREFIX;
    }

    ///////////////////////
    // HELPER METHODS

    private String readLineWrappingException() {
        try {
            return reader.readLine();
        } catch (final RuntimeIOException e) {
            throw GemMappabilityException.readingException(path, reader.getLineNumber(), e);
        }
    }

    private int peekWrappingException() {
        try {
            return reader.peek();
        } catch (final RuntimeIOException e) {
            throw GemMappabilityException.readingException(path, reader.getLineNumber(), e);
        }
    }

    private byte readByteWrappingException() {
        try {
            return (byte) reader.read();
        } catch (final IOException e) {
            throw GemMappabilityException.readingException(path, reader.getLineNumber(), e);
        }
    }

}
