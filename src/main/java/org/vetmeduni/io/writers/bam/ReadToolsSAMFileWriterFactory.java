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
package org.vetmeduni.io.writers.bam;

import htsjdk.samtools.*;
import htsjdk.samtools.util.Log;
import org.vetmeduni.io.IOdefault;
import org.vetmeduni.methods.barcodes.dictionary.BarcodeDictionary;
import org.vetmeduni.methods.barcodes.dictionary.decoder.BarcodeDecoder;
import org.vetmeduni.utils.misc.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * SAMFileWriterFactory for ReadTools, that allow the checking of the output file and generating all intermediate
 * directories
 *
 * @author Daniel Gómez-Sánchez
 */
public class ReadToolsSAMFileWriterFactory {

	private final Log logger = Log.getInstance(ReadToolsSAMFileWriterFactory.class);

	/**
	 * The underlying factory
	 */
	private final SAMFileWriterFactory FACTORY;

	public ReadToolsSAMFileWriterFactory() {
		FACTORY = new SAMFileWriterFactory();
	}

	/**
	 * Should we check the existence of the file. Default value is {@link org.vetmeduni.io.IOdefault#DEFAULT_CHECK_EXISTENCE}
	 */
	private boolean CHECK_EXISTENCE = IOdefault.DEFAULT_CHECK_EXISTENCE;

	/**
	 * Sets whether to create md5Files for BAMs from this factory.
	 */
	public ReadToolsSAMFileWriterFactory setCreateMd5File(final boolean createMd5File) {
		FACTORY.setCreateMd5File(createMd5File);
		return this;
	}

	/**
	 * Convenience method allowing newSAMFileWriterFactory().setCreateIndex(true); Equivalent to
	 * SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true); newSAMFileWriterFactory(); If a BAM (not SAM) file
	 * is created, the setting is true, and the file header specifies coordinate order, then a BAM index file will be
	 * written along with the BAM file.
	 *
	 * @param setting whether to attempt to create a BAM index while creating the BAM file.
	 *
	 * @return this factory object
	 */
	public ReadToolsSAMFileWriterFactory setCreateIndex(final boolean setting) {
		FACTORY.setCreateIndex(setting);
		return this;
	}

	/**
	 * Before creating a writer that is not presorted, this method may be called in order to override the default number
	 * of SAMRecords stored in RAM before spilling to disk (c.f. SAMFileWriterImpl.MAX_RECORDS_IN_RAM).  When writing
	 * very large sorted SAM files, you may need call this method in order to avoid running out of file handles.  The
	 * RAM available to the JVM may need to be increased in order to hold the specified number of records in RAM.  This
	 * value affects the number of records stored in subsequent calls to one of the make...() methods.
	 *
	 * @param maxRecordsInRam Number of records to store in RAM before spilling to temporary file when creating a sorted
	 *                        SAM or BAM file.
	 */
	public ReadToolsSAMFileWriterFactory setMaxRecordsInRam(final int maxRecordsInRam) {
		FACTORY.setMaxRecordsInRam(maxRecordsInRam);
		return this;
	}

	/**
	 * Turn on or off the use of asynchronous IO for writing output SAM and BAM files.  If true then each SAMFileWriter
	 * creates a dedicated thread which is used for compression and IO activities.
	 */
	public ReadToolsSAMFileWriterFactory setUseAsyncIo(final boolean useAsyncIo) {
		FACTORY.setUseAsyncIo(useAsyncIo);
		return this;
	}

	/**
	 * If and only if using asynchronous IO then sets the maximum number of records that can be buffered per
	 * SAMFileWriter before producers will block when trying to write another SAMRecord.
	 */
	public ReadToolsSAMFileWriterFactory setAsyncOutputBufferSize(final int asyncOutputBufferSize) {
		FACTORY.setAsyncOutputBufferSize(asyncOutputBufferSize);
		return this;
	}

	/**
	 * Controls size of write buffer. Default value: [[htsjdk.samtools.Defaults#BUFFER_SIZE]]
	 */
	public ReadToolsSAMFileWriterFactory setBufferSize(final int bufferSize) {
		FACTORY.setBufferSize(bufferSize);
		return this;
	}

