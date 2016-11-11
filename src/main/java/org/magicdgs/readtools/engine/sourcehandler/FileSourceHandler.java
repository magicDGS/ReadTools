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
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Helper for implement a source handler from a file. Close operations are encapsulated here.
 *
 * @param <T> closeable reader.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
abstract class FileSourceHandler<T extends Closeable> extends ReadsSourceHandler {

    // keep all the readers that have been open to close them all at the end
    private final List<T> openReaders = new ArrayList<>();

    /** The path to handle. */
    protected final Path path;

    /**
     * Protected constructor. Use {@link #getHandler(String)} for detect the handler.
     */
    protected FileSourceHandler(String source) {
        super(source);
        this.path = IOUtils.getPath(source);
    }

    /**
     * Gets a fresh reader for the {@link #path}.
     *
     * @return the fresh reader.
     */
    protected abstract T getFreshReader();

    /** Returns the header for this reader. */
    protected abstract SAMFileHeader getReaderHeader(final T reader);

    /** Returns the quality encoding from the reader. */
    protected abstract FastqQualityFormat getReaderQualityEncoding(final T reader,
            long maxNumberOfReads);

    /** Returns an iterator for this reader. */
    protected abstract Iterator<GATKRead> getReaderIterator(final T reader);

    /** Returns an iterator over the locations for this reader. */
    protected abstract Iterator<GATKRead> getReaderIntervalIterator(final T reader,
            final List<SimpleInterval> locs);

    @Override
    public FastqQualityFormat getQualityEncoding(long maxNumberOfReads) {
        return readAndClose(r -> getReaderQualityEncoding(r, maxNumberOfReads));
    }

    /**
     * Default implementation open a fresh reader, retrieve the header from it and close the
     * reader.
     *
     * {@inheritDoc}
     */
    @Override
    public SAMFileHeader getHeader() {
        return readAndClose(this::getReaderHeader);
    }

    private <O> O readAndClose(final Function<T, O> getter) {
        final T reader = getFreshReader();
        final O toReturn = getter.apply(reader);
        CloserUtil.close(reader);
        return toReturn;
    }

    /**
     * Default implementation open a fresh reader, add it to the open readers (to close when {@link
     * #close()} is call, and return the iterator.
     *
     * {@inheritDoc}
     */
    @Override
    public Iterator<GATKRead> toIterator() {
        final T reader = getFreshReader();
        openReaders.add(reader);
        return getReaderIterator(reader);
    }

    /**
     * Default implementation open a fresh reader, add it to the open readers (to close when {@link
     * #close()} is call, and return the iterator.
     *
     * {@inheritDoc}
     */
    @Override
    public Iterator<GATKRead> toIntervalIterator(final List<SimpleInterval> locs) {
        final T reader = getFreshReader();
        openReaders.add(reader);
        return getReaderIntervalIterator(reader, locs);
    }

    @Override
    public final void close() throws IOException {
        CloserUtil.close(openReaders);
        openReaders.clear();
    }
}
