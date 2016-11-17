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
 * SOFTWARE.
 */
package org.magicdgs.io.writers.bam;

import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionaryFactory;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.utils.misc.IOUtils;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;

import htsjdk.samtools.BamFileIoUtils;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * SAMFileWriterFactory for ReadTools, that allow the checking of the output file and generating
 * all intermediate directories
 *
 * @author Daniel Gómez-Sánchez
 * @deprecated for create writers, use {@link ReadWriterFactory}.
 */
@Deprecated
public class ReadToolsSAMFileWriterFactory {

    private final Logger logger = LogManager.getLogger(ReadToolsSAMFileWriterFactory.class);

    /**
     * The underlying factory
     */
    private final ReadWriterFactory FACTORY;

    public ReadToolsSAMFileWriterFactory() {
        FACTORY = new ReadWriterFactory();
    }

    /**
     * If {@code true} the output will be overwritten, otherwise it will check for the existence of
     * the output.
     */
    public ReadToolsSAMFileWriterFactory setForceOverwrite(final boolean forceOverwrite) {
        FACTORY.setForceOverwrite(forceOverwrite);
        return this;
    }

    /**
     * Sets whether to create md5Files for BAMs from this factory.
     */
    public ReadToolsSAMFileWriterFactory setCreateMd5File(final boolean createMd5File) {
        FACTORY.setCreateMd5File(createMd5File);
        return this;
    }

    /**
     * Convenience method allowing newSAMFileWriterFactory().setCreateIndex(true); Equivalent to
     * SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true); newSAMFileWriterFactory(); If
     * a
     * BAM (not SAM) file
     * is created, the setting is true, and the file header specifies coordinate order, then a BAM
     * index file will be
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
     * Before creating a writer that is not presorted, this method may be called in order to
     * override the default number
     * of SAMRecords stored in RAM before spilling to disk (c.f. SAMFileWriterImpl.MAX_RECORDS_IN_RAM).
     * When writing
     * very large sorted SAM files, you may need call this method in order to avoid running out of
     * file handles.  The
     * RAM available to the JVM may need to be increased in order to hold the specified number of
     * records in RAM.  This
     * value affects the number of records stored in subsequent calls to one of the make...()
     * methods.
     *
     * @param maxRecordsInRam Number of records to store in RAM before spilling to temporary file
     *                        when creating a sorted
     *                        SAM or BAM file.
     */
    public ReadToolsSAMFileWriterFactory setMaxRecordsInRam(final int maxRecordsInRam) {
        FACTORY.setMaxRecordsInRam(maxRecordsInRam);
        return this;
    }

    /**
     * Turn on or off the use of asynchronous IO for writing output SAM and BAM files.  If true
     * then
     * each SAMFileWriter
     * creates a dedicated thread which is used for compression and IO activities.
     */
    public ReadToolsSAMFileWriterFactory setUseAsyncIo(final boolean useAsyncIo) {
        FACTORY.setUseAsyncIo(useAsyncIo);
        return this;
    }

    /**
     * If and only if using asynchronous IO then sets the maximum number of records that can be
     * buffered per
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
     * Set the reference file for output CRAM.
     *
     * @param referenceFile the reference file
     */
    public ReadToolsSAMFileWriterFactory setReferenceFile(final File referenceFile) {
        FACTORY.setReferenceFile(referenceFile);
        return this;
    }

    /**
     * Create either a SAM/BAM/CRAM writer based on examination of the outputFile extension.
     *
     * @param header     entire header. Sort order is determined by the sortOrder property of this
     *                   arg.
     * @param presorted  presorted if true, SAMRecords must be added to the SAMFileWriter in order
     *                   that agrees with header.sortOrder.
     * @param outputFile where to write the output.  Must end with .sam or .bam.
     *
     * @return SAM/BAM/CRAM writer based on file extension of outputFile.
     */
    public SAMFileWriter openSAMWriter(final SAMFileHeader header, final boolean presorted,
            final String outputFile) {
        return FACTORY.openSAMWriter(header, presorted, outputFile);
    }

