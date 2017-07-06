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
import htsjdk.samtools.util.MergingIterator;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTSplitPairEndSource implements RTReadsSource {

    private final RTReadsSource first;

    private final RTReadsSource second;

    public RTSplitPairEndSource(RTReadsSource first, RTReadsSource second) {
        this.first = Utils.nonNull(first, "null first");
        this.second = Utils.nonNull(first, "null second");
        Utils.validateArg(!first.isPaired(), "source cannot be paired for split files: " + first.getSourceDescription());
        Utils.validateArg(!second.isPaired(), "source cannot be paired for split files: " + second.getSourceDescription());
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
        final FastqQualityFormat format1 = first.getQualityEncoding();
        final FastqQualityFormat format2 = second.getQualityEncoding();
        if (format2.equals(format1)) {
            return format1;
        }
        throw new UserException(String.format("%s: different encoding found (%s vs. %s)",
                getSourceDescription(), format1, format2));
    }

    @Override
    public SAMFileHeader getHeader() {
        final SAMFileHeader header1 = first.getHeader();
        final SAMFileHeader header2 = second.getHeader();
        if (!header1.getSortOrder().equals(header2.getSortOrder())) {
            throw new UserException(String.format("%s: different sort orders found (%s vs. %s)",
                    getSourceDescription(), header1.getSortOrder(), header2.getSortOrder()));
        }
        final SamFileHeaderMerger merger = new SamFileHeaderMerger(
                header1.getSortOrder(), Arrays.asList(header1, header2), true);
        return merger.getMergedHeader();
    }

    @Override
    public boolean isPaired() {
        return true;
    }

    @Override
    public Iterator<GATKRead> query(List<SimpleInterval> locs) {
        // TODO: this requires merging iterator, no?
        return null;
    }

    @Override
    public String getSourceDescription() {
        // TODO: implement
        return null;
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
    public void close() throws IOException {
        // TODO: catch the exceptions and re-throw, but try to close both
        first.close();
        second.close();
    }

    @Override
    public Iterator<GATKRead> query(SimpleInterval interval) {
        // TODO: this requires a merging iterator, no?
        return null;
    }
}
