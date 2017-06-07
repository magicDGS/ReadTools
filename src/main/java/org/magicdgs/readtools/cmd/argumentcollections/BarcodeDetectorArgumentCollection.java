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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionaryFactory;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Argument collection for barcode detector.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class BarcodeDetectorArgumentCollection implements Serializable {
    private static final long serialVersionUID = 1L;

    // TODO: this should be pointing to the help page for more information
    // TODO: and here just add the required elements
    @Argument(fullName = "barcodeFile", shortName = "bc", optional = false, doc =
            BarcodeDictionaryFactory.BARCODE_FILE_FORMAT_DESCRIPTION
                    + " Barcode file will overwrite any of Read Group arguments for the same information. "
                    // TODO: remove WARNING if a different pipeline is implemented
                    + "WARNING: this file should contain all the barcodes present in the multiplexed file.")
    public String barcodeFile;

    private static final String SPECIFY_MORE_THAN_ONCE_DOC_END =
            "Specify more than once for apply a different threshold to several indexes.";

    @Argument(fullName = "maximumMismatches", shortName = "mm", optional = true,
            doc = "Maximum number of mismatches allowed for a matched barcode. "
                    + SPECIFY_MORE_THAN_ONCE_DOC_END)
    public List<Integer> maximumMismatches = new ArrayList<>(Collections.singleton(0));

    @Argument(fullName = "minimumDistance", shortName = "md", optional = true,
            doc = "Minimum distance (difference in number of mismatches) between the best match and the second. "
                    + SPECIFY_MORE_THAN_ONCE_DOC_END)
    public List<Integer> minimumDistance = new ArrayList<>(Collections.singleton(1));

    @Argument(fullName = "maximumN", shortName = "maxN", optional = true, doc = "Maximum number of unknown bases (Ns) allowed in the barcode to consider them. If 'null', no threshold will be applied.")
    public Integer maximumN = null;

    @Argument(fullName = "nNoMismatch", shortName = "nnm", optional = true, doc = "Do not count unknown bases (Ns) as mismatch.")
    public boolean nNoMismatch = false;

    // barcode arguments
    @Argument(fullName = "runName", shortName = "runName", optional = true, doc = "Run name to add to the ID in the read group information.")
    public String runID = null;

    @ArgumentCollection
    public ReadGroupArgumentCollection rgArguments = new ReadGroupArgumentCollection();

    /**
     * Validates that the arguments are within the range.
     *
     * @throws CommandLineException.BadArgumentValue if arguments are out of range.
     */
    public void validateArguments() {
        // TODO: this should be change once the range is specify
        if (maximumN != null && maximumN < 0) {
            throw new CommandLineException.BadArgumentValue("--maximumN",
                    String.valueOf(maximumN),
                    "Maximum number of Ns should be a positive integer.");
        }
        if (maximumMismatches.isEmpty()
                || maximumMismatches.stream().filter(i -> i < 0).findAny().isPresent()) {
            throw new CommandLineException.BadArgumentValue("--maximumMismatches",
                    maximumMismatches.toString(),
                    "Maximum number of mismatches should be a positive integer.");
        }
        if (minimumDistance.isEmpty()
                || minimumDistance.stream().filter(i -> i < 1).findAny().isPresent()) {
            throw new CommandLineException.BadArgumentValue("--minimumDistance",
                    minimumDistance.toString(),
                    "Minimum distance should be at least 1 to avoid ambiguous barcodes.");
        }
    }

    public BarcodeDecoder getBarcodeDecoder() {
        final BarcodeDictionary dictionary = BarcodeDictionaryFactory
                .fromFile(org.broadinstitute.hellbender.utils.io.IOUtils
                        .getPath(barcodeFile), runID, rgArguments);

        // checking number of barcodes
        final int numberOfBarcodes = dictionary.getNumberOfBarcodes();
        final int[] maxMismatchArg =
                paramMatchingNumberOfBarcodes("maximumMismatches", numberOfBarcodes,
                        maximumMismatches);

        final int[] minDistArg =
                paramMatchingNumberOfBarcodes("minimumDistance", numberOfBarcodes,
                        minimumDistance);

        return new BarcodeDecoder(dictionary,
                (maximumN == null) ? Integer.MAX_VALUE : maximumN,
                !nNoMismatch, maxMismatchArg, minDistArg);
    }


    private final static int[] paramMatchingNumberOfBarcodes(final String argName,
            final int numberOfBarcodes, final List<Integer> paramValue) {
        if (paramValue.size() == 1 && numberOfBarcodes != 1) {
            final int value = paramValue.get(0);
            return IntStream.range(0, numberOfBarcodes).map(i -> value).toArray();
        } else if (paramValue.size() != numberOfBarcodes) {
            // TODO: this should be change to a BadArgument exception once the GATK framework handle them properly
            throw new UserException(
                    "--" + argName + "specified " + paramValue.size() + "times for "
                            + numberOfBarcodes + " barcodes.");
        }
        return paramValue.stream().mapToInt(Integer::intValue).toArray();
    }

}