    /**
     * Create a split writer by barcode; the default behaviour for add alignment is use the id tag
     * (SM) in the read
     * group
     *
     * @param header     entire header. Sort order is determined by the sortOrder property of this
     *                   arg.
     * @param filePrefix the prefix for the files
     * @param bam        if <code>true</code> the writers will be bam, if <code>false</code> they
     *                   will be sam
     * @param dictionary the barcode dictionary with the barcodes
     *
     * @return a new instance of the writer
     */
    public SplitSAMFileWriter makeSplitByBarcodeWriter(final SAMFileHeader header,
            final String filePrefix, boolean bam,
            BarcodeDictionary dictionary) throws IOException {
        logger.debug("Creating a split by barcode SAM writer");
        final String extension =
                (bam) ? BamFileIoUtils.BAM_FILE_EXTENSION : IOUtils.DEFAULT_SAM_EXTENSION;
        Hashtable<String, SAMFileWriter> mapping = new Hashtable<>();
        // HashMap<String, SAMFileWriter> sampleNames = new HashMap<>();
        HashMap<String, ArrayList<SAMReadGroupRecord>> readGroups = new HashMap<>();
        // first create the samples
        for (int i = 0; i < dictionary.numberOfSamples(); i++) {
            final SAMReadGroupRecord sampleRG = dictionary.getReadGroupFor(i);
            final String sample = sampleRG.getSample();
            if (!readGroups.containsKey(sample)) {
                readGroups.put(sample, new ArrayList<>());
            }
            readGroups.get(sample).add(sampleRG);
        }
        // create the mapping
        for (Map.Entry<String, ArrayList<SAMReadGroupRecord>> entry : readGroups.entrySet()) {
            mapping.put(entry.getKey(),
                    this.openSAMWriter(getReadHeaderFor(header, entry.getValue()), true,
                            String.format("%s_%s%s", filePrefix, entry.getKey(), extension)));
        }
        // add a unknown barcode
        mapping.put(BarcodeMatch.UNKNOWN_STRING,
                this.openSAMWriter(getDiscardedFileHeader(header), true,
                        String.format("%s_%s%s", filePrefix, IOUtils.DISCARDED_SUFFIX, extension)));
        return new SplitSAMFileWriterAbstract(header, mapping) {

            @Override
            public void addAlignment(SAMRecord alignment) {
                try {
                    final String sampleName = alignment.getReadGroup().getSample();
                    addAlignment(sampleName, alignment);
                } catch (NullPointerException e) {
                    addAlignment(BarcodeMatch.UNKNOWN_STRING, alignment);
                }
            }
        };
    }

    /**
     * Create a split writer between assign/unknown barcodes; the mapping is "assign" and {@link
     * org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch#UNKNOWN_STRING}. The
     * add
     * alignment checks for the
     * read group ID; if it is not found (<code>null</code> value for read group) or {@link
     * org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch#UNKNOWN_STRING}, it
     * goes to the
     * unknown file,
     *
     * @param header     entire header. Sort order is determined by the sortOrder property of this
     *                   arg.
     * @param filePrefix the prefix for the files
     * @param bam        if <code>true</code> the writers will be bam, if <code>false</code> they
     *                   will be sam
     *
     * @return a new instance of the writer
     */
    public SplitSAMFileWriter makeSplitAssignUnknownBarcodeWriter(final SAMFileHeader header,
            final String filePrefix,
            boolean bam) throws IOException {
        logger.debug("Creating a split by barcode SAM writer");
        final String extension =
                (bam) ? BamFileIoUtils.BAM_FILE_EXTENSION : IOUtils.DEFAULT_SAM_EXTENSION;
        Hashtable<String, SAMFileWriter> mapping = new Hashtable<>(2);
        // add the assign barcode
        mapping.put("assign", this.openSAMWriter(header, true, filePrefix + extension));
        // add a unknown barcode
        mapping.put(BarcodeMatch.UNKNOWN_STRING,
                this.openSAMWriter(getDiscardedFileHeader(header), true,
                        String.format("%s_%s%s", filePrefix, IOUtils.DISCARDED_SUFFIX, extension)));
        return new SplitSAMFileWriterAbstract(header, mapping) {

            @Override
            public void addAlignment(SAMRecord alignment) {
                String assignedId;
                try {
                    assignedId = alignment.getReadGroup().getId();
                } catch (NullPointerException e) {
                    assignedId = BarcodeMatch.UNKNOWN_STRING;
                }
                if (assignedId == null) {
                    assignedId = BarcodeMatch.UNKNOWN_STRING;
                } else if (!assignedId.equals(BarcodeMatch.UNKNOWN_STRING)) {
                    assignedId = "assign";
                }
                addAlignment(assignedId, alignment);
            }
        };
    }

    /**
     * Clone the original header and add as the only one read group
     *
     * @param original      the original header
     * @param readGroupInfo the requested read group
     *
     * @return the header for the sample file
     */
    private static SAMFileHeader getReadHeaderFor(SAMFileHeader original,
            ArrayList<SAMReadGroupRecord> readGroupInfo) {
        SAMFileHeader toReturn = original.clone();
        toReturn.setReadGroups(readGroupInfo);
        return toReturn;
    }

    /**
     * Clone the original header and add as the only one read group for the unknown
     *
     * @param original the original header
     *
     * @return the header for the discarded file
     */
    private static SAMFileHeader getDiscardedFileHeader(SAMFileHeader original) {
        return getReadHeaderFor(original, new ArrayList<SAMReadGroupRecord>() {{
            add(BarcodeDictionaryFactory.UNKNOWN_READGROUP_INFO);
        }});
    }
}
