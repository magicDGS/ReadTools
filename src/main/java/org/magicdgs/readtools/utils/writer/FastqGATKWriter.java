/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.utils.writer;

import htsjdk.samtools.BAMRecordCodec;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordComparator;
import htsjdk.samtools.SAMRecordCoordinateComparator;
import htsjdk.samtools.SAMRecordDuplicateComparator;
import htsjdk.samtools.SAMRecordQueryNameComparator;
import htsjdk.samtools.SAMSortOrderChecker;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.SortingCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Writer for GATKRead to output a FASTQ file.
 *
 * Note: does not allow yet to sort.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqGATKWriter implements GATKReadWriter {

    private static final Logger logger = LogManager.getLogger(FastqGATKWriter.class);

    // TODO: this will be in HTSJDK FastqConstants after https://github.com/samtools/htsjdk/pull/572
    public static final String FIRST_OF_PAIR = "/1";
    public static final String SECOND_OF_PAIR = "/2";

    // wrapper
    private final FastqWriter writer;
    private boolean isClosed = false;
    // the sort order for the writer
    // TODO: allow sorting by queryname (I don't like the idea to allow sorting by other)
    // TODO: although sorting by mapping position could be allowed (read could have been mapped)
    // TODO: it will separate the read-pairs in an interleaved file
    private final SAMFileHeader.SortOrder sortOrder = SAMFileHeader.SortOrder.unsorted;
    // are reads expected to be pre-sorted
    // TODO: this is for sorting
    private final boolean presorted = false;
    private final SAMSortOrderChecker sortOrderChecker;
    private final SAMFileHeader header;
    private final SortingCollection<SAMRecord> alignmentSorter;

    /**
     * Constructor with a factory and sort order.
     *
     * Note: sorting is not implemented yet.
     *
     * @param path      the path to write the file in.
     * @param factory   the factory to construct the writer.
     * @param sortOrder the order of the output file.
     * @param header    the header for use in sorting. May be null.
     */
    public FastqGATKWriter(final Path path, final FastqWriterFactory factory,
            final SAMFileHeader.SortOrder sortOrder, final SAMFileHeader header) {
        this.writer = factory.newWriter(path.toFile());
        this.header = header;
        // TODO: this should be set instead
        if (sortOrder != SAMFileHeader.SortOrder.unsorted) {
            // TODO: allow sorting by queryname (I don't like the idea to allow sorting by other)
            // TODO: although sorting by mapping position could be allowed (read could have been mapped)
            // TODO: it will separate the read-pairs in an interleaved file
            logger.warn("FASTQ file cannot be sorted in {} order. This will change in the future",
                    sortOrder);
        }
        // copied from SAMFileWriterImpl
        alignmentSorter = SortingCollection.newInstance(SAMRecord.class,
                new BAMRecordCodec(header), makeComparator(),
                // TODO: this two values should be settable
                500000, new File(System.getProperty("java.io.tmpdir")));
        sortOrderChecker = new SAMSortOrderChecker(this.sortOrder);
    }

    // copied from SAMFileWriterImpl
    private SAMRecordComparator makeComparator() {
        switch (sortOrder) {
            case coordinate:
                return new SAMRecordCoordinateComparator();
            case queryname:
                return new SAMRecordQueryNameComparator();
            case duplicate:
                return new SAMRecordDuplicateComparator();
            case unsorted:
                return null;
        }
        throw new IllegalStateException("sortOrder should not be null");
    }

    /**
     * Constructor with default factory and unsorted order.
     *
     * @param path the path to write the file in.
     */
    public FastqGATKWriter(final Path path) {
        this(path, new FastqWriterFactory(), SAMFileHeader.SortOrder.unsorted, null);
    }

    @Override
    public void addRead(final GATKRead read) {
        if (sortOrder.equals(SAMFileHeader.SortOrder.unsorted)) {
            write(read);
        } else if (presorted) {
            assertPresorted(read);
            write(read);
        } else {
            alignmentSorter.add(read.convertToSAMRecord(header));
        }
    }

    private void assertPresorted(final GATKRead read) {
        // TODO: this is too dirty
        final SAMRecord prev = sortOrderChecker.getPreviousRecord();
        final SAMRecord next = read.convertToSAMRecord(header);
        if (!sortOrderChecker.isSorted(next)) {
            throw new IllegalArgumentException(
                    "Alignments added out of order in . Sort order is " + this.sortOrder
                            + ". Offending records are at ["
                            + sortOrderChecker.getSortKey(prev) + "] and ["
                            + sortOrderChecker.getSortKey(next) + "]");
        }
    }

    private void write(final GATKRead read) {
        final FastqRecord fastq = new FastqRecord(
                (read.isPaired()) ? getPairedName(read) : read.getName(),
                read.getBasesString(),
                read.getAttributeAsString(SAMTag.CO.name()),
                ReadUtils.getBaseQualityString(read)
        );
        writer.write(fastq);
    }

    private void write(final SAMRecord read) {
        final FastqRecord fastq = new FastqRecord(
                (read.getReadPairedFlag()) ? getPairedName(read) : read.getReadName(),
                read.getReadString(),
                read.getStringAttribute(SAMTag.CO.name()),
                read.getBaseQualityString()
        );
        writer.write(fastq);
    }

    private String getPairedName(SAMRecord read) {
        if (read.getFirstOfPairFlag()) {
            return read.getReadName() + FIRST_OF_PAIR;
        } else if (read.getSecondOfPairFlag()) {
            return read.getReadName() + SECOND_OF_PAIR;
        } else {
            throw new GATKException("Found read with incorrect pair flag: " + read);
        }
    }

    private String getPairedName(final GATKRead read) {
        if (read.isFirstOfPair()) {
            return read.getName() + FIRST_OF_PAIR;
        } else if (read.isSecondOfPair()) {
            return read.getName() + SECOND_OF_PAIR;
        } else {
            throw new GATKException("Found read with incorrect pair flag: " + read);
        }
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            if (alignmentSorter != null) {
                for (final SAMRecord record : alignmentSorter) {
                    write(record);
                }
                alignmentSorter.cleanup();
            }
            writer.close();
        }
        isClosed = true;
    }

}
