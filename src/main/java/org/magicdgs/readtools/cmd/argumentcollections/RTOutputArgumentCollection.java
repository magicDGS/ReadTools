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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

import java.io.File;
import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Abstract class for all output arguments in ReadTools.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class RTOutputArgumentCollection implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final static Logger logger = LogManager.getLogger(RTOutputArgumentCollection.class);

    @Argument(fullName = RTStandardArguments.FORCE_OVERWRITE_NAME, shortName = RTStandardArguments.FORCE_OVERWRITE_NAME, doc = "Force output overwriting if it exists", optional = true)
    public Boolean forceOverwrite = false;

    /**
     * Gets a fresh default factory. Implementations should call the super method to honor the
     * common arguments.
     */
    protected ReadWriterFactory getWriterFactory() {
        return new ReadWriterFactory()
                .setForceOverwrite(forceOverwrite);
    }

    /**
     * Gets the output writer for the arguments.
     *
     * @param header        the header for the output file.
     * @param programRecord program record supplier. May be {@code null}, but should not return
     *                      {@code null}.
     * @param presorted     if {@code true}, the output is assumed to be pre-sorted.
     * @param referenceFile the reference file for CRAM output. May be {@code null}.
     */
    public final GATKReadWriter outputWriter(final SAMFileHeader header,
            final Supplier<SAMProgramRecord> programRecord, final boolean presorted,
            final File referenceFile) {
        Utils.nonNull(header, "null header");
        updateHeader(header, programRecord);
        return createWriter(getWriterFactory().setReferenceFile(referenceFile),
                header, presorted);
    }

    /**
     * Creates the writer with the provided factory, updated header and presorted.
     *
     * @param factory   the factory to use for get the output writer.
     * @param header    the header for the output file (already updated).
     * @param presorted if {@code true}, the output is assumed to be pre-sorted.
     */
    protected abstract GATKReadWriter createWriter(final ReadWriterFactory factory,
            final SAMFileHeader header, boolean presorted);

    /** Updates the header if necessary. */
    protected abstract void updateHeader(final SAMFileHeader header,
            final Supplier<SAMProgramRecord> programRecord);


    /** Returns the default output collection for ReadTools (only SAM/BAM/CRAM files). */
    public static final RTOutputArgumentCollection defaultOutput() {
        return new RTOutputBamArgumentCollection();
    }

    /** Returns the a collection for output FASTQ files. */
    public static final RTOutputArgumentCollection fastqOutput() {
        return new RTOutputFastqArgumentCollection();
    }

    /** Returns an output collection for SAM/BAM/CRAM files and splitting them. */
    public static final RTOutputArgumentCollection splitOutput() {
        return new RTOutputBamSplitArgumentCollection();
    }

}
