/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.utils.distmap.encoder;

import org.magicdgs.readtools.utils.read.RTReadUtils;

import com.google.common.annotations.Beta;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;
import scala.Tuple2;

/**
 * Codec for the experimental tabbed-FASTQ format.
 *
 * <p>For single-end data, this format is the same as remove the empty lines within FASTQ records
 * (every 4 lines). For paired-end data, the format is the same but with both records in the same
 * line separated with a tab.
 *
 * <p>This format might be the one used in Distmap 3.0.0.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@Beta
public final class TabFastqCodec implements DistmapCodec {

    /**
     * Singleton instance for the {@link TabFastqCodec}.
     */
    public static final DistmapCodec SINGLETON = new TabFastqCodec();

    // patter for tfq
    // 1. read name
    // 2. sequence bases
    // 3. quality header
    // 4. sequence quality
    private static final String TFQ_PATTERN = "@%s\t%s\t+%s\t%s";

    private TabFastqCodec() {}

    public final String encode(final GATKRead read) {
        // patter for tfq
        // 1. read name (Illumina)
        // 2. sequence bases
        // 3. quality header
        // 4. sequence quality
        return String.format(TFQ_PATTERN,
                RTReadUtils.getIlluminaReadName(read),
                read.getBasesString(),
                "", // TODO: maybe add the quality header if required
                ReadUtils.getBaseQualityString(read)
        );
    }

    public final String encode(final Tuple2<GATKRead, GATKRead> pair) {
        return encode(pair._1) + "\t" + encode(pair._2);
    }

    @Override
    public GATKRead decodeSingle(final String distmapSingleString) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Tuple2<GATKRead, GATKRead> decodePaired(final String distmapPairedString) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isPaired(final String distmapString) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
