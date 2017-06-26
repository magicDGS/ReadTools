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

package org.magicdgs.readtools.utils.fastq;

import org.magicdgs.readtools.utils.read.RTReadUtils;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMFileWriterImpl;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.fastq.FastqConstants;
import htsjdk.samtools.fastq.FastqEncoder;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.utils.Utils;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Writer for {@link GATKRead} to output a FASTQ file (Illumina formatted). It includes the
 * following information:
 *
 * <ul>
 * <li>If present, barcode in the read name</li>
 * <li>If pair-end, marker for first/second of pair</li>
 * <li>Comment line if the 'CO' tag is set</li>
 * </ul>
 *
 * <p>WARNING: using the {@link #addAlignment(SAMRecord)} method does not set the read name in the
 * Illumina formatting.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqGATKWriter extends SAMFileWriterImpl implements GATKReadWriter {

    private static final Logger logger = LogManager.getLogger(FastqGATKWriter.class);

    private final String fileName;
    private final Writer writer;

    /** Constructor from an underlying writer coming from the provided file. */
    public FastqGATKWriter(final Writer writer, final String fileName) {
        this.writer = Utils.nonNull(writer, "null writer");
        this.fileName = fileName;
    }

    /** Constructor from an underlying writer. */
    public FastqGATKWriter(final Writer writer) {
        this(writer, null);
    }

    public void setMaxRecordsInRam(final int maxRecordsInRam) {
        // TODO: requires a patch in HTSJDK to be able to call the super method
        logger.warn(
                "FASTQ writer does not honor the maximum records in RAM parameter ({}). This may be solved in the near future.",
                maxRecordsInRam);
    }


    public void setTempDirectory(final File tmpDir) {
        // TODO: requires a patch in HTSJDK to be able to call the super method
        logger.warn(
                "FASTQ writer does not honor the temp directory parameter ({}). This is a known limitation that will be solved in the future.",
                tmpDir);
    }

    /**
     * Adds the read to the writer after applying the Illumina formatting to the read name.
     *
     * <p>WARNING: it is not guaranteed that the read name is maintain as it is.
     * If you want that the writing does not affect the read name, use {@link GATKRead#deepCopy()}
     * before adding to the writer.
     */
    @Override
    public void addRead(final GATKRead read) {
        // adding the raw barcode information if found
        String readName = RTReadUtils.getReadNameWithIlluminaBarcode(read);
        // adding the pair information
        if (read.isPaired()) {
            readName += (read.isFirstOfPair())
                    ? FastqConstants.FIRST_OF_PAIR : FastqConstants.SECOND_OF_PAIR;
        }
        // convert to a SAM record and set the read name
        final SAMRecord record = read.convertToSAMRecord(getFileHeader());
        record.setReadName(readName);
        // add the alignment
        addAlignment(record);
    }

    /** Closes the underlying writer. */
    @Override
    protected void finish() {
        try {
            writer.close();
        } catch (final IOException e) {
            throw new UserException.CouldNotCreateOutputFile(fileName, e.getMessage(), e);
        }
    }

    /** Converts the alignment into a {@link FastqRecord} and encode it in the underlying writer. */
    @Override
    protected void writeAlignment(SAMRecord alignment) {
        try {
            // TODO: use asFastqRecord, but requires a patch in HTSJDK to include the CO tag
            // final FastqRecord record = FastqEncoder.asFastqRecord(alignment);
            final FastqRecord record = new FastqRecord(
                    alignment.getReadName(),
                    alignment.getReadString(),
                    alignment.getStringAttribute(SAMTag.CO.name()),
                    alignment.getBaseQualityString()
            );
            FastqEncoder.write(writer, record);
            writer.append('\n');
        } catch (final IOException | SAMException e) {
            throw new UserException.CouldNotCreateOutputFile(fileName, e.getMessage(), e);
        }
    }

    /** Does not write the header in the output. */
    @Override
    protected void writeHeader(final String textHeader) {
        // TODO: submit a patch in HTSJDK to remove this method in favor of a method using the SAMHeader directly or a supplier
        // TODO: in the case of FASTQ files, this is not needed at all, but the header string is encoded anyway
        // TODO: which may be expensive
        // no-op
    }

    @Override
    protected String getFilename() {
        return fileName;
    }
}