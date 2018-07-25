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
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.FastLineReader;
import org.apache.commons.lang3.Range;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
 *         the values of mappability. See {@link GemMappabilityHeader} for more information.</li>
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
    private static final Pattern ENCODING_PATTERN = Pattern.compile("'(.)'~\\[(\\d+)-(\\d+)]");

    // stored for error message and get the identity of the path
    private final Path path;
    // header from the file, read on construction
    private final GemMappabilityHeader header;

    // TODO: change implementation? (see https://github.com/magicDGS/ReadTools/issues/486)
    private FastLineReader reader;

    // iteration values to keep track of the current sequence
    private String currentSequence = null;
    private int currentSequencePosition = -1;

    // logger
    private final Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Loads a gem-mappability formatted file.
     *
     * @param path path where the file is located.
     *
     * @throws IOException if the reader cannot be open.
     */
    public GemMappabilityReader(final Path path) throws IOException {
        this.path = Utils.nonNull(path);
        // TODO: accept compressed inputs (https://github.com/magicDGS/ReadTools/issues/482)
        this.reader = new FastLineReader(Files.newInputStream(this.path));
        this.header = readHeader();
    }

    /**
     * Testing constructor.
     *
     * @param reader reader starting from the encodings.
     * @param header header with the meta-data storage.
     */
    @VisibleForTesting
    protected GemMappabilityReader(final FastLineReader reader, final GemMappabilityHeader header) {
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
        if (reader == null) {
            return false;
        }
        // first skip the new lines to ensure that hasNext is accurate
        reader.skipNewlines();
        return !reader.eof();
    }

    @Override
    public GemMappabilityRecord next() {
        // throw if there is no next
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        // changing to next sequence
        if (isAtHeaderLine()) {
            this.currentSequence = readLine().substring(1);
            this.currentSequencePosition = 1;
        }

        // read the encoded value and get the range
        // TODO: use buffering? (see https://github.com/magicDGS/ReadTools/issues/486)
        final byte encoded = reader.getByte();
        final Range<Long> range = header.getEncodedValues(encoded);

        if (range == null) {
            throw new GemMappabilityException(path, currentSequence, currentSequencePosition,
                    String.format("character '%c' not present in the header", encoded));
        }

        // advance the position one and generate the record
        return new GemMappabilityRecord(
                this.currentSequence,
                this.currentSequencePosition++,
                range);
    }

    @Override
    public void close() {
        // set iteration values to unitialize
        currentSequence = null;
        currentSequencePosition = -1;
        CloserUtil.close(reader);
        reader = null;
    }

    //////////////////////////
    // METADATA RELATED-METHODS

    /**
     * Gets the header with the metadata from the file.
     *
     * @return gem-mappability heaader.
     */
    public GemMappabilityHeader readHeader() {
        logger.debug("Reading header");
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

    // reaad a meta-data line (integer value)
    private int readIntHeader(final String expected) {
        logger.debug("Readig meta-data line: {}", expected);
        skipMetadataLine(expected);
        final String line = readLine();
        try {
            return Integer.parseInt(line);
        } catch (final NumberFormatException e) {
            throw new GemMappabilityException(path, expected, "expected integer but found " + line);
        }
    }

    // read the encoding map conversion
    private Map<Byte, Range<Long>> readEncoding() {
        logger.debug("Reading encoding meta-data");
        skipMetadataLine(ENCODING_HEADER_TEXT);
        final Map<Byte, Range<Long>> map = new HashMap<>();

        while (hasNext() && !isAtHeaderLine()) {
            final String line = readLine();
            // break if there is no next
            final Matcher matcher = ENCODING_PATTERN.matcher(line);
            if (!matcher.find()) {
                throw new GemMappabilityException(path, ENCODING_HEADER_TEXT,
                        String.format("should match %s (found %s)", ENCODING_PATTERN,
                                line));
            }
            // this should not fail because the pattern matches
            map.put((byte) matcher.group(1).charAt(0), Range.between(
                    Long.parseLong(matcher.group(2)),
                    Long.parseLong(matcher.group(3))));

        }
        if (map.isEmpty()) {
            throw new GemMappabilityException(path, ENCODING_HEADER_TEXT, " no entry found");
        }
        return map;
    }

    // skip a metadata line
    private void skipMetadataLine(final String text) {
        final String headerLine = readLine();
        if (!headerLine.isEmpty() &&
                headerLine.charAt(0) == HEADER_PREFIX && headerLine.charAt(1) == HEADER_PREFIX &&
                headerLine.endsWith(text)) {
            return;
        }
        // invalid format
        throw new GemMappabilityException(path, text, "expected header not found: " + headerLine + " instead");
    }

    // return true for a header line (starting with ~); false otherwise
    private boolean isAtHeaderLine() {
        // skip first the end of line if at any
        reader.skipNewlines();
        return !reader.eof() && reader.peekByte() == HEADER_PREFIX;
    }

    ///////////////////////
    // HELPER METHODS

    // read a line wrapping the IO exceptions
    private String readLine() {
        final StringBuilder str = new StringBuilder();
        // TODO: use buffer? (see https://github.com/magicDGS/ReadTools/issues/486)
        while (!reader.skipNewlines()) {
            if (reader.eof()) {
                break;
            }
            str.append((char) reader.getByte());
        }
        return str.toString();
    }
}
