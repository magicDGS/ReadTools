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

package org.magicdgs.readtools.utils.read.writer;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.fastq.RTFastqContstants;
import org.magicdgs.readtools.utils.read.RTReadUtils;

import htsjdk.samtools.SAMTag;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.io.IOException;

/**
 * Basic writer for GATKRead to output a FASTQ file.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqGATKWriter implements GATKReadWriter {

    // TODO: this is just a wrapper for now, but I would like to have more control
    // TODO: to allow using Path and set buffer size and other parameters not available
    // TODO: in the HTSJDK implementation
    private final FastqWriter writer;

    /** Constructor from a wrapped writer. */
    public FastqGATKWriter(final FastqWriter writer) {
        this.writer = writer;
    }

    @Override
    public void addRead(final GATKRead read) {
        final StringBuilder readName = new StringBuilder(read.getName());
        // adding the raw barcode information if found
        final String[] barcodes = RTReadUtils.getRawBarcodes(read);
        if (barcodes.length != 0) {
            readName.append(RTFastqContstants.ILLUMINA_NAME_BARCODE_DELIMITER)
                    .append(String.join(RTDefaults.BARCODE_INDEX_DELIMITER, barcodes));
        }
        // adding the pair information
        if (read.isPaired()) {
            readName.append((read.isFirstOfPair())
                    ? RTFastqContstants.FIRST_OF_PAIR : RTFastqContstants.SECOND_OF_PAIR);
        }
        writer.write(new FastqRecord(readName.toString(),
                read.getBasesString(),
                read.getAttributeAsString(SAMTag.CO.name()),
                ReadUtils.getBaseQualityString(read)));
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}