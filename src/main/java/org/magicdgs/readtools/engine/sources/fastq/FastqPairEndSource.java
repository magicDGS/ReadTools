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

package org.magicdgs.readtools.engine.sources.fastq;

import org.magicdgs.readtools.engine.sources.RTAbstractReadsSource;
import org.magicdgs.readtools.engine.sources.RTReadsSource;
import org.magicdgs.readtools.utils.iterators.InterleaveGATKReadIterators;
import org.magicdgs.readtools.utils.iterators.paired.GATKReadPairedIterator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.FastqQualityFormat;
import org.broadinstitute.barclay.utils.Utils;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class FastqPairEndSource extends RTAbstractReadsSource {

    private final FastqSingleEndSource first;

    private final Optional<FastqSingleEndSource> second;


    public FastqPairEndSource(final String source) {
        this(new FastqSingleEndSource(source), null);
    }

    public FastqPairEndSource(final String first, final String second) {
        this(new FastqSingleEndSource(first), new FastqSingleEndSource(second));
    }

    // private constructor
    private FastqPairEndSource(final FastqSingleEndSource first,
            final FastqSingleEndSource second) {
        this.first = Utils.nonNull(first);
        this.second = (second == null) ? Optional.empty() : Optional.of(second);
    }


    @Override
    public FastqQualityFormat detectEncoding(long maxNumberOfReads) {
        final FastqQualityFormat format1 = first.getQualityEncoding();
        if (!second.isPresent()) {
            return format1;
        }
        final FastqQualityFormat format2 = second.get().getQualityEncoding();
        if (format2.equals(format1)) {
            return format1;
        }
        throw new UserException(String.format("%s: different encoding found (%s vs. %s)",
                getSourceDescription(), format1, format2));
    }

    @Override
    public SAMFileHeader getHeader() {
        return first.getHeader();
    }

    @Override
    protected Iterator<GATKRead> rawIterator() {
        return (second.isPresent())
                ? new InterleaveGATKReadIterators(first.iterator(), second.get().iterator())
                : first.iterator();
    }

    @Override
    public boolean isPaired() {
        return true;
    }

    @Override
    public Iterator<Tuple2<GATKRead, GATKRead>> getPairedIterator() {
        // already transforming
        return (second.isPresent())
                ? GATKReadPairedIterator.of(first.iterator(), second.get().iterator())
                : GATKReadPairedIterator.of(first.iterator());
    }

    @Override
    public Iterator<GATKRead> query(List<SimpleInterval> locs) {
        return (second.isPresent())
                ? new InterleaveGATKReadIterators(first.query(locs),
                second.get().query(locs))
                : first.query(locs);
    }

    @Override
    public String getSourceDescription() {
        return (second.isPresent())
                ? "Pair-end FASTQ (" + first.source + "," + second.get().source + ")"
                : "Interleaved paired-end FASTQ (" + first.source + ")";
    }

    @Override
    public RTReadsSource setValidationStringency(final ValidationStringency stringency) {
        first.setValidationStringency(stringency);
        second.ifPresent(s -> s.setValidationStringency(stringency));
        return this;
    }

    @Override
    public RTReadsSource setUseAsyncIo(final boolean useAsyncIo) {
        first.setUseAsyncIo(useAsyncIo);
        second.ifPresent(s -> s.setUseAsyncIo(useAsyncIo));
        return this;
    }

    @Override
    public RTReadsSource setReferenceSequence(final File referenceFile) {
        first.setReferenceSequence(referenceFile);
        second.ifPresent(s -> s.setReferenceSequence(referenceFile));
        return this;
    }

    @Override
    public void close() throws IOException {
        first.close();
        if (second.isPresent()) {
            second.get().close();
        }
    }
}
