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

package org.magicdgs.readtools.engine.sources.sam;

import org.magicdgs.readtools.engine.sources.RTAbstractReadsSource;
import org.magicdgs.readtools.engine.sources.RTReadsSource;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.iterators.SAMRecordToReadIterator;
import org.broadinstitute.hellbender.utils.iterators.SamReaderQueryingIterator;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class SamReadsSource extends RTAbstractReadsSource {

    private final SamReaderFactory samFactory = SamReaderFactory.makeDefault();

    private final String source;
    private final boolean interleaved;

    private SamReader reader = null;

    public SamReadsSource(final String source, final boolean interleaved) {
        this.source = source;
        this.interleaved = interleaved;
    }

    private SamReader openReader() {
        return samFactory.open(IOUtils.getPath(source));
    }

    private void setReader() {
        if (reader == null) {
            reader = openReader();
        }
    }

    @Override
    public SAMFileHeader getHeader() {
        try(final SamReader reader = openReader()) {
            return reader.getFileHeader();
        } catch (IOException e) {
            throw new UserException.CouldNotReadInputFile(source, e);
        }
    }

    @Override
    protected FastqQualityFormat detectEncoding(long maxNumberOfReads) {
        try(final SamReader reader = openReader()) {
            return QualityEncodingDetector.detect(maxNumberOfReads, reader);
        } catch (IOException e) {
            throw new UserException.CouldNotReadInputFile(source, e);
        }
    }

    @Override
    protected Iterator<GATKRead> rawIterator() {
        setReader();
        return new SAMRecordToReadIterator(reader.iterator());
    }

    @Override
    public boolean isPaired() {
        return interleaved;
    }

    @Override
    public Iterator<GATKRead> query(final List<SimpleInterval> locs) {
        try {
            setReader();
            return standardizeEncodingIterator(new SAMRecordToReadIterator(new SamReaderQueryingIterator(reader, locs, false)));
        } catch (SAMException | UserException e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        }
    }

    @Override
    public String getSourceDescription() {
        return (isPaired())
                ? "SAM paired interleaved (" + source + ")"
                : "SAM (" + source + ")";
    }

    @Override
    public RTReadsSource setValidationStringency(ValidationStringency stringency) {
        samFactory.validationStringency(stringency);
        return this;
    }

    @Override
    public RTReadsSource setUseAsyncIo(boolean useAsyncIo) {
        samFactory.setUseAsyncIo(useAsyncIo);
        return this;
    }

    @Override
    public RTReadsSource setReferenceSequence(File referenceFile) {
        samFactory.referenceSequence(referenceFile);
        return this;
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }
}
