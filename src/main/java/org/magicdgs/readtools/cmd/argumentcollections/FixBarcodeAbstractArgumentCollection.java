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

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.utils.read.RTReadUtils;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixRawBarcodeTagsReadTransformer;
import org.magicdgs.readtools.utils.read.transformer.barcodes.FixReadNameBarcodesReadTransformer;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;
import scala.Tuple2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Abstract argument collection for fixing barcodes that are encoded in different places than the
 * {@link RTReadUtils#RAW_BARCODE_TAG} tag.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class FixBarcodeAbstractArgumentCollection implements Serializable {
    private static final long serialVersionUID = 1L;

    @Argument(fullName = RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME, shortName = RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME, optional = true, common = true,
            doc = "Include the barcodes encoded in this tag(s) in the read name. Note: this is not necessary for input FASTQ files. WARNING: this tag(s) will be removed/updated as necessary.",
            mutex = {RTStandardArguments.USER_READ_NAME_BARCODE_NAME})
    public List<String> rawBarcodeTags = new ArrayList<>(RTReadUtils.RAW_BARCODE_TAG_LIST);

    @Argument(fullName = RTStandardArguments.USER_READ_NAME_BARCODE_NAME, shortName = RTStandardArguments.USER_READ_NAME_BARCODE_NAME, optional = true, common = true,
            doc = "Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files.",
            mutex = {RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME})
    public boolean useReadNameBarcode = false;

    // cached transformer, initialized if needed
    private ReadTransformer transformer = null;

    /** Logger for the class. */
    protected final Logger logger = LogManager.getLogger(this.getClass());

    /** Gets the raw barcode quality tags if they are provided by the command line. */
    protected abstract List<String> getRawBarcodeQualityTags();

    /**
     * Fix the single-end barcodes' tags using the provided arguments.
     *
     * @param singleEnd single-end read.
     *
     * @return the same read modified in-place.
     */
    public GATKRead fixBarcodeTags(final GATKRead singleEnd) {
        // this rely on in place transformation
        getFixBarcodeReadTransformer().apply(singleEnd);
        return singleEnd;
    }

    /**
     * Fix the pair-end barcodes' tags using the provided arguments.
     *
     * @param reads pair-end reads.
     *
     * @return the same tuple, with the reads modified in-place.
     */
    public Tuple2<GATKRead, GATKRead> fixBarcodeTags(final Tuple2<GATKRead, GATKRead> reads) {
        // this rely on in place transformation
        getFixBarcodeReadTransformer().apply(reads._1);
        getFixBarcodeReadTransformer().apply(reads._2);
        fixBarcodeForPair(reads._1, reads._2);
        return reads;
    }

    /**
     * Fix the barcodes for the pair. Default implementation only fix the
     * {@link RTReadUtils#RAW_BARCODE_TAG} tag.
     */
    protected void fixBarcodeForPair(final GATKRead read1, final GATKRead read2) {
        RTReadUtils.fixPairTag(RTReadUtils.RAW_BARCODE_TAG, read1, read2);
    }

    /**
     * Validate the arguments after parsing, and throw a CommandLineException if:
     *
     * - Repeated raw barcode sequence tag(s) are found.
     * - No valid tag name(s) are provided.
     */
    public void validateArguments() {
        // non-duplicated tags
        final Set<String> duplicated = Utils.getDuplicatedItems(rawBarcodeTags);
        if (!duplicated.isEmpty()) {
            throw new CommandLineException.BadArgumentValue(
                    RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME,
                    "contain duplicated tags: " + duplicated);
        }

        // valid tag names
        rawBarcodeTags.forEach(rbt -> validateTagArgument(rbt,
                RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME));
    }

    /** Gets a ReadTransformer to fix the barcodes. */
    @VisibleForTesting
    final ReadTransformer getFixBarcodeReadTransformer() {
        if (transformer == null) {
            // if it is using the read names, apply the simplest fix
            if (useReadNameBarcode) {
                logger.debug("Using barcodes from read names");
                transformer = new FixReadNameBarcodesReadTransformer();
            } else if (!rawBarcodeTags.isEmpty()) {

                // if there are barcode tags, try to get qualities too
                final List<String> rawBarcodeQualsTags = getRawBarcodeQualityTags();

                // if no quality tags, not fixing; otherwise fix and log
                if (rawBarcodeQualsTags.isEmpty()) {
                    logger.warn("Quality tags are not updated.");
                    if (!rawBarcodeTags.equals(RTReadUtils.RAW_BARCODE_TAG_LIST)) {
                        logger.debug("Using barcode tags: {}", () -> rawBarcodeTags);
                        transformer = new FixRawBarcodeTagsReadTransformer(rawBarcodeTags);
                    }
                } else {
                    logger.debug("Using barcode tags: {}", () -> rawBarcodeTags);
                    logger.debug("Using quality tags: {}", () -> rawBarcodeQualsTags);
                    transformer = new FixRawBarcodeTagsReadTransformer(rawBarcodeTags,
                            rawBarcodeQualsTags);
                }
            }
            if (transformer == null) {
                logger.debug("Not using barcode tags: {}", () -> rawBarcodeTags);
                transformer = ReadTransformer.identity();
            }
        }
        return transformer;
    }

    /**
     * Gets an argument collection implementation.
     *
     * @param fixQualitiesArgument if {@code true}, provides an argument to fix the qualities.
     */
    public static final FixBarcodeAbstractArgumentCollection getArgumentCollection(
            final boolean fixQualitiesArgument) {
        return (fixQualitiesArgument)
                ? new FixBarcodeWithQualitiesArgumentCollection()
                : new FixSimpleBarcodeArgumentCollection();
    }


    /** Implementation without barcode qualities. */
    private static final class FixSimpleBarcodeArgumentCollection
            extends FixBarcodeAbstractArgumentCollection {

        @Override
        protected List<String> getRawBarcodeQualityTags() {
            logger.debug("No barcode quality tags are allowed in this argument collection");
            return Collections.emptyList();
        }
    }

    /** Implementation with barocde qualities */
    private static final class FixBarcodeWithQualitiesArgumentCollection
            extends FixBarcodeAbstractArgumentCollection {

        @Argument(fullName = RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME, shortName = RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME, optional = true, common = true,
                doc = "Use the qualities encoded in this tag(s) as raw barcode qualities. Requires --"
                        + RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME
                        + ". WARNING: this tag(s) will be removed/updated as necessary.",
                mutex = {RTStandardArguments.USER_READ_NAME_BARCODE_NAME})
        public List<String> rawBarcodeQualsTags = new ArrayList<>();

        @Override
        protected List<String> getRawBarcodeQualityTags() {
            return rawBarcodeQualsTags;
        }

        /** Overrides to fix also the quality tag. */
        @Override
        public void fixBarcodeForPair(final GATKRead read1, final GATKRead read2) {
            super.fixBarcodeForPair(read1, read2);
            RTReadUtils.fixPairTag(RTReadUtils.RAW_BARCODE_QUALITY_TAG, read1, read2);
        }

        /**
         * {@inheritDoc}
         * In addition, it validates the raw barcode quality tag(s) and throws if:
         *
         * - Barcode qualities tag(s) were provided for no barcode sequence tag(s).
         * - If barcode sequence and quality tag(s) have different lengths.
         */
        @Override
        public void validateArguments() {
            super.validateArguments();
            // non-duplicated tags
            final Set<String> duplicated = Utils.getDuplicatedItems(rawBarcodeQualsTags);
            if (!duplicated.isEmpty()) {
                throw new CommandLineException.BadArgumentValue(
                        RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME,
                        "contain duplicated tags: " + duplicated);
            }

            // valid quality tag names
            rawBarcodeQualsTags.forEach(rbt -> validateTagArgument(rbt,
                    RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME));

            // if quals are used, requires at least a raw barcode tag
            if (rawBarcodeTags.isEmpty() && !rawBarcodeQualsTags.isEmpty()) {
                throw new CommandLineException.MissingArgument(
                        RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME,
                        "required if --" + RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME
                                + "is specified.");
            }

            // the same number of barcode/quals tags
            // TODO 20-03-2017: maybe we should allow different number of tags (see issue https://github.com/magicDGS/ReadTools/issues/157)
            if (!rawBarcodeQualsTags.isEmpty()
                    && rawBarcodeTags.size() != rawBarcodeQualsTags.size()) {
                throw new CommandLineException.BadArgumentValue(
                        "--" + RTStandardArguments.RAW_BARCODE_SEQUENCE_TAG_NAME
                                + " and --" + RTStandardArguments.RAW_BARCODE_QUALITIES_TAG_NAME
                                + " should be provided the same number of times.");
            }
        }
    }

    // helper method to thrown CommandLineException if the tag is not a legal one based on the SAM specs
    private static void validateTagArgument(final String tag, final String argName) {
        try {
            ReadUtils.assertAttributeNameIsLegal(tag);
        } catch (IllegalArgumentException e) {
            throw new CommandLineException.BadArgumentValue(argName, e.getMessage());
        }
    }

}
