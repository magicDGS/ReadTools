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
import org.magicdgs.readtools.engine.sourcehandler.ReadsSourceHandler;
import org.magicdgs.readtools.utils.iterators.InterleaveGATKReadIterators;
import org.magicdgs.readtools.utils.iterators.ReadTransformerIterator;
import org.magicdgs.readtools.utils.iterators.paired.GATKReadPairedIterator;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;
import org.magicdgs.readtools.utils.read.transformer.CheckQualityReadTransformer;
import org.magicdgs.readtools.utils.read.transformer.SolexaToSangerReadTransformer;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamFileHeaderMerger;
import htsjdk.samtools.util.FastqQualityFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.engine.GATKDataSource;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.transformers.MisencodedBaseQualityReadTransformer;
import org.broadinstitute.hellbender.transformers.ReadTransformer;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * ReadTools abstract implementation of {@link GATKDataSource} for {@link GATKRead}, which includes:
 *
 * - Tracking the original quality encoding of the data source.
 * - Iterates over reads already in standard format.
 *
 * WARNING: query is not working yet.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class RTDataSource implements GATKDataSource<GATKRead>, AutoCloseable {

    /** Logger for the class. */
    protected final Logger logger = LogManager.getLogger(this);

    /** Factory for creating readers. */
    protected static ReadReaderFactory readerFactory = new ReadReaderFactory();

    // TODO: add as argument to constructor
    private final FastqQualityFormat forceEncoding = null;

    @Override
    public Iterator<GATKRead> query(SimpleInterval interval) {
        throw new UnsupportedOperationException("TODO: msg");
    }

    public abstract boolean isPaired();

    public abstract FastqQualityFormat sourceEncoding();

    public abstract SAMFileHeader getHeader();

    public abstract boolean isSource(final String source);

    protected abstract Iterator<GATKRead> sourceIterator();

    protected abstract Iterator<Tuple2<GATKRead, GATKRead>> sourcePairedIterator();

    @Override
    public Iterator<GATKRead> iterator() {
        return new ReadTransformerIterator(sourceIterator(), qualityTransformer());
    }

    public Iterator<Tuple2<GATKRead, GATKRead>> pairedIterator() {
        // TODO: paired-iterator with transformer
        return null;
    }

    // gets the quality transformer for normalization
    private ReadTransformer qualityTransformer() {
        switch (sourceEncoding()) {
            case Standard:
                // TODO: port to GATK MisencodedBaseQualityReadTransformer
                return new CheckQualityReadTransformer();
            case Illumina:
                return new MisencodedBaseQualityReadTransformer();
            case Solexa:
                return new SolexaToSangerReadTransformer();
            default:
                throw new GATKException(
                        "Unknown quality encoding: " + sourceEncoding());
        }
    }
}
