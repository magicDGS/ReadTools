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

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;
import org.magicdgs.readtools.utils.read.ReadWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import org.apache.commons.io.FilenameUtils;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

import java.nio.file.Path;

/**
 * Simple output argument collection for output SAM/BAM/CRAM files, without splitting.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class RTOutputBamArgumentCollection extends RTAbstractOutputBamArgumentCollection {
    private static final long serialVersionUID = 1L;

    @Argument(fullName = RTStandardArguments.OUTPUT_LONG_NAME, shortName = RTStandardArguments.OUTPUT_SHORT_NAME, doc = "Output SAM/BAM/CRAM file.", optional = false)
    public String outputName;

    @Override
    public String getOutputNameWithSuffix(final String suffix) {
        final String outputNameWithSuffix = FilenameUtils.removeExtension(outputName) + suffix
                + "." + FilenameUtils.getExtension(outputName);
        validateUserOutput(outputNameWithSuffix);
        return outputNameWithSuffix;
    }

    @Override
    public Path makeMetricsFile(String suffix) {
        String prefix = FilenameUtils.removeExtension(outputName);
        if (suffix != null) {
            prefix += suffix;
        }
        return ReadToolsIOFormat.makeMetricsFile(prefix);
    }

    @Override
    public void validateUserOutput() {
        validateUserOutput(outputName);
    }

    /**
     * Checks if the output name is a SAM/BAM/CRAM file and if so it creates a SAM writer.
     * Otherwise, it thrown an UserException.
     */
    @Override
    protected GATKReadWriter createWriter(final ReadWriterFactory factory,
            final SAMFileHeader header, final boolean presorted) {
        validateUserOutput();
        return factory.createSAMWriter(outputName, header, presorted);
    }
}
