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

package org.magicdgs.readtools.engine.sources;

import org.magicdgs.readtools.utils.iterators.InterleaveGATKReadIterators;
import org.magicdgs.readtools.utils.iterators.paired.GATKReadPairedIterator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamFileHeaderMerger;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Source of read pairs stored in two different sources, one for the first of pair and another for
 * the second of pair.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class PairedEndSplitReadsSource implements RTReadsSource {

    // wrapped sources
    private final RTReadsSource first;
    private final RTReadsSource second;

    // cached format
    private FastqQualityFormat format = null;

    /**
     * Constructor from two sources, containing the first and the second of pair.
     *
     * @param first  source of reads for the first of pair.
     * @param second source of reads for the second of pair.
     */
    public PairedEndSplitReadsSource(final RTReadsSource first, final RTReadsSource second) {
        Utils.nonNull(first, "null first");
        Utils.nonNull(second, "null second");
        Utils.validateArg(!first.isPaired(), () -> String.format(
                "first source cannot be paired for split files: %s (%s)",
                first.getSourceFormat(), first.getSourceName()));
        Utils.validateArg(!second.isPaired(), () -> String.format(
                "second source cannot be paired for split files: %s (%s)",
                first.getSourceFormat(), first.getSourceName()));
        this.first = first;
        this.second = second;
    }

    @Override
    public Iterator<Tuple2<GATKRead, GATKRead>> getPairedIterator() {
        return GATKReadPairedIterator.of(first.iterator(), second.iterator());
    }

    @Override
    public Iterator<GATKRead> iterator() {
        return new InterleaveGATKReadIterators(first.iterator(), second.iterator());
    }

    @Override
    public FastqQualityFormat getQualityEncoding() {
        if (format == null) {
            final FastqQualityFormat format1 = first.getQualityEncoding();
            final FastqQualityFormat format2 = second.getQualityEncoding();
            if (format2.equals(format1)) {
                return format1;
            }
            throw new UserException(String.format(
                    "Found two different quality encoding for %s: %s (%s) vs. %s (%s)",
                    getSourceFormat(),
                    format1, first.getSourceName(),
                    format2, second.getSourceName()));
        }
        return format;
    }

    @Override
    public SAMFileHeader getHeader() {
        final SAMFileHeader header1 = first.getHeader();
        final SAMFileHeader header2 = second.getHeader();
        if (!header1.getSortOrder().equals(header2.getSortOrder())) {
            throw new UserException(String.format(
                    "%s (%s): different sort orders found (%s vs. %s)",
                    getSourceFormat(), getSourceName(),
                    header1.getSortOrder(), header2.getSortOrder()));
        }
        return new SamFileHeaderMerger(header1.getSortOrder(), Arrays.asList(header1, header2),
                true)
                .getMergedHeader();
    }

    @Override
    public boolean isPaired() {
        return true;
    }

    @Override
    public String getSourceFormat() {
        final String firstFormat = first.getSourceFormat();
        final String secondFormat = second.getSourceFormat();
        if (firstFormat.equals(secondFormat)) {
            return firstFormat + "-split";
        }
        return firstFormat + "-" + secondFormat;
    }

    /** Gets the source name of both sources, separated by commas. */
    @Override
    public String getSourceName() {
        return first.getSourceName() + "," + second.getSourceName();
    }

    @Override
    public RTReadsSource setValidationStringency(ValidationStringency stringency) {
        first.setValidationStringency(stringency);
        second.setValidationStringency(stringency);
        return this;
    }

    @Override
    public RTReadsSource setUseAsyncIo(boolean useAsyncIo) {
        first.setUseAsyncIo(useAsyncIo);
        second.setUseAsyncIo(useAsyncIo);
        return this;
    }

    @Override
    public RTReadsSource setReferenceSequence(File referenceFile) {
        first.setReferenceSequence(referenceFile);
        second.setReferenceSequence(referenceFile);
        return this;
    }

    @Override
    public RTReadsSource setForcedEncoding(FastqQualityFormat encoding) {
        first.setForcedEncoding(encoding);
        second.setForcedEncoding(encoding);
        return this;
    }

    @Override
    public RTReadsSource setMaxNumberOfReadsForQuality(long maxNumberOfReadsForQuality) {
        first.setMaxNumberOfReadsForQuality(maxNumberOfReadsForQuality);
        second.setMaxNumberOfReadsForQuality(maxNumberOfReadsForQuality);
        return this;
    }

    @Override
    public void close() {
        final List<UserException> exceptions = new ArrayList<>(2);
        try {
            first.close();
        } catch (UserException e) {
            exceptions.add(e);
        }
        try {
            second.close();
        } catch (UserException e) {
            exceptions.add(e);
        }
        switch (exceptions.size()) {
            case 1:
                throw exceptions.get(0);
            case 2:
                throw new UserException(String.format("Error closing %s (%s): %s and %s",
                        getSourceFormat(), getSourceName(),
                        exceptions.get(0).getMessage(), exceptions.get(0).getMessage()));
            default:
                // do nothing
        }
    }
}
