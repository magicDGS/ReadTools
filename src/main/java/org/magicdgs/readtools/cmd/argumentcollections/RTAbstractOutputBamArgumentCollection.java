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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.utils.read.ReadWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;

import java.util.function.Supplier;

/**
 * Abstract output argument collection for output SAM/BAM/CRAM files.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
abstract class RTAbstractOutputBamArgumentCollection extends RTOutputArgumentCollection {
    private static final long serialVersionUID = 1L;

    @Argument(fullName = StandardArgumentDefinitions.CREATE_OUTPUT_BAM_INDEX_LONG_NAME, shortName = StandardArgumentDefinitions.CREATE_OUTPUT_BAM_INDEX_SHORT_NAME, doc = "If true, create a BAM/CRAM index when writing a coordinate-sorted BAM/CRAM file.", optional = true, common = true)
    public boolean createOutputBamIndex = true;

    @Argument(fullName = StandardArgumentDefinitions.CREATE_OUTPUT_BAM_MD5_LONG_NAME, shortName = StandardArgumentDefinitions.CREATE_OUTPUT_BAM_MD5_SHORT_NAME, doc = "If true, create a MD5 digest for any BAM/SAM/CRAM file created", optional = true, common = true)
    public boolean createOutputBamMD5 = false;

    @Argument(fullName = StandardArgumentDefinitions.ADD_OUTPUT_SAM_PROGRAM_RECORD, shortName = StandardArgumentDefinitions.ADD_OUTPUT_SAM_PROGRAM_RECORD, doc = "If true, adds a PG tag to created SAM/BAM/CRAM files.", optional = true, common = true)
    public boolean addOutputSAMProgramRecord = true;

    /** Gets the writer factory for the arguments, adding also the reference file. */
    @Override
    public final ReadWriterFactory getWriterFactory() {
        return super.getWriterFactory()
                .setForceOverwrite(forceOverwrite)
                .setCreateIndex(createOutputBamIndex)
                .setCreateMd5File(createOutputBamMD5);
    }

    /**
     * Updates the header with the program record if {@link #addOutputSAMProgramRecord} is
     * {@code true} and the supplier is not {@code null}.
     */
    @Override
    protected final void updateHeader(final SAMFileHeader header,
            final Supplier<SAMProgramRecord> programRecord) {
        if (addOutputSAMProgramRecord && programRecord != null) {
            header.addProgramRecord(programRecord.get());
        }
    }
}
