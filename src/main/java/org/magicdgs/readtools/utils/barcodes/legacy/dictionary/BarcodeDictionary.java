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

package org.magicdgs.readtools.utils.barcodes.legacy.dictionary;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.barcodes.BarcodeSet;
import org.magicdgs.readtools.utils.barcodes.BarcodeSetFactory;

import htsjdk.samtools.SAMReadGroupRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classs for store a barcode dictionary
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeDictionary implements BarcodeSet {

    /**
     * New name for the samples
     */
    private final List<SAMReadGroupRecord> sampleRecord;

    /** The unknown barcode for this dictionary. */
    private final SAMReadGroupRecord unknownBarcode;

    /**
     * Array which contains the barcodes. The lenght is the number of barcodes used, and the
     * internal array contain the
     * associated barcode for each sample
     */
    private final List<List<String>> barcodes;

    /**
     * Cached map between combined barcodes and read groups
     */
    private final Map<String, SAMReadGroupRecord> barcodeRGmap = new LinkedHashMap<>();

    /**
     * Cached barcode set(s) for fast access
     */
    private final List<Set<String>> barcodesSets = new ArrayList<>();

    /**
     * Protected constructor. For construct an instance, use {@link BarcodeSetFactory}
     *
     * @param samples        the sample names.
     * @param barcodes       the barcodes.
     * @param unknownBarcode the unknown barcode to assign to unknonw samples.
     */
    public BarcodeDictionary(final List<SAMReadGroupRecord> samples,
            final List<List<String>> barcodes, final SAMReadGroupRecord unknownBarcode) {
        this.sampleRecord = samples;
        this.barcodes = barcodes;
        this.unknownBarcode = unknownBarcode;
    }

    /**
     * Initialize the barcode-RG map for the dictionary to cached
     */
    private void initBarcodeRGmap() {
        // init the barcode-rg map
        for (int i = 0; i < size(); i++) {
            barcodeRGmap.put(getJoinedBarcodesForSample(i), get(i));
        }
    }

    /**
     * Get the number of barcodes in this dictionary
     *
     * @return the number of barcodes
     */
    @Override
    public int getMaxNumberOfIndexes() {
        return barcodes.size();
    }

    /**
     * Get the sample names in order
     *
     * @return the sample names
     */
    @Override
    public List<SAMReadGroupRecord> asReadGroupList() {
        return Collections.unmodifiableList(sampleRecord);
    }

    /**
     * Get the number of samples in this dictionary associated with a different barcode
     *
     * @return the number of samples
     */
    @Override
    public int size() {
        return sampleRecord.size();
    }

    /**
     * Get the barcodes associated with certain sample
     *
     * @param sampleIndex the sample index
     *
     * @return the barcodes for the sample
     */
    @Override
    public List<String> getAllBarcodesForSample(final int sampleIndex) {
        return barcodes.stream().map(l -> l.get(sampleIndex)).collect(Collectors.toList());
    }

    /**
     * Get the name for a concrete sample
     *
     * @param sampleIndex the sample index
     *
     * @return the read group of the sample
     */
    @Override
    public SAMReadGroupRecord get(final int sampleIndex) {
        return sampleRecord.get(sampleIndex);
    }

    @Override
    public SAMReadGroupRecord getUnknown() {
        return unknownBarcode;
    }

    /**
     * Gets the read group for a combined barcode.
     *
     * @param combinedBarcode the combined barcode.
     *
     * @return the read group associated with that barcode; if not found it returns the unknown r
     * ead group (see {@link #getUnknown()}).
     */
    @Override
    public SAMReadGroupRecord getReadGroupForJoinedBarcode(final String combinedBarcode) {
        if (barcodeRGmap.isEmpty()) {
            initBarcodeRGmap();
        }
        return (barcodeRGmap.containsKey(combinedBarcode)) ?
                barcodeRGmap.get(combinedBarcode) :
                unknownBarcode;
    }

    /**
     * Get the barcodes associated with certain sample pasted together
     *
     * @param sampleIndex the sample index
     *
     * @return the combined barcodes for the sample
     */
    @Override
    public String getJoinedBarcodesForSample(final int sampleIndex) {
        return barcodes.stream().map(l -> l.get(sampleIndex))
                .collect(Collectors.joining(RTDefaults.BARCODE_INDEX_DELIMITER));
    }

    /**
     * Check if the provided barcode is unique for that index
     *
     * @param barcode the barcode to test
     * @param index   0-based index
     *
     * @return <code>true</code> if the barcode is unique; <code>false</code> otherwise
     */
    @Override
    public boolean isBarcodeUniqueInAt(final String barcode, final int index) {
        return Collections.frequency(barcodes.get(index), barcode) == 1;
    }

    /**
     * Get the first, second... barcodes (0-indexed)
     *
     * @param index the index
     *
     * @return the list with the barcodes associated with each sample
     */
    @Override
    public List<String> getSampleBarcodesForIndex(final int index) {
        return barcodes.get(index);
    }

    /**
     * Get the first, second... barcodes (0-indexed) but reducing the complexity
     *
     * @param index the index
     *
     * @return a set representation of the index barcodes
     */
    @Override
    public Set<String> getSetBarcodesForIndex(final int index) {
        if (barcodesSets.isEmpty()) {
            initSets();
        }
        return barcodesSets.get(index);
    }

    /**
     * Initialize the sets for the barcodes
     */
    private void initSets() {
        barcodes.forEach(l -> barcodesSets.add(new LinkedHashSet<>(l)));
    }

    /**
     * String representation of the dictionary, that is the mapping between the combined barcode
     * (result of {@link
     * #getJoinedBarcodesForSample(int)} (int)}) and the samples as {@link htsjdk.samtools.SAMReadGroupRecord}
     *
     * @return the short representation
     */
    @Override
    public String toString() {
        if (barcodeRGmap.isEmpty()) {
            initBarcodeRGmap();
        }
        return barcodeRGmap.toString();
    }
}
