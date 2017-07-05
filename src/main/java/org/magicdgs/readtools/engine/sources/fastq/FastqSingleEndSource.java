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
import org.magicdgs.readtools.utils.iterators.RecordToReadIterator;
import org.magicdgs.readtools.utils.fastq.FastqGATKRead;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.QualityEncodingDetector;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class FastqSingleEndSource extends RTAbstractReadsSource {

    protected final String source;

    private FastqReader reader = null;

    public FastqSingleEndSource(final String source) {
        this.source = source;
    }

    @Override
    public String getSourceDescription() {
        return "Single-end FASTQ (" + source + ")";
    }

    @Override
    public FastqSingleEndSource setValidationStringency(final ValidationStringency stringency) {
        logger.debug("Validation stringency option is ignored for FASTQ sources");
        return this;
    }

    @Override
    public FastqSingleEndSource setUseAsyncIo(final boolean useAsyncIo) {
        logger.debug("Asynchronous reading option is ignored for FASTQ sources");
        return this;
    }

    @Override
    public FastqSingleEndSource setReferenceSequence(final File referenceFile) {
        logger.debug("Reference sequence option is ignored for FASTQ sources");
        return this;
    }

    private FastqReader openReader() {
        // TODO: catch exceptions here
        return new FastqReader(IOUtil.openFileForBufferedReading(IOUtils.getPath(source)));
    }

    /** Returns an empty header, with {@link SAMFileHeader.SortOrder#unknown} sort order. */
    @Override
    public SAMFileHeader getHeader() {
        final SAMFileHeader header = new SAMFileHeader();
        header.setSortOrder(SAMFileHeader.SortOrder.unknown);
        return header;
    }

    @Override
    protected FastqQualityFormat detectEncoding(long maxNumberOfReads) {
        // try with resources with auto-closing
        try(final FastqReader reader = openReader()) {
            return QualityEncodingDetector.detect(maxNumberOfReads, reader);
        } catch (SAMException e) {
            throw new UserException.CouldNotReadInputFile(source, e);
        }
    }

    @Override
    protected Iterator<GATKRead> rawIterator() {
        if (reader != null) {
            throw new IllegalStateException(getSourceDescription() + ": should be close before starting a new iteration");
        }
        // TODO: the FASTQ readeer is not the best - maybe we should implement our own reader
        reader = openReader();
        return new RecordToReadIterator<>(reader.iterator(), FastqGATKRead::new);
    }

    @Override
    public boolean isPaired() {
        return false;
    }

    @Override
    public Iterator<GATKRead> query(List<SimpleInterval> locs) {
        throw new UnsupportedOperationException(getSourceDescription() + ": cannot query FASTQ file");
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }
}
