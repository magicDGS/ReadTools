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

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.List;
import java.util.function.Consumer;

/**
 * Use different tags to encode and update the {@link htsjdk.samtools.SAMTag#BC} tag (and
 * {@link htsjdk.samtools.SAMTag#QT} if requested). In addition, used tags are discarded because
 * they are already encoded in the raw BC/QT tags and they are not longer needed.
 *
 * This may be useful only in several cases:
 *
 * - When output a FASTQ file, some other tags want to be used into the read name.
 * - For old BAM data where barcodes where stored in the deprecated tag {@link
 * htsjdk.samtools.SAMTag#RT}.
 * - When other tools were used to encode a read. For example,
 * <a href=http://gq1.github.io/illumina2bam/index.html>illumina2bam</a> uses BC/QT to encode the
 * first index/quality, and B2/Q2 to encode the second. By providing to this transformer a list
 * with BC and B2 (and optional QT and Q2), this will encode properly in the BC tag the double
 * index.
 *
 * Note: usage of this transformer is discouraged for other purposes, because it removes
 * information encoded in other tags.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class FixRawBarcodeTagsReadTransformer implements ReadTransformer {
    private static final long serialVersionUID = 1L;

    private final List<String> tagsToUse;
    private final List<String> qualityTags;
    private final Consumer<GATKRead> updater;


    /** Constructor for the transformer with a list of tags to use (not qualities updated). */
    public FixRawBarcodeTagsReadTransformer(final List<String> tagsToUse) {
        Utils.nonEmpty(tagsToUse, "tagsToUse");
        this.tagsToUse = tagsToUse;
        this.qualityTags = null;
        this.updater = this::updateBc;
    }

    /** Constructor for the transformer with a list of tags to use and associated qualities. */
    public FixRawBarcodeTagsReadTransformer(final List<String> tagsToUse,
            final List<String> qualityTags) {
        Utils.nonEmpty(tagsToUse, "tagsToUse");
        Utils.nonEmpty(qualityTags, "qualityTags");
        Utils.validateArg(tagsToUse.size() == qualityTags.size(), "");
        this.tagsToUse = tagsToUse;
        this.qualityTags = qualityTags;
        this.updater = this::updateBcAndQt;
    }

    /**
     * Transforms the read in place getting the barcodes from the provided tags and removing them.
     *
     * @see RTReadUtils#getBarcodesFromTags(GATKRead, List)
     * @see RTReadUtils#getBarcodesAndQualitiesFromTags(GATKRead, List, List)
     * @see RTReadUtils#addBarcodesTagToRead(GATKRead, String[])
     * @see RTReadUtils#addBarcodeWithQualitiesTagsToRead(GATKRead, String[], String[])
     */
    @Override
    public GATKRead apply(final GATKRead read) {
        updater.accept(read);
        return read;
    }

    private void updateBc(final GATKRead read) {
        // get the barcode for the tags, joining them according to the specs
        final String[] barcodes = RTReadUtils.getBarcodesFromTags(read, tagsToUse);
        // clean all the tags used for the barcodes (even if it is BC)
        tagsToUse.forEach(tag -> read.setAttribute(tag, (String) null));
        // update the BC tag using the obtained barcodes
        RTReadUtils.addBarcodesTagToRead(read, barcodes);
    }

    private void updateBcAndQt(final GATKRead read) {
        // get the barcode for the tags, joining them according to the specs
        final Pair<String[], String[]> bcAndQt =
                RTReadUtils.getBarcodesAndQualitiesFromTags(read, tagsToUse, qualityTags);
        // clean all the tags used for the barcodes/qualities (even if it is BC/QT)
        tagsToUse.forEach(tag -> read.setAttribute(tag, (String) null));
        qualityTags.forEach(tag -> read.setAttribute(tag, (String) null));
        // update the BC tag using the obtained barcodes
        RTReadUtils.addBarcodeWithQualitiesTagsToRead(read, bcAndQt.getLeft(), bcAndQt.getRight());
    }

}
