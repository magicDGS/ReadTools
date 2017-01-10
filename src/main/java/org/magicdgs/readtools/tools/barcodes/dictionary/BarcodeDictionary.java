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
 */
package org.magicdgs.readtools.tools.barcodes.dictionary;

import org.magicdgs.readtools.utils.fastq.BarcodeMethods;

import htsjdk.samtools.SAMReadGroupRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classs for store a barcode dictionary
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeDictionary {

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
     * Protected constructor. For construct an instance, use {@link BarcodeDictionaryFactory}
     *
     * @param samples        the sample names.
     * @param barcodes       the barcodes.
     * @param unknownBarcode the unknown barcode to assign to unknonw samples.
     */
    protected BarcodeDictionary(final List<SAMReadGroupRecord> samples,
            final List<List<String>> barcodes, final SAMReadGroupRecord unknownBarcode) {
        this.sampleRecord = samples;
        this.barcodes = barcodes;
        this.unknownBarcode = unknownBarcode;
    }

    /**
     * Protected constructor. For get an instance of a dictionary, use {@link
     * BarcodeDictionaryFactory}
     *
     * @param run           the run name for the samples; if <code>null</code> it will be igonored
     * @param samples       the sample names
     * @param barcodes      the barcodes
     * @param libraries     the library for each barcode; if <code>null</code>, the library is the
     *                      samples_combinedBarcodes
     * @param readGroupInfo the additional information for the read group
     */
    protected BarcodeDictionary(final String run, final List<String> samples,
            final List<List<String>> barcodes, final List<String> libraries,
            final SAMReadGroupRecord readGroupInfo) {
        this(new ArrayList<>(samples.size()), barcodes, readGroupInfo);
        initReadGroups(run, samples, libraries, readGroupInfo);
    }

    /**
     * Create the read group for the samples: the sample name will be in the SM tag, the
     * sampleName_combinedBarcode the
     * ID and the rest of tags will come from the read group. The barcode field should be
     * initialized before calling
     * this method
     *
     * @param run           the run name for the samples; if <code>null</code> it will be igonored
     * @param libraries     the library for each barcode; if <code>null</code> or empty, the
     *                      library
     *                      is the
     *                      samples_combinedBarcodes
     * @param samples       the sample name
     * @param readGroupInfo the read group information
     */
    private void initReadGroups(final String run, final List<String> samples,
            final List<String> libraries, final SAMReadGroupRecord readGroupInfo) {
        for (int i = 0; i < samples.size(); i++) {
            final String sampleBarcode =
                    String.format("%s_%s", samples.get(i), getCombinedBarcodesFor(i));
            final SAMReadGroupRecord rg = new SAMReadGroupRecord(
                    (run == null) ? sampleBarcode : String.format("%s_%s", run, sampleBarcode),
                    readGroupInfo);
            rg.setSample(samples.get(i));
            if (libraries != null && !libraries.isEmpty()) {
                rg.setLibrary(libraries.get(i));
            } else {
                rg.setLibrary(sampleBarcode);
            }
            sampleRecord.add(rg);
        }
    }

    /**
     * Initialize the barcode-RG map for the dictionary to cached
     */
    private void initBarcodeRGmap() {
        // init the barcode-rg map
        for (int i = 0; i < numberOfSamples(); i++) {
            barcodeRGmap.put(getCombinedBarcodesFor(i), getReadGroupFor(i));
        }
    }

    /**
     * Get the number of barcodes in this dictionary
     *
     * @return the number of barcodes
     */
    public int getNumberOfBarcodes() {
        return barcodes.size();
    }

    /**
     * Get the sample names in order
     *
     * @return the sample names
     */
    public List<String> getSampleNames() {
        return sampleRecord.stream().map(SAMReadGroupRecord::getSample)
                .collect(Collectors.toList());
    }

    /**
     * Get the sample names in order
     *
     * @return the sample names
     */
    public List<SAMReadGroupRecord> getSampleReadGroups() {
        return sampleRecord;
    }

    /**
     * Get the number of samples in this dictionary associated with a different barcode
     *
     * @return the number of samples
     */
    public int numberOfSamples() {
        return sampleRecord.size();
    }

    /**
     * Get the number of unique samples in this dictionary
     *
     * @return the effective number of samples
     */
    public int numberOfUniqueSamples() {
        // will it be better to cache this value??
        return new HashSet<>(sampleRecord).size();
    }

    /**
     * Get the barcodes associated with certain sample
     *
     * @param sampleIndex the sample index
     *
     * @return the barcodes for the sample
     */
    public String[] getBarcodesFor(final int sampleIndex) {
        return barcodes.stream().map(l -> l.get(sampleIndex)).toArray(String[]::new);
    }

    /**
     * Get the name for a concrete sample
     *
     * @param sampleIndex the sample index
     *
     * @return the read group of the sample
     */
    public SAMReadGroupRecord getReadGroupFor(final int sampleIndex) {
        return sampleRecord.get(sampleIndex);
    }

    public SAMReadGroupRecord getUnknownReadGroup() {
        return unknownBarcode;
    }

    /**
     * Gets the read group for a combined barcode.
     *
     * @param combinedBarcode the combined barcode.
     *
     * @return the read group associated with that barcode; if not found it returns the unknown r
     * ead group (see {@link #getUnknownReadGroup()}).
     */
    public SAMReadGroupRecord getReadGroupFor(final String combinedBarcode) {
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
    public String getCombinedBarcodesFor(final int sampleIndex) {
        return BarcodeMethods.joinBarcodes(getBarcodesFor(sampleIndex));
    }

    /**
     * Check if the provided barcode is unique for that index
     *
     * @param barcode the barcode to test
     * @param index   0-based index
     *
     * @return <code>true</code> if the barcode is unique; <code>false</code> otherwise
     */
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
    public List<String> getBarcodesFromIndex(final int index) {
        return barcodes.get(index);
    }

    /**
     * Get the first, second... barcodes (0-indexed) but reducing the complexity
     *
     * @param index the index
     *
     * @return a set representation of the index barcodes
     */
    public Set<String> getSetBarcodesFromIndex(final int index) {
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
     * #getCombinedBarcodesFor(int)}) and the samples as {@link htsjdk.samtools.SAMReadGroupRecord}
     *
     * @return the short representation
     */
    public String toString() {
        if (barcodeRGmap.isEmpty()) {
            initBarcodeRGmap();
        }
        return barcodeRGmap.toString();
    }
}
