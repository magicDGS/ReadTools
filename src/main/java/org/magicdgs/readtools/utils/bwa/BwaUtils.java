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

package org.magicdgs.readtools.utils.bwa;

import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.bwa.BwaMemIndex;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class BwaUtils {

    // cannot be instatiated
    private BwaUtils() {}

    /**
     * Filter for only primary alignemntes
     *
     * <P>Note: this will be ported to GATK4, to its {@link ReadFilterLibrary}.
     */
    // TODO: remove after https://github.com/broadinstitute/gatk/pull/3195
    public static final ReadFilter PRIMARY_LINE_FILTER = ReadFilterLibrary.NOT_SECONDARY_ALIGNMENT
            .and(ReadFilterLibrary.NOT_SUPPLEMENTARY_ALIGNMENT);

    /**
     * Gets the default index image name for the provided FASTA.
     *
     * <p>Note: this method will be ported to the gatk-bwamem-jni, and it will be removed
     * eventually.
     *
     * @param fasta the location of the fasta reference file.
     */
    // TODO: port to gatk-bwamem-jni
    public static String getDefaultIndexImageNameFromFastaFile(final String fasta) {
        final Optional<String> extension = BwaMemIndex.FASTA_FILE_EXTENSIONS.stream()
                .filter(fasta::endsWith).findFirst();
        if (!extension.isPresent()) {
            throw new UserException(String.format(
                    "the fasta file provided '%s' does not have any of the standard fasta extensions: %s",
                    fasta,
                    BwaMemIndex.FASTA_FILE_EXTENSIONS.stream().collect(Collectors.joining(", "))));
        }
        final String prefix = fasta.substring(0, fasta.length() - extension.get().length());
        return prefix + BwaMemIndex.IMAGE_FILE_EXTENSION;
    }

}
