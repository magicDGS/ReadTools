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
package org.vetmeduni.io.writers.fastq;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.Lazy;
import htsjdk.samtools.util.Log;
import org.vetmeduni.io.FastqPairedRecord;
import org.vetmeduni.io.IOdefault;
import org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionary;
import org.vetmeduni.methods.barcodes.dictionary.MatcherBarcodeDictionary;
import org.vetmeduni.utils.misc.IOUtils;
import org.vetmeduni.utils.record.FastqRecordUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * FastqWriterFactory for ReadTools
 *
 * @author Daniel Gómez-Sánchez
 */
public class ReadToolsFastqWriterFactory {

	private final Log logger = Log.getInstance(ReadToolsFastqWriterFactory.class);

	/**
	 * Discarded suffix for discarded output
	 */
	public static final String DISCARDED_SUFFIX = "discarded";

	/**
	 * The underlying factory
	 */
	private final FastqWriterFactory FACTORY;

	/**
	 * By default, the ouput will be GZIPPED if a prefix is provided; if a file is provided, it is not needed
	 */
	private boolean GZIP_OUTPUT = true;

	/**
	 * Should we check the existence of the file. Default value is {@link org.vetmeduni.io.IOdefault#DEFAULT_CHECK_EXISTENCE}
	 */
	private boolean CHECK_EXISTENCE = IOdefault.DEFAULT_CHECK_EXISTENCE;

	/**
	 * Initialize a new factory
	 */
	public ReadToolsFastqWriterFactory() {
		FACTORY = new FastqWriterFactory();
	}

	/**
	 * Sets whether or not to use async io (i.e. a dedicated thread per writer.
	 */
	public void setUseAsyncIo(final boolean useAsyncIo) {
		FACTORY.setUseAsyncIo(useAsyncIo);
	}

	/**
	 * If true, compute MD5 and write appropriately-named file when file is closed.
	 */
	public void setCreateMd5(final boolean createMd5) {
		FACTORY.setCreateMd5(createMd5);
	}

	/**
	 * If <code>true</code> the output generated from a prefix will be gzipped.
	 */
	public void setGzipOutput(final boolean gzip) {
		GZIP_OUTPUT = gzip;
	}

	/**
	 * If <code>true</code> the output will be checked for existence, otherwise if will be overwritten if already
	 * exists
	 */
	public void setCheckExistence(final boolean checkExistence) {
		CHECK_EXISTENCE = checkExistence;
	}

	/**
	 * Create a new default writer (using {@link htsjdk.samtools.fastq.FastqWriterFactory#newWriter(java.io.File)})
	 *
	 * @param out the file to create the writer from (it is not checked for anything)
	 *
	 * @return a new instance of the writer
	 */
	protected ReadToolsFastqWriter newWriterDefault(final File out) {
		return new ReadToolsBasicFastqWriter(FACTORY.newWriter(out));
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
		return newWriterDefault(IOUtils.newOutputFile(outputName, CHECK_EXISTENCE));
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
		final String seOutputName = IOUtils.makeOutputNameFastqWithDefaults(prefix + "_SE", GZIP_OUTPUT);
		final File seOutput = IOUtils.newOutputFile(seOutputName, CHECK_EXISTENCE);
		Lazy<FastqWriter> single = new Lazy<>(new Lazy.LazyInitializer<FastqWriter>() {

			@Override
			public FastqWriter make() {
				return FACTORY.newWriter(seOutput);
			}
		});
		return new PairFastqWriters(pair1, pair2, single);
	}

