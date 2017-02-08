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
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.Closeable;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Handler for a ReadTools source of reads. All the source handlers have the following properties:
 *
 * - Call to iteration methods always start from the beginning of the file/source.
 * - Operations for get the quality encoding and header always return a fresh instance independent
 * of previous calls.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class ReadsSourceHandler implements Closeable {

    /** The handled source string. */
    protected final String source;

    /**
     * Protected constructor. Use {@link #getHandler(String, ReadReaderFactory)} for detect the
     * handler or a concrete implementation.
     */
    protected ReadsSourceHandler(final String source) {
        this.source = source;
    }

    /** Gets the handled source. */
    public String getHandledSource() {
        return source;
    }

    /** Guess the quality encoding by reading {@code maxNumberOfReads}. */
    public abstract FastqQualityFormat getQualityEncoding(final long maxNumberOfReads);

    /** Gets the header for the source of reads. It may be a simple header, but never {@code null}. */
    public abstract SAMFileHeader getHeader();

    /**
     * Converts a source into an iterator. It always starts for the beginning of the file, so
     * multiple iterations does not affect the rest of the open iterations.
     */
    public abstract Iterator<GATKRead> toIterator();

    /**
     * Converts a source into an iterator which overlaps with the provided intervals. It always
     * starts for the beginning of the file, so multiple iterations does not affect the rest of the
     * open iterations.
     *
     * Note: unmapped reads will not be returned by this iterator.
     */
    public abstract Iterator<GATKRead> toIntervalIterator(final List<SimpleInterval> locs);

    /** Returns an ordered stream for the reads. */
    public Stream<GATKRead> toStream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(toIterator(), Spliterator.ORDERED),
                false);
    }

    /**
     * Gets a handler to the provided source: FASTQ or SAM/BAM/CRAM.
     *
     * Note: the source use the extension to determine the kind of source.
     *
     * @param source the source string.
     *
     * @return the source handler.
     */
    public static ReadsSourceHandler getHandler(final String source,
            final ReadReaderFactory factory) {
        // first check if it is a SAM/BAM/CRAM format
        if (ReadToolsIOFormat.isSamBamOrCram(source)) {
            return new SamSourceHandler(source, factory);
        } else if (ReadToolsIOFormat.isFastq(source)) {
            return new FastqSourceHandler(source, factory);
        }
        throw new UserException.CouldNotReadInputFile(source,
                "not recognized extension for reads source.");
    }
}
