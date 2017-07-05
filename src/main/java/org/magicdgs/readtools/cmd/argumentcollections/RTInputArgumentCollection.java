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
import org.magicdgs.readtools.engine.RTDataSource;
import org.magicdgs.readtools.engine.sources.RTReadsSource;
import org.magicdgs.readtools.engine.sources.fastq.FastqPairEndSource;
import org.magicdgs.readtools.engine.sources.fastq.FastqSingleEndSource;
import org.magicdgs.readtools.engine.sources.sam.SamReadsSource;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;

import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.FastqQualityFormat;
import org.apache.logging.log4j.util.Supplier;
import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.utils.read.ReadConstants;

import java.io.File;
import java.io.Serializable;

/**
 * Argument collection for ReaTools input files.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: we should update this class to get the new data sources
public final class RTInputArgumentCollection implements Serializable {
    private static final long serialVersionUID = 1L;

    // TODO: change our default validation stringency?
    @Argument(fullName = StandardArgumentDefinitions.READ_VALIDATION_STRINGENCY_LONG_NAME, shortName = StandardArgumentDefinitions.READ_VALIDATION_STRINGENCY_SHORT_NAME,
            doc = "Validation stringency for all SAM/BAM/CRAM files read by this program. "
                    + "The default stringency value SILENT can improve performance when processing "
                    + "a BAM file in which variable-length data (read, qualities, tags) do not otherwise need to be decoded.",
            common = true, optional = true)
    public ValidationStringency readValidationStringency =
            ReadConstants.DEFAULT_READ_VALIDATION_STRINGENCY;

    @Argument(fullName = StandardArgumentDefinitions.INPUT_LONG_NAME, shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME, doc = "BAM/SAM/CRAM/FASTQ source of reads.", common = true, optional = false)
    public String inputSource;

    @Argument(fullName = RTStandardArguments.INPUT_PAIR_LONG_NAME, shortName = RTStandardArguments.INPUT_PAIR_SHORT_NAME, doc = "BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end).",
            common = true, optional = true, mutex = {RTStandardArguments.INTERLEAVED_INPUT_LONG_NAME})
    public String inputPair = null;

    @Argument(fullName = RTStandardArguments.INTERLEAVED_INPUT_LONG_NAME, shortName = RTStandardArguments.INTERLEAVED_INPUT_SHORT_NAME, doc = "Interleaved input.",
            common = true, optional = true, mutex = {RTStandardArguments.INPUT_PAIR_LONG_NAME})
    public boolean interleaved = false;

    @Advanced
    @Argument(fullName = RTStandardArguments.FORCE_QUALITY_ENCODING_NAME, shortName = RTStandardArguments.FORCE_QUALITY_ENCODING_NAME, doc = "Force original quality encoding of the input files.", common = true, optional = true)
    public FastqQualityFormat forceQualityEncoding = null;

    // supplier to change the reference
    private Supplier<RTDataSource> source = null;

    // creates the reader
    private ReadReaderFactory getReaderFactory(final File referenceFileName) {
        return new ReadReaderFactory()
                .setReferenceSequence(referenceFileName)
                .setValidationStringency(readValidationStringency);
    }

    /**
     * Gets the data source provided by the command line parameters.
     *
     * @param referenceFileName the reference file for CRAM inputs. May be {@code null}.
     * @deprecated use {@link #getReadsSource(File)} instead.
     */
    @Deprecated
    public RTDataSource getDataSource(final File referenceFileName) {
        RTDataSource.setReadReaderFactory(getReaderFactory(referenceFileName));
        if (source == null) {
            if (inputPair != null) {
                // there is already a mutually exclusive way of using interleaved, so no checking is necessary
                source = () -> new RTDataSource(inputSource, inputPair, forceQualityEncoding);
            } else {
                // the interleaved is by default false
                source = () -> new RTDataSource(inputSource, interleaved, forceQualityEncoding);
            }
        }
        return source.get();
    }

    // TODO: documentation
    public RTReadsSource getReadsSource(final File referenceFileName) {
        // get the correct source
        final RTReadsSource source;
        // because they are mutually-exclusive, this should work fine in the command line
        // maybe we should check just in case
        if (interleaved) {
            source = getSingleFilePaired(inputSource);
        } else if (inputPair != null) {
            source = getSplitFilesPaired(inputSource, inputPair);
        } else {
            source = getSingleEnd(inputSource);
        }
        // set the parameters for iterating
        // TODO: add parameter for maximum reads for quality?
        // TODO: add parameter to read asynchronously?
        return source
                .setForcedEncoding(forceQualityEncoding)
                .setValidationStringency(readValidationStringency)
                .setReferenceSequence(referenceFileName);

    }

    // TODO: documentation
    public static RTReadsSource getSplitFilesPaired(final String inputSource, final String inputPair) {
        if (ReadToolsIOFormat.isFastq(inputSource) && ReadToolsIOFormat.isFastq(inputPair)) {
            return new FastqPairEndSource(inputSource, inputPair);
        }
        // TODO: this removes support for other split files
        // TODO: throw an exception for non-supported splited files
        return null;
    }

    // TODO: documentation
    public static RTReadsSource getSingleFilePaired(final String inputSource) {
        if (ReadToolsIOFormat.isFastq(inputSource)) {
            return new FastqPairEndSource(inputSource);
        } else if (ReadToolsIOFormat.isSamBamOrCram(inputSource)) {
            return new SamReadsSource(inputSource, true);
        }
        // TODO: throw exception or default to FASTQ?
        return null;
    }

    // TODO: documentation
    public static RTReadsSource getSingleEnd(final String inputSource) {
        if (ReadToolsIOFormat.isFastq(inputSource)) {
            return new FastqSingleEndSource(inputSource);
        } else if (ReadToolsIOFormat.isSamBamOrCram(inputSource)) {
            return new SamReadsSource(inputSource, false);
        }
        // TODO: throw exception or default to FASTQ?
        return null;
    }

}
