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

package org.magicdgs.readtools.utils.distmap;

import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import scala.Tuple2;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Abstract class for GATKRead writers.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DistmapGATKWriter implements GATKReadWriter {

    // the name of the source for the output
    private final String sourceName;
    // where to write the records
    private final Writer writer;
    // if it is single-end, this will just write the read out
    // if it is pair-end, this will use DistmapPairedConsumer
    private final Consumer<GATKRead> singleEndHandler;

    /**
     * Public constructor.
     *
     * @param writer     output to write the reads on.
     * @param paired     if {@code true}, the file will be written as paired; otherwise,
     *                   it will be written as single.
     * @param sourceName the name for the source where we are writing to.
     */
    public DistmapGATKWriter(final Writer writer, final String sourceName,
            final boolean paired) {
        this.writer = writer;
        this.sourceName = sourceName;
        this.singleEndHandler = (paired)
                ? new DistmapPairedConsumer(this::addPair)
                : read -> printAndCheckError(() -> DistmapEncoder.encode(read));
    }

    /**
     * Adds a read to the writer. If added in pair-end mode, the reads in the pair are expected to
     * be passed one after the other.
     */
    @Override
    public void addRead(final GATKRead read) {
        singleEndHandler.accept(read);
    }

    /**
     * Writes a pair of reads in the Distmap format.
     *
     * @param pair the pair of reads.
     */
    public void addPair(final Tuple2<GATKRead, GATKRead> pair) {
        printAndCheckError(() -> DistmapEncoder.encode(pair));
    }

    /**
     * Close the underlying writer.
     *
     * @throws DistmapException if the writer is in paired and not second read is added.
     */
    @Override
    public void close() throws IOException {
        // close the handler if it is a DistmapPairedConsumer
        CloserUtil.close(singleEndHandler);
        writer.close();
    }

    // helper method to print and with the writer and check if an error occurs to throw an exception
    // uses a supplier to do not store the String
    private void printAndCheckError(final Supplier<String> toPrint) {
        try {
            writer.write(toPrint.get());
            writer.write("\n");
            // catch exceptions due to formatting too
        } catch (final IOException | DistmapException e) {
            throw new UserException.CouldNotCreateOutputFile(sourceName,
                    e.getClass().getSimpleName(), e);
        }
    }

    // private class to handle pair-end reads
    // it keeps the first and second in pair
    // it is closeable because it should throw if the consumer have an unexpected ending
    private final static class DistmapPairedConsumer implements Consumer<GATKRead>, Closeable {

        // first and second read passed to the handler, afterwards they are cleaned
        private GATKRead first = null;

        private final Consumer<Tuple2<GATKRead, GATKRead>> consumer;

        private DistmapPairedConsumer(final Consumer<Tuple2<GATKRead, GATKRead>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void accept(final GATKRead read) {
            if (first == null) {
                first = read;
            } else {
                // assume that it is the second
                consumer.accept(new Tuple2<>(first, read));
                first = null;
            }
        }

        // throws if the first is not null
        @Override
        public void close() throws IOException {
            DistmapException.distmapValidation(first == null,
                    () -> "missing second pair for " + first.getName());
        }
    }
}
