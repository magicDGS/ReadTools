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

package org.magicdgs.readtools.tools.quality;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.engine.ReadToolsProgram;
import org.magicdgs.readtools.engine.sourcehandler.ReadsSourceHandler;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;

import htsjdk.samtools.ValidationStringency;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.utils.read.ReadConstants;
import picard.cmdline.programgroups.DiagnosticsAndQCProgramGroup;

import java.io.IOException;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Detects the quality encoding format for all kind of sources for ReadTools.",
        summary = "Detects the quality encoding for a SAM/BAM/CRAM/FASTQ files, output to the STDOUT the quality encoding.",
        programGroup = DiagnosticsAndQCProgramGroup.class)
@DocumentedFeature
public final class QualityEncodingDetector extends ReadToolsProgram {

    @Argument(fullName = RTStandardArguments.INPUT_LONG_NAME, shortName = RTStandardArguments.INPUT_SHORT_NAME, doc = "Reads input.", optional = false, common = true)
    public String sourceString;

    @Argument(fullName = "maximumReads", shortName = "maximumReads", doc = "Maximum number of reads to use for detect the quality encoding.", optional = true)
    public Long recordsToIterate = RTDefaults.MAX_RECORDS_FOR_QUALITY;

    @Argument(fullName = RTStandardArguments.READ_VALIDATION_STRINGENCY_LONG_NAME, shortName = RTStandardArguments.READ_VALIDATION_STRINGENCY_SHORT_NAME,
            doc = RTStandardArguments.READ_VALIDATION_STRINGENCY_DOC,
            common = true, optional = true)
    public ValidationStringency readValidationStringency =
            ReadConstants.DEFAULT_READ_VALIDATION_STRINGENCY;

    @Override
    protected String[] customCommandLineValidation() {
        if (recordsToIterate <= 0) {
            throw new CommandLineException.BadArgumentValue("maximumReads",
                    recordsToIterate.toString(), "should be a positive integer");
        }
        return super.customCommandLineValidation();
    }

    @Override
    protected Object doWork() {
        try (final ReadsSourceHandler handler = ReadsSourceHandler.getHandler(sourceString, new ReadReaderFactory().setValidationStringency(readValidationStringency))) {
            return handler.getQualityEncoding(recordsToIterate);
        } catch (IOException e) {
            logger.debug(e);
        }
        logger.warn("Unable to detect quality encoding");
        return "UNDEFINED";
    }
}
