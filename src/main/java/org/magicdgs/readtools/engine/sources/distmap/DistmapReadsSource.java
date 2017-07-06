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

package org.magicdgs.readtools.engine.sources.distmap;

import org.magicdgs.readtools.engine.sources.RTAbstractReadsSource;
import org.magicdgs.readtools.engine.sources.RTReadsSource;
import org.magicdgs.readtools.utils.distmap.DistmapEncoder;
import org.magicdgs.readtools.utils.iterators.RecordToReadIterator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.QualityEncodingDetector;
import htsjdk.tribble.readers.AsciiLineReader;
import htsjdk.tribble.readers.AsciiLineReaderIterator;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DistmapReadsSource extends RTAbstractReadsSource {

    private final String source;

    private AsciiLineReaderIterator iterator = null;

    public DistmapReadsSource(final String source) {
        this.source = source;
    }

    @Override
    protected FastqQualityFormat detectEncoding(long maxNumberOfReads) {
        // open a raw iterator
        final Iterator<GATKRead> iterator = rawIterator();
        final SAMFileHeader header = getHeader();

        final CloseableIterator<SAMRecord> forQuality =
                new CloseableIterator<SAMRecord>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public SAMRecord next() {
                        return iterator.next().convertToSAMRecord(header);
                    }

                    @Override
                    public void close() {
                        // closing the opened raw iterator
                        this.close();
                    }
                };
        // this should close the iterator
        return QualityEncodingDetector.detect(maxNumberOfReads, forQuality);
    }

    private AsciiLineReaderIterator openReader() {
        // TODO: this should open also bgzip, but IOUtil is not doing it
        return new AsciiLineReaderIterator(new AsciiLineReader(
                IOUtil.openFileForReading(IOUtils.getPath(source))));
    }

    @Override
    protected Iterator<GATKRead> rawIterator() {
        if (iterator == null) {
            iterator = openReader();
        }
        if (isPaired()) {
            // TODO: this is not supported yet!!
            return new GATKReadTupleIterator(getPairedIterator());
        }
        return new RecordToReadIterator<>(iterator, DistmapEncoder::decodeSingle);
    }

    @Override
    public Iterator<Tuple2<GATKRead, GATKRead>> getPairedIterator() {
        if (!isPaired()) {
            throw new UnsupportedOperationException(getSourceDescription() + ": cannot retrieve pair-end");
        }
        if (iterator == null) {
            iterator = openReader();
        }
        // TODO: this should return the correct encoding!!
        return Utils.stream(iterator).map(DistmapEncoder::decodePaired).iterator();
    }

    @Override
    public SAMFileHeader getHeader() {
        final SAMFileHeader header = new SAMFileHeader();
        header.setSortOrder(SAMFileHeader.SortOrder.unknown);
        return header;
    }

    @Override
    public boolean isPaired() {
        try (final AsciiLineReaderIterator iterator = openReader()) {
            return DistmapEncoder.isPaired(iterator.next());
        } catch (IOException e) {
            // TODO: better exception msg
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public Iterator<GATKRead> query(List<SimpleInterval> locs) {
        // TODO: better msg
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceDescription() {
        return String.format("Distmap %s-end (%s)", (isPaired()) ? "pair" : "single", source);
    }

    @Override
    public RTReadsSource setValidationStringency(ValidationStringency stringency) {
        return null;
    }

    @Override
    public RTReadsSource setUseAsyncIo(boolean useAsyncIo) {
        return null;
    }

    @Override
    public RTReadsSource setReferenceSequence(File referenceFile) {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    private final static class GATKReadTupleIterator implements Iterator<GATKRead> {

        private final Iterator<Tuple2<GATKRead, GATKRead>> tupleIterator;
        private List<GATKRead> reads = new ArrayList<>(2);

        private GATKReadTupleIterator(Iterator<Tuple2<GATKRead, GATKRead>> tupleIterator) {
            this.tupleIterator = tupleIterator;
            readNext();
        }

        @Override
        public boolean hasNext() {
            return (!reads.isEmpty() || tupleIterator.hasNext());
        }

        @Override
        public GATKRead next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Exhausted iterator");
            }
            if (reads.isEmpty()) {
                readNext();
            }
            return reads.remove(0);
        }

        private void readNext() {
            final Tuple2<GATKRead, GATKRead> pair = this.tupleIterator.next();
            reads.add(pair._1);
            reads.add(pair._2);
        }
    }

}