	/**
	 * Set the temporary directory to use when sort data.
	 *
	 * @param tmpDir Path to the temporary directory
	 */
	public ReadToolsSAMFileWriterFactory setTempDirectory(final File tmpDir) {
		FACTORY.setTempDirectory(tmpDir);
		return this;
	}

	/**
	 * Create a BAMFileWriter that is ready to receive SAMRecords.  Uses default compression level.
	 *
	 * @param header     entire header. Sort order is determined by the sortOrder property of this arg.
	 * @param presorted  if true, SAMRecords must be added to the SAMFileWriter in order that agrees with
	 *                   header.sortOrder.
	 * @param outputFile where to write the output.
	 */
	public SAMFileWriter makeBAMWriter(final SAMFileHeader header, final boolean presorted, final File outputFile)
		throws IOException {
		checkExistenceAndCreateDirs(outputFile);
		return FACTORY.makeBAMWriter(header, presorted, outputFile);
	}

	/**
	 * Create a BAMFileWriter that is ready to receive SAMRecords.
	 *
	 * @param header           entire header. Sort order is determined by the sortOrder property of this arg.
	 * @param presorted        if true, SAMRecords must be added to the SAMFileWriter in order that agrees with
	 *                         header.sortOrder.
	 * @param outputFile       where to write the output.
	 * @param compressionLevel Override default compression level with the given value, between 0 (fastest) and 9
	 *                         (smallest).
	 */
	public SAMFileWriter makeBAMWriter(final SAMFileHeader header, final boolean presorted, final File outputFile,
		final int compressionLevel) throws IOException {
		checkExistenceAndCreateDirs(outputFile);
		return FACTORY.makeBAMWriter(header, presorted, outputFile, compressionLevel);
	}

	/**
	 * Create a SAMTextWriter that is ready to receive SAMRecords.
	 *
	 * @param header     entire header. Sort order is determined by the sortOrder property of this arg.
	 * @param presorted  if true, SAMRecords must be added to the SAMFileWriter in order that agrees with
	 *                   header.sortOrder.
	 * @param outputFile where to write the output.
	 */
	public SAMFileWriter makeSAMWriter(final SAMFileHeader header, final boolean presorted, final File outputFile)
		throws IOException {
		checkExistenceAndCreateDirs(outputFile);
		return FACTORY.makeSAMWriter(header, presorted, outputFile);
	}

	/**
	 * Create a SAMTextWriter for writing to a stream that is ready to receive SAMRecords. This method does not support
	 * the creation of an MD5 file
	 *
	 * @param header    entire header. Sort order is determined by the sortOrder property of this arg.
	 * @param presorted if true, SAMRecords must be added to the SAMFileWriter in order that agrees with
	 *                  header.sortOrder.
	 * @param stream    the stream to write records to.  Note that this method does not buffer the stream, so the caller
	 *                  must buffer if desired.  Note that PrintStream is buffered.
	 */
	public SAMFileWriter makeSAMWriter(final SAMFileHeader header, final boolean presorted, final OutputStream stream) {
		return FACTORY.makeSAMWriter(header, presorted, stream);
	}

	/**
	 * Create a BAMFileWriter for writing to a stream that is ready to receive SAMRecords. This method does not support
	 * the creation of an MD5 file
	 *
	 * @param header    entire header. Sort order is determined by the sortOrder property of this arg.
	 * @param presorted if true, SAMRecords must be added to the SAMFileWriter in order that agrees with
	 *                  header.sortOrder.
	 * @param stream    the stream to write records to.  Note that this method does not buffer the stream, so the caller
	 *                  must buffer if desired.  Note that PrintStream is buffered.
	 */
	public SAMFileWriter makeBAMWriter(final SAMFileHeader header, final boolean presorted, final OutputStream stream) {
		return FACTORY.makeBAMWriter(header, presorted, stream);
	}

