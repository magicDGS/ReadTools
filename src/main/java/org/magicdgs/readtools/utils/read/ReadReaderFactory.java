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

package org.magicdgs.readtools.utils.read;

import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.fastq.FastqReader;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Factory for generate readers for all sources of reads with the same parameters. If any error
 * occurs when opening a reader, an {@link UserException.CouldNotCreateOutputFile} is thrown.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadReaderFactory {

    private final SamReaderFactory samFactory;

    /** Creates a default factory. */
    public ReadReaderFactory() {
        this.samFactory = SamReaderFactory.makeDefault();

    }

    /** Sets the validation stringency. */
    public ReadReaderFactory setValidationStringency(final ValidationStringency stringency) {
        // TODO: this should also set validation stringency for FASTQ reader
        samFactory.validationStringency(stringency);
        return this;
    }

    /** Gets the validation stringency for the SAMReader factory. */
    public ValidationStringency validationStringency() {
        return samFactory.validationStringency();
    }

    /** Sets if asynchronous reading should be used. */
    public ReadReaderFactory setUseAsyncIo(final boolean useAsyncIo) {
        samFactory.setUseAsyncIo(useAsyncIo);
        return this;
    }

    /** Set the reference sequence for reading. */
    public ReadReaderFactory setReferenceSequence(final File referenceFile) {
        samFactory.referenceSequence(referenceFile);
        return this;
    }

    /** Open a new SAMReader from a path. */
    public SamReader openSamReader(final Path path) {
        return openWrappingException(() -> samFactory.open(path), path::toString);
    }

    /** Open a new SAMReader from a file. */
    public SamReader openSamReader(final File file) {
        return openWrappingException(() -> samFactory.open(file), file::getAbsolutePath);
    }

    /** Open a new SAMReader from a resource. */
    public SamReader openSamReader(final SamInputResource resource) {
        // TODO: probably this should disappear in favour of a String open.
        return openWrappingException(() -> samFactory.open(resource), resource::toString);
    }

    /** Open a new FastqReader from a path. */
    public FastqReader openFastqReader(final Path path) {
        return openFastqReader(path.toFile());
    }

    /** Open a new FastqReaderr from a file. */
    public FastqReader openFastqReader(final File file) {
        return openWrappingException(() -> new FastqReader(file), file::getAbsolutePath);
    }

    // any exception caused by open a file will thrown a could not read input file exception
    private static <T> T openWrappingException(final Callable<T> opener,
            final Supplier<String> source) {
        try {
            return opener.call();
        } catch (Exception e) {
            throw new UserException.CouldNotReadInputFile(source.get(), e);
        }
    }


}