	/**
	 * Create a split writer by barcode
	 *
	 * @param prefix     the prefix for the files
	 * @param dictionary the barcode dictionary with the barcodes
	 * @param paired     <code>true</code> indicates that paired writers are used; otherwise a default is used
	 *
	 * @return a new instance of the writer
	 */
	public SplitFastqWriter newSplitByBarcodeWriter(String prefix, BarcodeDictionary dictionary, final boolean paired)
		throws IOException {
		logger.debug("Creating new SplitByBarcode barcode for ", (paired) ? "paired" : "single", "-end");
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
		// add a unknow barcode
		mapping.put(MatcherBarcodeDictionary.UNKNOWN_STRING,
			(paired) ? newPairWriter(prefix + "_" + DISCARDED_SUFFIX) : newWriter(prefix + "_" + DISCARDED_SUFFIX));
		return new SplitFastqWriterAbstract(mapping) {

			private final boolean p = paired;

			@Override
			public Hashtable<String, Object> getCurrentReport() {
				if (!p) {
					return new Hashtable<>(counts);
				} else {
					Hashtable<String, Object> report = new Hashtable<>();
					for (Map.Entry<String, ? extends FastqWriter> entry : mapping.entrySet()) {
						report.put(entry.getKey(), entry.getValue().toString());
					}
					return report;
				}
			}

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
				return (mapping.contains(barcode)) ? barcode : MatcherBarcodeDictionary.UNKNOWN_STRING;
			}
		};
	}

	/**
	 * Create a single-end split writer by barcode
	 *
	 * @param prefix     the prefix for the files
	 * @param dictionary the barcode dictionary with the barcodes
	 *
	 * @return a new instance of the writer
	 */
	public SplitFastqWriter newSplitByBarcodeWriterSingle(String prefix, BarcodeDictionary dictionary)
		throws IOException {
		return newSplitByBarcodeWriter(prefix, dictionary, false);
	}

	/**
	 * Create a pair-end split writer by barcode
	 *
	 * @param prefix     the prefix for the files
	 * @param dictionary the barcode dictionary with the barcodes
	 *
	 * @return a new instance of the writer
	 */
	public SplitFastqWriter newSplitByBarcodeWriterPaired(String prefix, BarcodeDictionary dictionary)
		throws IOException {
		return newSplitByBarcodeWriter(prefix, dictionary, true);
	}

	/**
	 * Writer that split between assign/unknow barcodes; the mapping is "assign" and {@link
	 * org.vetmeduni.methods.barcodes.dictionary.MatcherBarcodeDictionary#UNKNOWN_STRING}. By default, any record is
	 * correct unless the unknow string is provided as identifier
	 *
	 * @param prefix the prefix for the files
	 * @param paired <code>true</code> indicates that paired writers are used; otherwise a default is used
	 *
	 * @return a new instance of the writer
	 */
	public SplitFastqWriter newSplitAssingUnknownBarcodeWriter(String prefix, boolean paired) throws IOException {
		logger.debug("Creating new Assing-Unknown barcode for ", (paired) ? "paired" : "single", "-end");
		final Hashtable<String, FastqWriter> mapping = new Hashtable<>(2);
		mapping.put("assign", (paired) ? newPairWriter(prefix) : newWriter(prefix));
		mapping.put(MatcherBarcodeDictionary.UNKNOWN_STRING,
			(paired) ? newPairWriter(prefix + "_" + DISCARDED_SUFFIX) : newWriter(prefix + "_" + DISCARDED_SUFFIX));
		return new SplitFastqWriterAbstract(mapping) {

			private final boolean p = paired;

			@Override
			public Hashtable<String, Object> getCurrentReport() {
				if (!p) {
					return new Hashtable<>(counts);
				} else {
					Hashtable<String, Object> report = new Hashtable<>();
					for (Map.Entry<String, ? extends FastqWriter> entry : mapping.entrySet()) {
						report.put(entry.getKey(), entry.getValue().toString());
					}
					return report;
				}
			}

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
				if (MatcherBarcodeDictionary.UNKNOWN_STRING.equals(identifier)) {
					super.write(identifier, record);
				} else {
					write(record);
				}
			}

			@Override
			public void write(String identifier, FastqPairedRecord record) {
				if (MatcherBarcodeDictionary.UNKNOWN_STRING.equals(identifier)) {
					super.write(identifier, record);
				} else {
					write(record);
				}
			}
		};
	}

	/**
	 * Writer that split between assign/unknow barcodes for single-end
	 *
	 * @param prefix the prefix for the files
	 *
	 * @return a new instance of the writer
	 */
	public SplitFastqWriter newSplitAssingUnknownBarcodeWriterSingle(String prefix) throws IOException {
		return newSplitAssingUnknownBarcodeWriter(prefix, false);
	}

	/**
	 * Writer that split between assign/unknow barcodes for single-end
	 *
	 * @param prefix the prefix for the files
	 *
	 * @return a new instance of the writer
	 */
	public SplitFastqWriter newSplitAssingUnknownBarcodeWriterPaired(String prefix) throws IOException {
		return newSplitAssingUnknownBarcodeWriter(prefix, true);
	}
}
