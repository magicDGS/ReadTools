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
import org.magicdgs.readtools.utils.misc.IOUtils;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

import java.io.File;
import java.util.function.Supplier;

/**
 * Output argument collection for output SAM/BAM/CRAM files.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTOutputBamArgumentCollection implements ArgumentCollectionDefinition {
    private static final long serialVersionUID = 1L;

    // TODO: add option for splitting by different splitters

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME, shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, doc = "Output SAM/BAM/CRAM file.", optional = false)
    public String outputName;

    @Argument(fullName = RTStandardArguments.FORCE_OVERWRITE_NAME, shortName = RTStandardArguments.FORCE_OVERWRITE_NAME, doc = "Force output overwriting if it exists", optional = true)
    public Boolean forceOverwrite = false;

    @Argument(fullName = StandardArgumentDefinitions.CREATE_OUTPUT_BAM_INDEX_LONG_NAME, shortName = StandardArgumentDefinitions.CREATE_OUTPUT_BAM_INDEX_SHORT_NAME, doc = "If true, create a BAM/CRAM index when writing a coordinate-sorted BAM/CRAM file.", optional = true)
    public boolean createOutputBamIndex = true;

    @Argument(fullName = StandardArgumentDefinitions.CREATE_OUTPUT_BAM_MD5_LONG_NAME, shortName = StandardArgumentDefinitions.CREATE_OUTPUT_BAM_MD5_SHORT_NAME, doc = "If true, create a MD5 digest for any BAM/SAM/CRAM file created", optional = true)
    public boolean createOutputBamMD5 = false;

    @Argument(fullName = "addOutputSAMProgramRecord", shortName = "addOutputSAMProgramRecord", doc = "If true, adds a PG tag to created SAM/BAM/CRAM files.", optional = true)
    public boolean addOutputSAMProgramRecord = true;

    /** Gets the writer factory for the arguments, adding also the reference file. */
    protected ReadWriterFactory getWriterFactory(final File referenceFile) {
        return new ReadWriterFactory()
                .setReferenceFile(referenceFile)
                .setForceOverwrite(forceOverwrite)
                .setCreateIndex(createOutputBamIndex)
                .setCreateMd5File(createOutputBamMD5);
    }

    /**
     * Gets the output writer for the arguments.
     *
     * @param referenceFile the reference file for CRAM output. May be {@code null}.
     * @param header        the header for the output file.
     * @param presorted     if {@code true}, the output is assumed to be pre-sorted.
     * @param programRecord program record supplier. May be {@code null}, but should not return
     *                      {@code null}.
     */
    public GATKReadWriter outputWriter(final File referenceFile, final SAMFileHeader header,
            final boolean presorted, final Supplier<SAMProgramRecord> programRecord) {
        if (addOutputSAMProgramRecord && programRecord != null) {
            header.addProgramRecord(programRecord.get());
        }
        if (!IOUtils.isSamBamOrCram(outputName)) {
            // TODO: GATK4 pr for String/Path/File on UserExceptions for files
            throw new UserException.CouldNotCreateOutputFile(new File(outputName),
                    "The output file should have a BAM/SAM/CRAM extension.");
        }
        return getWriterFactory(referenceFile)
                .createSAMWriter(outputName, header, presorted);
    }
}
