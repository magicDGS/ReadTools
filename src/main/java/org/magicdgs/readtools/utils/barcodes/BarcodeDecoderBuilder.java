package org.magicdgs.readtools.utils.barcodes;

import org.magicdgs.readtools.utils.barcodes.legacy.dictionary.decoder.LegacyBarcodeDecoder;

import org.broadinstitute.barclay.utils.Utils;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class BarcodeDecoderBuilder {

    private BarcodeSet barcodeSet;
    private int maximumUnknownBases;
    private boolean treatNsAsMismatches;
    private int[] maximumMismatches;
    private int[] minimumDifferenceWithSecond;

    public BarcodeDecoderBuilder setBarcodeSet(final BarcodeSet barcodeSet) {
        // TODO: check non-empty
        this.barcodeSet = Utils.nonNull(barcodeSet);
        return this;
    }

    public BarcodeDecoderBuilder setMaximumUnknownBases(final int maxN) {
        // TODO: check range
        this.maximumUnknownBases = maxN;
        return this;
    }

    public BarcodeDecoderBuilder setTreatNsAsMismatches(final boolean nAsMismatches) {
        // TODO: check range
        this.treatNsAsMismatches = nAsMismatches;
        return this;
    }

    public BarcodeDecoderBuilder setMaximumMismatches(final int[] maxMismatches) {
        // TODO: check range
        this.maximumMismatches = Utils.nonNull(maxMismatches);
        return this;
    }

    public BarcodeDecoderBuilder setMinimumDifferenceWithSecond(final int[] minDifferenceWithSecond) {
        // TODO: check range
        this.minimumDifferenceWithSecond = Utils.nonNull(minDifferenceWithSecond);
        return this;
    }

    public BarcodeDecoder build() {
        return getLegacyBarcodeDecoder();
    }

    private BarcodeDecoder getLegacyBarcodeDecoder() {
        return new LegacyBarcodeDecoder(
                barcodeSet,
                maximumUnknownBases,
                treatNsAsMismatches,
                maximumMismatches,
                minimumDifferenceWithSecond);
    }
}
