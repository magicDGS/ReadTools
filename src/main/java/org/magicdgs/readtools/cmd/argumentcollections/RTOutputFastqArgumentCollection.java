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
import org.magicdgs.readtools.utils.read.ReadWriterFactory;
import org.magicdgs.readtools.utils.read.writer.PairEndSplitter;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;
import org.magicdgs.readtools.utils.read.writer.SplitGATKWriter;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.tools.readersplitters.ReaderSplitter;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Output argument collection for output FASTQ files.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class RTOutputFastqArgumentCollection extends RTOutputArgumentCollection {

    // this is the default splitter for pair-end reads.
    private static final List<ReaderSplitter<?>> PAIR_END_SPLITTER =
            Collections.singletonList(new PairEndSplitter());

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME, shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, doc = "Output FASTQ file prefix.", optional = false)
    public String outputPrefix;

    @Argument(fullName = RTStandardArguments.OUTPUT_FORMAT_NAME, shortName = RTStandardArguments.OUTPUT_FORMAT_NAME, doc = "FASTQ output format.", optional = true)
    public ReadToolsIOFormat.FastqFormat outputFormat = ReadToolsIOFormat.FastqFormat.GZIP;

    // TODO: this creates the MD5 not for the gzipped file, but for the FASTQ contents themselves
    @Argument(fullName = RTStandardArguments.CREATE_OUTPUT_FASTQ_MD5_LONG_NAME, shortName = RTStandardArguments.CREATE_OUTPUT_FASTQ_MD5_SHORT_NAME, doc = "If true, create a MD5 digest for FASTQ file(s).", optional = true)
    public boolean createsMd5 = false;

    @Argument(fullName = RTStandardArguments.INTERLEAVED_OUTPUT_FASTQ_LONG_NAME, shortName = RTStandardArguments.INTERLEAVED_OUTPUT_FASTQ_SHORT_NAME, doc = "If true, creates an interleaved FASTQ output. Otherwise, it will be splited by pairs/single end.", optional = true)
    public boolean interleaved = false;


    @Override
    public ReadWriterFactory getWriterFactory() {
        return super.getWriterFactory()
                .setCreateMd5File(createsMd5);
    }

    @Override
    public String getOutputNameWithSuffix(final String suffix) {
        return outputPrefix + suffix + outputFormat.getExtension();
    }

    @Override
    public Path makeMetricsFile(final String suffix) {
        final String prefix = (suffix == null) ? outputPrefix : outputPrefix + suffix;
        return ReadToolsIOFormat.makeMetricsFile(prefix);
    }

    @Override
    protected void updateHeader(final SAMFileHeader header,
            final Supplier<SAMProgramRecord> programRecord) {
        // do nothing!
    }

    @Override
    protected GATKReadWriter createWriter(final ReadWriterFactory factory,
            final SAMFileHeader header, final boolean presorted) {
        return (interleaved) ? interleavedOutput(factory) : splitOutput(factory);
    }

    // this creates the split output
    private GATKReadWriter splitOutput(final ReadWriterFactory factory) {
        // header is not important for FASTQ files
        return new SplitGATKWriter(outputPrefix, outputFormat, PAIR_END_SPLITTER,
                new SAMFileHeader(), true, factory, false);
    }

    // this creates the interleaved output
    private GATKReadWriter interleavedOutput(final ReadWriterFactory factory) {
        final String outputName = outputPrefix + outputFormat.getExtension();
        return factory.createFASTQWriter(outputName);
    }
}
