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

package org.magicdgs.readtools.utils.read.transformer.barcodes;

import org.magicdgs.readtools.utils.read.RTReadUtils;

import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Fix barcodes stored in the read read name (Illumina formatted) putting them into the
 * default barcode tag ({@link RTReadUtils#RAW_BARCODE_TAG}), and removing them from the name.
 *
 * This may be useful when reading a BAM file mapped from a FASTQ file where the barcodes are keep
 * in the read name. For example, <a href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0072614">Distmap</a>
 * uses a file format in a Hadoop cluster where the information will be lost if it is not stored in
 * the read name, so it should be maintained and then fixed with this transformer.
 *
 * Note: although this transformer will do nothing if there is no barcode encoded in the read name,
 * its usage is discouraged except for mapped BAM files without the BC informationm, because while
 * reading FASTQ files this processing is already applied.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class FixReadNameBarcodesReadTransformer implements ReadTransformer {
    private static final long serialVersionUID = 1L;

    /**
     * Transforms the read in place getting the barcodes from the read name and updating the read
     * with them.
     *
     * @see RTReadUtils#extractBarcodesFromReadName(GATKRead)
     * @see RTReadUtils#addBarcodesTagToRead(GATKRead, String[])
     */
    @Override
    public GATKRead apply(final GATKRead read) {
        final String[] barcodes = RTReadUtils.extractBarcodesFromReadName(read);
        RTReadUtils.addBarcodesTagToRead(read, barcodes);
        return read;
    }
}
