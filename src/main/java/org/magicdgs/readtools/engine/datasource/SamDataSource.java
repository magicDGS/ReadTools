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

package org.magicdgs.readtools.engine.datasource;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.utils.iterators.paired.GATKReadPairedIterator;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.iterators.SAMRecordToReadIterator;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.nio.file.Path;
import java.util.Iterator;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class SamDataSource extends RTDataSource {

    // add constructor params
    private final Path path;
    private final boolean interleaved;
    private final FastqQualityFormat format;
    private final SamReader reader;

    public SamDataSource(String source, boolean interleaved) {
        this.path = IOUtils.getPath(source);
        this.interleaved = interleaved;
        // TODO: close reader
        this.format = QualityEncodingDetector.detect(RTDefaults.MAX_RECORDS_FOR_QUALITY,
                readerFactory.openSamReader(path));
        this.reader = readerFactory.openSamReader(path);
    }

    @Override
    public boolean isPaired() {
        return interleaved;
    }

    @Override
    public FastqQualityFormat sourceEncoding() {
        return format;
    }

    @Override
    public SAMFileHeader getHeader() {
        return reader.getFileHeader();
    }

    @Override
    public boolean isSource(String source) {
        return ReadToolsIOFormat.isSamBamOrCram(source);
    }

    @Override
    protected Iterator<GATKRead> sourceIterator() {
        return new SAMRecordToReadIterator(reader.iterator());
    }

    @Override
    protected Iterator<Tuple2<GATKRead, GATKRead>> sourcePairedIterator() {
        if (!isPaired()) {
            throw new IllegalStateException("TODO: msg");
        }
        return GATKReadPairedIterator.of(sourceIterator());
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
