/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.tools.quality;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.cmd.programgroups.DeprecatedProgramGroup;
import org.magicdgs.readtools.utils.fastq.QualityUtils;

import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;

import java.io.File;

/**
 * Tool for check the quality in both FASTQ and BAM files
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @deprecated this tool correspond to legacy elements.
 */
@CommandLineProgramProperties(oneLineSummary = "DEPRECATED: USE 'QualityEncodingDetector' for get the quality encoding for a BAM/FASTQ file.",
        summary = "DEPRECATED: USE 'QualityEncodingDetector' instead.\n"
                + "Check the quality encoding for a BAM/FASTQ file and output in the STDOUT the encoding.",
        programGroup = DeprecatedProgramGroup.class)
@Deprecated
public final class QualityChecker extends CommandLineProgram {

    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.INPUT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.INPUT_SHORT_NAME, optional = false,
            doc = "Input BAM/FASTQ to determine the quality.")
    public File input;

    @Argument(fullName = "maximum-reads", optional = true, doc = "Maximum number of read to use to iterate.")
    public Long recordsToIterate = RTDefaults.MAX_RECORDS_FOR_QUALITY;

    @Override
    protected String[] customCommandLineValidation() {
        if (recordsToIterate <= 0) {
            throw new CommandLineException.BadArgumentValue("maximum-reads",
                    recordsToIterate.toString(), "should be a positive integer");
        }
        return super.customCommandLineValidation();
    }

    @Override
    protected Object doWork() {
        final FastqQualityFormat format =
                QualityUtils.getFastqQualityFormat(input, recordsToIterate);
        return (format == FastqQualityFormat.Standard) ? "Sanger" : "Illumina";
    }
}
