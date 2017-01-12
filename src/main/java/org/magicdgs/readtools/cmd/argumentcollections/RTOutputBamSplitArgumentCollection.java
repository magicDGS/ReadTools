/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gómez-Sánchez
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
import org.magicdgs.readtools.utils.read.writer.ReadToolsOutputFormat;
import org.magicdgs.readtools.utils.read.writer.SplitGATKWriter;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.tools.readersplitters.LibraryNameSplitter;
import org.broadinstitute.hellbender.tools.readersplitters.ReadGroupIdSplitter;
import org.broadinstitute.hellbender.tools.readersplitters.ReaderSplitter;
import org.broadinstitute.hellbender.tools.readersplitters.SampleNameSplitter;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Output argument collection for output SAM/BAM/CRAM files but allowing splitting by sample,
 * read group and/or library.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
class RTOutputBamSplitArgumentCollection extends RTAbstractOutputBamArgumentCollection {
    private static final long serialVersionUID = 1L;

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME, shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, doc = "Output SAM/BAM/CRAM file prefix.", optional = false)
    public String outputPrefix;

    @Argument(fullName = RTStandardArguments.OUTPUT_FORMAT_NAME, shortName = RTStandardArguments.OUTPUT_FORMAT_NAME, doc = "SAM/BAM/CRAM output format.", optional = true)
    public ReadToolsOutputFormat.BamFormat outputFormat = ReadToolsOutputFormat.BamFormat.BAM;


    @Argument(fullName = "splitBySample", shortName = "splitSM", doc = "Split file by sample.", optional = true)
    public boolean splitBySample = false;

    @Argument(fullName = "splitByReadGroup", shortName = "splitRG", doc = "Split file by read group.", optional = true)
    public boolean splitByReadGroup = false;

    @Argument(fullName = "splitByLibrary", shortName = "splitLB", doc = "Split file by library.", optional = true)
    public boolean splitByLibrary = false;

    @Override
    public String getOutputNameWithSuffix(final String suffix) {
        return outputPrefix + suffix + outputFormat.getExtension();
    }

    @Override
    public Path makeMetricsFile(String suffix) {
        final String prefix = (suffix == null) ? outputPrefix : outputPrefix + suffix;
        return IOUtils.makeMetricsFile(prefix);
    }

    @Override
    protected GATKReadWriter createWriter(final ReadWriterFactory factory,
            final SAMFileHeader header, final boolean presorted) {
        // set the splitter
        final List<ReaderSplitter<?>> splitter = new ArrayList<>(3);
        // first sample
        if (splitBySample) {
            splitter.add(new SampleNameSplitter());
        }
        // second the read group
        if (splitByReadGroup) {
            splitter.add(new ReadGroupIdSplitter());
        }
        // third the library
        if (splitByLibrary) {
            splitter.add(new LibraryNameSplitter());
        }

        // if there is a splitter, split; if not, output a simple writer
        return splitter.isEmpty()
                ? factory.createSAMWriter(outputPrefix + outputFormat.getExtension(),
                header, presorted)
                : new SplitGATKWriter(outputPrefix, outputFormat,
                        splitter, header, presorted, factory, false);
    }
}
