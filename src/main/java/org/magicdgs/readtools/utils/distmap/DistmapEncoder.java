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

package org.magicdgs.readtools.utils.distmap;

import org.magicdgs.readtools.utils.read.FastqGATKRead;
import org.magicdgs.readtools.utils.read.RTReadUtils;

import htsjdk.samtools.fastq.FastqRecord;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;
import scala.Tuple2;

/**
 * Utility class for encode/decode {@link GATKRead} to the Distmap format.
 *
 * Distmap (<a href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0072614">
 * Pandey & Schlötterer 2013</a>) is a wrapper around different mappers for distributed
 * computation using Hadoop. The input for this tool is a modified FASTQ format which is writen in
 * HDFS to safe space and to distribute easily pair-end reads. It is a tab-delimited format with
 * the following fields:
 *
 * 1. Read name with '@' symbol included.
 * 2. Read sequence.
 * 3. Read quality.
 * 4. Second read sequence (if pair-end).
 * 5. Second read quality (if pair-end).
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class DistmapEncoder {

    /** Cannot be instantiated. */
    private DistmapEncoder() {}

    // Distmap format is tab-delimited
    private static final String DISTMAP_TOKEN_SEPARATOR = "\t";

    // Distmap format read name is preceded by this character
    private static final char DISTMAP_READ_NAME_INDICATOR = '@';
    // read name index in the tokens (common for first and second)
    private static final int READ_NAME_TOKEN = 0;
    // first read bases index in the tokens
    private static final int FIRST_READ_BASES_TOKEN = 1;
    // first read qualities index in the tokens
    private static final int FIRST_READ_QUALITY_TOKEN = 2;
    // second read bases index in the token
    private static final int SECOND_READ_BASES_TOKEN = 3;
    // second read qualities indes in the tokens
    private static final int SECOND_READ_QUALITY_TOKEN = 4;
    // maximum number of tokens in the index
    private static final int MAXIMUM_NUMBER_OF_TOKENS = 5;

    /**
     * Encodes a single-end read into a Distmap string.
     *
     * @param read the read to encode.
     */
    public static final String encode(final GATKRead read) {
        Utils.nonNull(read, "null read");
        return String.format("%s%s\t%s\t%s",
                DISTMAP_READ_NAME_INDICATOR,
                RTReadUtils.getReadNameWithIlluminaBarcode(read),
                read.getBasesString(),
                ReadUtils.getBaseQualityString(read)
        );
    }

    /**
     * Encodes a pair-end read into a Distmap string.
     *
     * @param pair tuple with the first and the second reads in the pair.
     *
     * @throws DistmapException if the pairs have different names.
     */
    public static final String encode(final Tuple2<GATKRead, GATKRead> pair) {
        Utils.nonNull(pair, "null pair-end read");
        Utils.nonNull(pair._1, "null first read");
        Utils.nonNull(pair._2, "null second read");
        DistmapException.distmapValidation(pair._1.getName().equals(pair._2.getName()),
                () -> "not equal names for pairs: " + pair._1 + " vs. " + pair._2);
        // TODO 21-03-2017: we assume that barcodes are equal and use the one of the first pair (without pair them here)
        // TODO: see https://github.com/magicDGS/ReadTools/issues/159 for more information

        // encode them
        return String.format("%s\t%s\t%s",
                encode(pair._1),
                pair._2.getBasesString(),
                ReadUtils.getBaseQualityString(pair._2));
    }

    /**
     * Checks if a Distmap String represents paired or unpaired data.
     *
     * @param distmapString the string encoded in the Distmap format.
     *
     * @return {@code true} if it is paired; {@code false} otherwise.
     *
     * @throws DistmapException if the String is not properly formatted.
     */
    public static final boolean isPaired(final String distmapString) {
        final String[] tokens = getTokens(distmapString);
        return tokens.length == MAXIMUM_NUMBER_OF_TOKENS;
    }

    // helper method to get the tokens for a distmap string, performing validation too
    // it also removes the '@' marker in the read name
    private static final String[] getTokens(final String distmapString) {
        // split
        final String[] tokens = distmapString.split(DISTMAP_TOKEN_SEPARATOR);

        // validates the first character
        Utils.validateArg(tokens[READ_NAME_TOKEN].charAt(0) == DISTMAP_READ_NAME_INDICATOR,
                () -> "distmap String should start with " + DISTMAP_READ_NAME_INDICATOR
                        + ": " + distmapString);

        // validates the number of tokens
        Utils.validateArg(tokens.length == SECOND_READ_BASES_TOKEN
                        || tokens.length == MAXIMUM_NUMBER_OF_TOKENS,
                () -> "unexpected number of tokens in distmap String: " + distmapString);

        // removes the first char
        tokens[READ_NAME_TOKEN] = tokens[READ_NAME_TOKEN].substring(1);
        return tokens;
    }

    /**
     * Decodes a single-end Distmap String.
     *
     * @param distmapSingleString the string encoded in the Distmap format.
     *
     * @return read using the {@link FastqGATKRead} implementation.
     *
     * @throws DistmapException if the String is not properly formatted.
     * @see FastqGATKRead
     */
    public static final GATKRead decodeSingle(final String distmapSingleString) {
        // split and validate the string
        final String[] tokens = getTokens(distmapSingleString);
        DistmapException.distmapValidation(tokens.length == SECOND_READ_BASES_TOKEN,
                () -> "not single-end Distmap input: " + distmapSingleString);

        // get the read as a first indexes -> this gets directly an unpaired read
        return getRead(tokens, FIRST_READ_BASES_TOKEN, FIRST_READ_QUALITY_TOKEN);
    }

    /**
     * Decodes a pair-end Distmap String.
     *
     * @param distmapPairedString the string encoded in the Distmap format.
     *
     * @return pair of reads using the {@link FastqGATKRead} implementation. The flags are set to be
     * first and second of pair.
     *
     * @throws DistmapException if the String is not properly formatted.
     * @see FastqGATKRead
     */
    public static final Tuple2<GATKRead, GATKRead> decodePaired(final String distmapPairedString) {
        // split and validate the string
        final String[] tokens = getTokens(distmapPairedString);
        DistmapException.distmapValidation(tokens.length == MAXIMUM_NUMBER_OF_TOKENS,
                () -> "not single-end Distmap input: " + distmapPairedString);

        // get each read using the token indexes
        final GATKRead first = getRead(tokens, FIRST_READ_BASES_TOKEN, FIRST_READ_QUALITY_TOKEN);
        final GATKRead second = getRead(tokens, SECOND_READ_BASES_TOKEN, SECOND_READ_QUALITY_TOKEN);

        // set the pair-end information
        first.setIsFirstOfPair();
        second.setIsSecondOfPair();

        // return the tuple
        return new Tuple2<>(first, second);
    }

    // helper method to create a read
    // it uses the implementation of the FastqGATKRead 
    private static final GATKRead getRead(final String[] tokens,
            final int baseToken, final int qualityToken) {
        return new FastqGATKRead(new FastqRecord(
                // the '@' symbol was removed when getting the tokens
                tokens[READ_NAME_TOKEN],
                tokens[baseToken],
                null, // there is no quality header
                tokens[qualityToken]));
    }
}
