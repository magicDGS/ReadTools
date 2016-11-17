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
package org.magicdgs.io.writers.fastq;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.utils.misc.IOUtils;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;
import org.magicdgs.readtools.utils.record.FastqRecordUtils;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.util.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * FastqWriterFactory for ReadTools
 *
 * @author Daniel Gómez-Sánchez
 * @deprecated for create writers, use {@link ReadWriterFactory}.
 */
@Deprecated
public class ReadToolsFastqWriterFactory {

    private final Logger logger = LogManager.getLogger(ReadToolsFastqWriterFactory.class);

    /**
     * The underlying factory
     */
    private final ReadWriterFactory FACTORY;

    /**
     * By default, the output will be GZIPPED if a prefix is provided; if a file is provided, it is
     * not needed
     */
    private boolean GZIP_OUTPUT = true;

    /**
     * Initialize a new factory
     */
    public ReadToolsFastqWriterFactory() {
        FACTORY = new ReadWriterFactory();
    }

    /** Sets the force overwrite. */
    public ReadToolsFastqWriterFactory setForceOverwrite(final boolean forceOverwrite) {
        FACTORY.setForceOverwrite(forceOverwrite);
        return this;
    }

    /**
     * Sets whether or not to use async io (i.e. a dedicated thread per writer.
     */
    public ReadToolsFastqWriterFactory setUseAsyncIo(final boolean useAsyncIo) {
        FACTORY.setUseAsyncIo(useAsyncIo);
        return this;
    }

    /**
     * If true, compute MD5 and write appropriately-named file when file is closed.
     */
    public ReadToolsFastqWriterFactory setCreateMd5(final boolean createMd5) {
        FACTORY.setCreateMd5File(createMd5);
        return this;
    }

    /**
     * If <code>true</code> the output generated from a prefix will be gzipped.
     */
    public ReadToolsFastqWriterFactory setGzipOutput(final boolean gzip) {
        GZIP_OUTPUT = gzip;
        return this;
    }

    /**
     * Create a new FastqWriter from a prefix
     *
     * @param prefix the prefix for the files
     *
     * @return a new instance of the writer
     */
    public ReadToolsFastqWriter newWriter(String prefix) throws IOException {
        final String outputName = IOUtils.makeOutputNameFastqWithDefaults(prefix, GZIP_OUTPUT);
        return new ReadToolsBasicFastqWriter(FACTORY.openFastqWriter(outputName));
    }

    /**
     * Create a new pair writer
     *
     * @param prefix the prefix for the files
     *
     * @return a new instance of the writer
     */
    public ReadToolsFastqWriter newPairWriter(String prefix) throws IOException {
        final FastqWriter pair1 = newWriter(prefix + "_1");
        final FastqWriter pair2 = newWriter(prefix + "_2");
        final String seOutputName =
                IOUtils.makeOutputNameFastqWithDefaults(prefix + "_SE", GZIP_OUTPUT);
        Lazy<FastqWriter> single = new Lazy<>(() -> FACTORY.openFastqWriter(seOutputName));
        return new PairFastqWriters(pair1, pair2, single);
    }

    /**
     * Create a split writer by barcode
     *
     * @param prefix     the prefix for the files
     * @param dictionary the barcode dictionary with the barcodes
     * @param paired     <code>true</code> indicates that paired writers are used; otherwise a
     *                   default is used
     *
     * @return a new instance of the writer
     */
    public SplitFastqWriter newSplitByBarcodeWriter(String prefix, BarcodeDictionary dictionary,
            final boolean paired)
            throws IOException {
        logger.debug("Creating new SplitByBarcode barcode for {}-end",
                (paired) ? "paired" : "single");
        final Hashtable<String, FastqWriter> mapping = new Hashtable<>();
        HashMap<String, FastqWriter> sampleNames = new HashMap<>();
        for (int i = 0; i < dictionary.numberOfSamples(); i++) {
            String sample = dictionary.getSampleNames().get(i);
            if (!sampleNames.containsKey(sample)) {
                FastqWriter sampleWriter = (paired) ?
                        newPairWriter(prefix + "_" + sample) :
                        newWriter(prefix + "_" + sample);
                sampleNames.put(sample, sampleWriter);
            }
            mapping.put(dictionary.getCombinedBarcodesFor(i), sampleNames.get(sample));
        }
        // add a unknown barcode
        mapping.put(BarcodeMatch.UNKNOWN_STRING, (paired) ?
                newPairWriter(prefix + "_" + IOUtils.DISCARDED_SUFFIX) :
                newWriter(prefix + "_" + IOUtils.DISCARDED_SUFFIX));
        return new SplitFastqWriterAbstract(mapping) {

            @Override
            public void write(FastqRecord record) {
                String barcode = FastqRecordUtils.getBarcodeInName(record);
                write(getUnknownIfNoMapping(barcode), record);
            }

            @Override
            public void write(FastqPairedRecord record) {
                String barcode = FastqRecordUtils.getBarcodeInName(record);
                write(getUnknownIfNoMapping(barcode), record);
            }

            /**
             * All unknown barcodes (exact match) goes to the discarded
             * @param barcode the barcode to test if is present
             * @return the barcode if it is present, the UNKNOWN_STRING otherwise
             */
            private String getUnknownIfNoMapping(String barcode) {
                return (mapping.contains(barcode)) ? barcode : BarcodeMatch.UNKNOWN_STRING;
            }
        };
    }

    /**
     * Writer that split between assign/unknown barcodes; the mapping is "assign" and {@link
     * org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch#UNKNOWN_STRING}. By
     * default,
     * any record is correct
     * unless the unknown string is provided as identifier
     *
     * @param prefix the prefix for the files
     * @param paired <code>true</code> indicates that paired writers are used; otherwise a default
     *               is used
     *
     * @return a new instance of the writer
     */
    public SplitFastqWriter newSplitAssignUnknownBarcodeWriter(String prefix, boolean paired)
            throws IOException {
        logger.debug("Creating new Assign-Unknown barcode for {}-end",
                (paired) ? "paired" : "single");
        final Hashtable<String, FastqWriter> mapping = new Hashtable<>(2);
        mapping.put("assign", (paired) ? newPairWriter(prefix) : newWriter(prefix));
        mapping.put(BarcodeMatch.UNKNOWN_STRING, (paired) ?
                newPairWriter(prefix + "_" + IOUtils.DISCARDED_SUFFIX) :
                newWriter(prefix + "_" + IOUtils.DISCARDED_SUFFIX));
        return new SplitFastqWriterAbstract(mapping) {

            /**
             * By default, any record is correct
             * @param record the record to write
             *
             */
            @Override
            public void write(FastqRecord record) {
                super.write("assign", record);
            }

            /**
             * By default, any record is correct
             * @param record the record to write
             *
             */
            @Override
            public void write(FastqPairedRecord record) {
                super.write("assign", record);
            }

            @Override
            public void write(String identifier, FastqRecord record) {
                if (BarcodeMatch.UNKNOWN_STRING.equals(identifier)) {
                    super.write(identifier, record);
                } else {
                    write(record);
                }
            }

            @Override
            public void write(String identifier, FastqPairedRecord record) {
                if (BarcodeMatch.UNKNOWN_STRING.equals(identifier)) {
                    super.write(identifier, record);
                } else {
                    write(record);
                }
            }
        };
    }
}
