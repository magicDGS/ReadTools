/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

package org.magicdgs.readtools.utils.read.writer;

import htsjdk.samtools.util.BinaryCodec;
import htsjdk.samtools.util.IOUtil;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.BaseUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.io.File;
import java.io.IOException;

/**
 * Basic writer for GATKRead to output a BFQ files, as described in
 * <a href=http://ngsutils.org/modules/fastqutils/bfq/>fastqutils</a>.
 *
 * Note: all the reads passed to this writer will be encoded from the values returned from the
 * following methods:
 * - {@link GATKRead#getName()} for the name of the read, without pair information.
 * - {@link GATKRead#getLength()} for the int used in BFQ to encode the length of the read.
 * - {@link GATKRead#getBases()} and {@link GATKRead#getBaseQualities()} for the read base/quality.
 *
 * Thus, reads should be filtered before add them to the writer to avoid wierd behaviour.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BfqGATKWriter implements GATKReadWriter {

    private static final int SEED_REGION_LENGTH = 28;
    private static final int MAX_SEED_REGION_NOCALL_FIXES = 2;

    private final BinaryCodec codec;

    // TODO: change for use Path
    public BfqGATKWriter(final File bfq) {
        // TODO: this will require more stuff, like in FastqGATKWriter
        codec = new BinaryCodec(IOUtil.openFileForWriting(bfq));
    }

    @Override
    public void addRead(GATKRead read) {
        // Writes the length of the read name and then the name (null-terminated)
        // TODO: should the read name include the pair information
        codec.writeString(read.getName(), true, true);
        // this assume that getLength/getBases/getBaseQualities have the same length
        codec.writeInt(read.getLength());
        codec.writeBytes(encodeSeqsAndQuals(read));
    }

    /**
     * Adapted from Picard <a href=https://github.com/broadinstitute/picard/blob/master/src/main/java/picard/fastq/BamToBfqWriter.java>BamToBfqWriter</a>.
     *
     * Differences with respect to the adapted method are:
     * - The whole read is retained, without applying any trimming or clip removal.
     * - Read bases are retrieved from the read and transformed to int values with {@link
     * BaseUtils}
     *
     * This implies that trimming/clip removal should be perform before adding a read to this
     * writer.
     */
    private byte[] encodeSeqsAndQuals(final GATKRead read) {
        final byte[] seqsAndQuals = new byte[read.getLength()];
        final char[] quals = ReadUtils.getBaseQualityString(read).toCharArray();
        int seedRegionNoCallFixes = 0;
        for (int i = 0; i < read.getLength(); i++) {
            // TODO: checks if this could use the qualities from the read
            int quality = Math.min(quals[i] - 33, 63);
            // TODO: check if this is giving the same result
            final byte readBase = read.getBase(i);
            final int base;
            if (BaseUtils.isNBase(readBase)) {
                // N base is 0
                base = 0;
                // I think this is an algorithm to fix the N bases
                if (i < SEED_REGION_LENGTH) {
                    if (seedRegionNoCallFixes < MAX_SEED_REGION_NOCALL_FIXES) {
                        quality = 1;
                        seedRegionNoCallFixes++;
                    } else {
                        quality = 0;
                    }
                } else {
                    quality = 1;
                }
            } else {
                base = BaseUtils.simpleBaseToBaseIndex(readBase);
                if (base == -1) {
                    throw new GATKException("Unknown base when writing bfq file: " + readBase);
                }
            }
            seqsAndQuals[i] = encodeBaseAndQuality(base, quality);
        }
        return seqsAndQuals;
    }

    /**
     * Copied from Picard <a href=https://github.com/broadinstitute/picard/blob/master/src/main/java/picard/fastq/BamToBfqWriter.java>BamToBfqWriter</a>.
     */
    private byte encodeBaseAndQuality(int base, int quality) {
        return (byte) ((base << 6) | quality);
    }

    @Override
    public void close() throws IOException {
        codec.close();
    }
}
