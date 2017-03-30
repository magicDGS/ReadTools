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

import org.magicdgs.readtools.utils.fastq.FastqReadNameEncoding;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.fastq.FastqRecord;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.SAMRecordToGATKReadAdapter;

/**
 * Simple GATKRead implementation from a FastqRecord.
 *
 * Note: on creation, FasteGATKRead are considered unmapped.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqGATKRead extends SAMRecordToGATKReadAdapter {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a headerless GATKRead from a FastqRecord.
     *
     * @param record the record to use as GATKRead.
     */
    public FastqGATKRead(final FastqRecord record) {
        this(null, record);
    }

    /**
     * Creates a GATKRead from a FastqRecord and a header.
     *
     * @param header the header for the record.
     * @param record the record to use as GATKRead.
     */
    public FastqGATKRead(final SAMFileHeader header, final FastqRecord record) {
        super(new SAMRecord(header));
        Utils.nonNull(record, "null record");
        // update the record with the read name information
        FastqReadNameEncoding.updateReadFromReadName(this, record.getReadName());
        // set the bases and the qualities
        this.setBases(record.getReadBases());
        this.setBaseQualities(record.getBaseQualities());
        // add the comments in the quality header to the comment if present
        final String baseQualHeader = record.getBaseQualityHeader();
        if (baseQualHeader != null) {
            // the default tag in the specs is CO
            this.setAttribute(SAMTag.CO.toString(), baseQualHeader);
        }
        this.setIsUnmapped();
        if (this.isPaired()) {
            this.setMateIsUnmapped();
        }
    }
}
