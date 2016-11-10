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

package org.magicdgs.readtools.engine.sourcehandler;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;
import org.broadinstitute.hellbender.utils.iterators.SAMRecordToReadIterator;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.IOException;
import java.util.Iterator;

/**
 * Handler for SAM/BAM/CRAM files, which uses the default factory provided.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class SamSourceHandler extends FileSourceHandler<SamReader> {

    // factory used for create the readers
    private final SamReaderFactory factory;

    /**
     * Constructor using the {@link SamReaderFactory#DEFAULT}.
     *
     * @param source the source of reads (SAM/BAM/CRAM).
     *
     * @throws IOException if there is an IO error.
     */
    public SamSourceHandler(final String source) throws IOException {
        this(source, SamReaderFactory.makeDefault());
    }

    /**
     * Constructor using the provided factory.
     *
     * @param source  the source of reads (SAM/BAM/CRAM).
     * @param factory the factory to create the readers from.
     *
     * @throws IOException if there is an IO error.
     */
    public SamSourceHandler(final String source, final SamReaderFactory factory)
            throws IOException {
        super(source);
        this.factory = factory;
    }

    protected SamReader getFreshReader() {
        return factory.open(path);
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
