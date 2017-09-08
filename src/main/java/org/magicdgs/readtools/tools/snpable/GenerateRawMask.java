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

package org.magicdgs.readtools.tools.snpable;

import org.magicdgs.readtools.engine.ReadToolsProgram;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@BetaFeature
@CommandLineProgramProperties(oneLineSummary = "Parse the result of reference kmer mapping and compute the raw mask",
        summary = "",
        programGroup = SnpableProgramGroup.class)
// TODO: this should be omitted from the CLI for now
public class GenerateRawMask extends ReadToolsProgram {

    @Argument(fullName = StandardArgumentDefinitions.INPUT_LONG_NAME, shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME, doc = Snpable.SNPABLE_INPUT_BAM_DESC)
    public String inputPath;

    @Argument(fullName = Snpable.RAW_MASK_OUTPUT_NAME, doc = Snpable.RAW_MASK_OUTPUT_DESC, optional =  false)
    public String output;

    private SamReader reader;
    private PrintStream stream;

    @Override
    protected void onStartup() {
        try {
            reader = new ReadReaderFactory().openSamReader(IOUtils.getPath(inputPath));
            stream = new PrintStream(Files.newOutputStream(IOUtils.getPath(output)));
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
    }

    @Override
    protected Object doWork() {
        // read length is not used internally except for the length of the mask map
        Snpable.computeRawMask(reader, 0, stream);
        return null;
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(reader);
        CloserUtil.close(stream);
    }

}
