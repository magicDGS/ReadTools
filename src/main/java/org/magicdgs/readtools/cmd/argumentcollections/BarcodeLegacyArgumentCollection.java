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
 * SOFTWARE.
 */

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionaryFactory;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;

import htsjdk.samtools.SAMReadGroupRecord;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.utils.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Argument collection for barcodes
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeLegacyArgumentCollection implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BARCODES_LONG_NAME = "barcodes";
    public static final String BARCODES_SHORT_NAME = "bc";
    public static final String BARCODES_DOC =
            "White-space delimited (tabs or spaces) file with the first column with the sample name, the second with the library name and the following containing the barcodes (1 or 2 depending on the barcoding method)";

    /** Barcode file. A white-space delimited file with sampleName, libraryName and barcodes. */
    @Argument(fullName = BARCODES_LONG_NAME, shortName = BARCODES_SHORT_NAME, optional = false, doc = BARCODES_DOC)
    public String inputFile;

    public static final String MAXIMUM_MISMATCH_LONG_NAME = "maximum-mismatches";
    public static final String MAXIMUM_MISMATCH_SHORT_NAME = "m";
    public static final String MAXIMUM_MISMATCH_DOC =
            "Maximum number of mismatches allowed for a matched barcode. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file.";

    /** Maximum number of mismatches allowed. */
    @Argument(fullName = MAXIMUM_MISMATCH_LONG_NAME, shortName = MAXIMUM_MISMATCH_SHORT_NAME, optional = true, doc = MAXIMUM_MISMATCH_DOC)
    public List<Integer> maxMismatches = new ArrayList<>(Collections.singleton(BarcodeDecoder.DEFAULT_MAXIMUM_MISMATCHES));

    public static final String MINIMUM_DISTANCE_LONG_NAME = "minimum-distance";
    public static final String MINIMUM_DISTANCE_SHORT_NAME = "d";
    public static final String MINIMUM_DISTANCE_DOC =
            "Minimum distance (in difference between number of mismatches) between the best match and the second to consider a match. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file.";

    /** Minimum distance between matches in barcodes. */
    @Argument(fullName = MINIMUM_DISTANCE_LONG_NAME, shortName = MINIMUM_DISTANCE_SHORT_NAME, optional = true, doc = MINIMUM_DISTANCE_DOC)
    public List<Integer> minimumDistance = new ArrayList<>(Collections.singleton(BarcodeDecoder.DEFAULT_MIN_DIFFERENCE_WITH_SECOND));

    public static final String N_NO_MISMATCH_LONG_NAME = "n-no-mismatch";
    public static final String N_NO_MISMATCH_SHORT_NAME = "nnm";
    public static final String N_NO_MISMATCH_DOC = "Do not count Ns as mismatch";

    /** If {@code true}, Ns should not be counted as mismatch. */
    @Argument(fullName = N_NO_MISMATCH_LONG_NAME, shortName = N_NO_MISMATCH_SHORT_NAME, optional = true, doc = N_NO_MISMATCH_DOC)
    public Boolean nNoMismatch = false;

    // TODO: this option should be change to a different place or baing change for a splitting strategy
    public static final String SPLIT_LONG_NAME = "split";
    public static final String SPLIT_SHORT_NAME = "x";
    public static final String SPLIT_DOC =
            "Split each sample from the barcode dictionary in a different file";

    /** If {@code true}, split the output by barcode. */
    @Argument(fullName = SPLIT_LONG_NAME, shortName = SPLIT_SHORT_NAME, optional = true, doc = SPLIT_DOC)
    public Boolean split = false;

    public static final String MAXIMUM_N_LONG_NAME = "maximum-N";
    public static final String MAXIMUM_N_SHORT_NAME = "N";
    public static final String MAXIMUM_N_DOC =
            "Maximum number of Ns allowed in the barcode to discard it. If null, no threshold will be applied.";

    /** Maximum number of Ns allowed in the barcode. */
    @Argument(fullName = MAXIMUM_N_LONG_NAME, shortName = MAXIMUM_N_SHORT_NAME, optional = true, doc = MAXIMUM_N_DOC)
    public Integer maxN = null;

    /**
     * Gets a barcode decoder from the command line.
     *
     * @param logger the logger to log results.
     *
     * @return the barcode decoder.
     */
    public BarcodeDecoder getBarcodeDecoderFromArguments(final Logger logger) throws IOException {
        return getBarcodeDecoderFromArguments(logger, new ReadGroupLegacyArgumentCollection());
    }


    /**
     * Gets a barcode decoder from the command line.
     *
     * @param logger the logger to log results.
     *
     * @return the barcode decoder.
     */
    public BarcodeDecoder getBarcodeDecoderFromArguments(final Logger logger,
            final ReadGroupLegacyArgumentCollection rgac) {
        final BarcodeDictionary dictionary = getBarcodeDictionaryFromArguments(logger, rgac);
        int[] mismatches = maxMismatches.stream().mapToInt(Integer::intValue).toArray();
        int[] minDist = minimumDistance.stream().mapToInt(Integer::intValue).toArray();
        return new BarcodeDecoder(dictionary,
                (maxN == null) ? Integer.MAX_VALUE : maxN,
                !nNoMismatch, mismatches, minDist);
    }

    /**
     * Gets the barcode dictionary option from the command line.
     *
     * @param logger the logger to log results.
     *
     * @return the combined barcode.
     */
    private BarcodeDictionary getBarcodeDictionaryFromArguments(final Logger logger,
            final ReadGroupLegacyArgumentCollection rgac) {
        final BarcodeDictionary dictionary;
        final SAMReadGroupRecord readGroupInfo = rgac.getUnknownBasicReadGroup();
        dictionary = BarcodeDictionaryFactory
                .createDefaultDictionary(rgac.runId, IOUtils.getPath(inputFile), readGroupInfo);
        logger.info("Loaded barcode file for {} samples with {} different barcode sets",
                dictionary.numberOfUniqueSamples(), dictionary.numberOfSamples());
        return dictionary;
    }

}