	/**
	 * Create either a SAM or a BAM writer based on examination of the outputFile extension.
	 *
	 * @param header     entire header. Sort order is determined by the sortOrder property of this arg.
	 * @param presorted  presorted if true, SAMRecords must be added to the SAMFileWriter in order that agrees with
	 *                   header.sortOrder.
	 * @param outputFile where to write the output.  Must end with .sam or .bam.
	 *
	 * @return SAM or BAM writer based on file extension of outputFile.
	 */
	public SAMFileWriter makeSAMOrBAMWriter(final SAMFileHeader header, final boolean presorted, final File outputFile)
		throws IOException {
		checkExistenceAndCreateDirs(outputFile);
		return FACTORY.makeSAMOrBAMWriter(header, presorted, outputFile);
	}

	public SAMFileWriter makeWriter(final SAMFileHeader header, final boolean presorted, final File outputFile,
		final File referenceFasta) throws IOException {
		checkExistenceAndCreateDirs(outputFile);
		return FACTORY.makeWriter(header, presorted, outputFile, referenceFasta);
	}

	public CRAMFileWriter makeCRAMWriter(final SAMFileHeader header, final OutputStream stream,
		final File referenceFasta) {
		return FACTORY.makeCRAMWriter(header, stream, referenceFasta);
	}

	public CRAMFileWriter makeCRAMWriter(final SAMFileHeader header, final File outputFile, final File referenceFasta)
		throws IOException {
		checkExistenceAndCreateDirs(outputFile);
		return FACTORY.makeCRAMWriter(header, outputFile, referenceFasta);
	}

	/**
	 * Create a split writer by barcode; the default behaviour for add alignment is use the sample tag (SM) in the read
	 * group
	 *
	 * @param header     entire header. Sort order is determined by the sortOrder property of this arg.
	 * @param filePrefix the prefix for the files
	 * @param bam        if <code>true</code> the writers will be bam, if <code>false</code> they will be sam
	 * @param dictionary the barcode dictionary with the barcodes
	 *
	 * @return a new instance of the writer
	 */
	public SplitSAMFileWriter makeSplitWriterByBarcode(final SAMFileHeader header, final String filePrefix, boolean bam,
		BarcodeDictionary dictionary) throws IOException {
		logger.debug("Creating a split by barcode SAM writer");
		final String extension = (bam) ? BamFileIoUtils.BAM_FILE_EXTENSION : IOUtils.DEFAULT_SAM_EXTENSION;
		Hashtable<String, SAMFileWriter> mapping = new Hashtable<>();
		HashMap<String, SAMFileWriter> sampleNames = new HashMap<>();
		for (int i = 0; i < dictionary.numberOfSamples(); i++) {
			String sample = dictionary.getSampleNames().get(i);
			if (!sampleNames.containsKey(sample)) {
				SAMFileWriter sampleWriter = this
					.makeSAMOrBAMWriter(header, header.getSortOrder().equals(SAMFileHeader.SortOrder.coordinate),
						new File(String.format("%s_%s%s", filePrefix, sample, extension)));
				sampleNames.put(sample, sampleWriter);
			}
			mapping.put(dictionary.getCombinedBarcodesFor(i), sampleNames.get(sample));
		}
		// add a unknow barcode
		mapping.put(BarcodeDecoder.UNKNOWN_STRING,
			this.makeSAMOrBAMWriter(header, header.getSortOrder().equals(SAMFileHeader.SortOrder.coordinate),
				new File(String.format("%s_%s%s", filePrefix, IOdefault.DISCARDED_SUFFIX, extension))));
		return new SplitSAMFileWriterAbstract(header, mapping) {

			@Override
			public void addAlignment(SAMRecord alignment) {
				final String sampleName = alignment.getReadGroup().getSample();
				addAlignment(sampleName, alignment);
			}
		};
	}

	/**
	 * Check the existence of the file if the factory should do it and generate all the intermediate directories
	 *
	 * @param outputFile
	 */
	private void checkExistenceAndCreateDirs(File outputFile) throws IOException {
		if (CHECK_EXISTENCE) {
			IOUtils.exceptionIfExists(outputFile);
		}
		IOUtils.createDirectoriesForOutput(outputFile);
	}
}
