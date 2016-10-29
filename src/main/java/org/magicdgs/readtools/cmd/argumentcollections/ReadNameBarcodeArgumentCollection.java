/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.utils.fastq.BarcodeMethods;

import org.broadinstitute.hellbender.cmdline.Advanced;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Argument collection for solve issues of barcodes in the read name not with the normal encoding.
 *
 * Note: this is a temporary solution and should be use with caution.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadNameBarcodeArgumentCollection implements ArgumentCollectionDefinition {
    private static final long serialVersionUID = 1L;

    public static final String READNAME_BARCODE_SEPARATOR_NAME = "readNameBarcodeSeparator";

    /** Separator between read name and barcode. */
    @Advanced
    @Argument(fullName = READNAME_BARCODE_SEPARATOR_NAME, optional = true, doc = "String separation between the read name and the actual barcode. Default value should be suitable for most of the inputs.")
    public String barcodeSeparator = BarcodeMethods.NAME_BARCODE_SEPARATOR;

    private Pattern barcodeCompletePattern = null;
    private Pattern barcodeWithReadPairPattern = null;

    private Pattern getBarcodeCompletePattern() {
        if (barcodeCompletePattern == null) {
            barcodeCompletePattern = Pattern.compile(barcodeSeparator + "(.+)");
        }
        return barcodeCompletePattern;
    }

    private Pattern getBarcodeWithReadPairPattern() {
        if (barcodeWithReadPairPattern == null) {
            barcodeWithReadPairPattern =
                    Pattern.compile(barcodeSeparator + "(.+)" + BarcodeMethods.READ_PAIR_SEPARATOR);
        }
        return barcodeWithReadPairPattern;
    }

    /**
     * Gets the barcode (1 or 2) in the read name using the parameters specified in the command
     * line.
     *
     * @return the barcode set; {@code null} if not found.
     */
    public String[] getBarcodesInReadName(final String readName) {
        final Matcher matcher;
        if (readName.contains(BarcodeMethods.READ_PAIR_SEPARATOR)) {
            matcher = getBarcodeWithReadPairPattern().matcher(readName);
        } else {
            matcher = getBarcodeCompletePattern().matcher(readName);
        }
        if (matcher.find()) {
            return matcher.group(1).split(BarcodeMethods.BARCODE_BARCODE_SEPARATOR);
        }
        throw new UserException.BadInput("Read name does not contains a barcode: " + readName);
    }

    /** Gets the read name without the barcode using the parameters specified in the command line. */
    public String getReadNameWithoutBarcode(final String readName) {
        int index = readName.indexOf(barcodeSeparator);
        if (index != -1) {
            return readName.substring(0, index);
        }
        return readName;
    }

    /** Change the barcode in the read name and format as the standard. */
    public final String changeBarcodeToStandard(final String readName, final String newBarcode) {
        return new StringBuilder(getReadNameWithoutBarcode(readName))
                .append(BarcodeMethods.NAME_BARCODE_SEPARATOR)
                .append(newBarcode)
                .toString();
    }

}
