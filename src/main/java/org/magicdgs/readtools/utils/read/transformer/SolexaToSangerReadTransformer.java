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

package org.magicdgs.readtools.utils.read.transformer;

import htsjdk.samtools.SAMUtils;
import htsjdk.samtools.util.SolexaQualityConverter;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Transformer that change the read encoding from Solexa to Standard PHRED score. If the quality is
 * incorrectly formatted, it will throw an error.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class SolexaToSangerReadTransformer implements ReadTransformer {

    // the quality converter
    private final static SolexaQualityConverter SOLEXA_QUALITY_CONVERTER =
            SolexaQualityConverter.getSingleton();

    // this is the value of the minimum quality in Solexa possible before conversion (';')
    // this is necessary because the quality converter transform lower qualities to '!'
    // and cannot be catch as an exception
    private final static byte MIN_SOLEXA_BEFORE_CONVERSION = 26;

    @Override
    public GATKRead apply(final GATKRead read) {
        // get the qualities
        final byte[] quals = read.getBaseQualities();
        try {
            // transform them in place
            for (int i = 0; i < quals.length; ++i) {
                if (quals[i] < MIN_SOLEXA_BEFORE_CONVERSION) {
                    // throw an exception if they are misencoded
                    throwException();
                }
                // convert to Solexa
                quals[i] = SOLEXA_QUALITY_CONVERTER
                        .solexaCharToPhredBinary((byte) SAMUtils.phredToFastq(quals[i]));
            }
        } catch (IndexOutOfBoundsException e) {
            // if there is an index exception, that means that the qualities are not correctly encoded
            throwException();
        }
        // set the base qualities
        read.setBaseQualities(quals);
        return read;
    }

    // throws the exception for Solexa mis-encoded qualities
    private static void throwException() {
        throw new UserException.BadInput(
                "while converting Solexa base qualities we encountered a read that was correctly encoded; we cannot handle such a mixture of reads so unfortunately the input must be fixed with some other tool.");
    }
}
