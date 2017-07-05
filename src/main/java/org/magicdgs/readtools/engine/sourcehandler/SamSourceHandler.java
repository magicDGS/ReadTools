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

package org.magicdgs.readtools.engine.sourcehandler;

import org.magicdgs.readtools.utils.read.ReadReaderFactory;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.iterators.SAMRecordToReadIterator;
import org.broadinstitute.hellbender.utils.iterators.SamReaderQueryingIterator;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Iterator;
import java.util.List;

/**
 * Handler for SAM/BAM/CRAM files, which uses the default factory provided.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @deprecated use {@link org.magicdgs.readtools.engine.sources.sam.SamReadsSource}.
 */
@Deprecated
public final class SamSourceHandler extends FileSourceHandler<SamReader> {

    /**
     * Constructor using default factory.
     *
     * @param source the source of reads (SAM/BAM/CRAM).
     */
    public SamSourceHandler(final String source) {
        this(source, new ReadReaderFactory());
    }

    /**
     * Constructor using the provided factory.
     *
     * @param source  the source of reads (SAM/BAM/CRAM).
     * @param factory the factory to create the readers from.
     */
    public SamSourceHandler(final String source, final ReadReaderFactory factory) {
        super(source, factory);
    }

    /** Gets the interval iterator from a fresh reader, including unmapped reads. */
    @Override
    protected Iterator<GATKRead> getReaderIntervalIterator(final SamReader reader,
            final List<SimpleInterval> locs) {
        try {
            return new SAMRecordToReadIterator(new SamReaderQueryingIterator(reader, locs, false));
        } catch (SAMException | UserException e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        }
    }

    protected SamReader getFreshReader() {
        return factory.openSamReader(path);
    }

    @Override
    protected SAMFileHeader getReaderHeader(final SamReader reader) {
        return reader.getFileHeader();
    }

    @Override
    protected FastqQualityFormat getReaderQualityEncoding(final SamReader reader,
            final long maxNumberOfReads) {
        return QualityEncodingDetector.detect(maxNumberOfReads, reader);
    }

    @Override
    protected Iterator<GATKRead> getReaderIterator(final SamReader reader) {
        return new SAMRecordToReadIterator(reader.iterator());
    }

}
