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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.readtools.utils.fastq.BarcodeMethods;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.fastq.FastqRecord;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Argument collection for solve issues of barcodes in the read name not with the normal encoding.
 *
 * Note: this is a temporary solution and should be use with caution.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadNameBarcodeArgumentCollection implements ArgumentCollectionDefinition {
    private static final long serialVersionUID = 1L;

    public static final String READNAME_ENCODING_NAME = "readNameEncoding";

    @Argument(fullName = READNAME_ENCODING_NAME, optional = true, doc = "Encoding for the read name.")
    public ReadNameEncoding readNameEncoding = ReadNameEncoding.ILLUMINA;

    /** Normalize the record name using the provided encoding. */
    public FastqRecord normalizeRecordName(final FastqRecord record) {
        return new FastqRecord(
                readNameEncoding.normalizeReadName(record.getReadHeader()), record.getReadString(),
                record.getBaseQualityHeader(), record.getBaseQualityString());
    }

    /** Normalize the record name using the provided encoding. */
    public FastqPairedRecord normalizeRecordName(final FastqPairedRecord record) {
        return new FastqPairedRecord(normalizeRecordName(record.getRecord1()),
                normalizeRecordName(record.getRecord2()));
    }

    public static enum ReadNameEncoding {
        ILLUMINA("([^#]+)#([^/]+)/?([012]?).?", 3, 2),
        CASAVA("([\\S]+)\\s+([012]):[YN]:[0-9]+:([ATCGN]+).?", 2, 3);

        private final Pattern pattern;
        private final int pairInfoGroup;
        private final int barcodeGroup;

        ReadNameEncoding(final String pattern, final int pairInfoGroup, final int barcodeGroup) {
            this.pattern = Pattern.compile(pattern);
            this.pairInfoGroup = pairInfoGroup;
            this.barcodeGroup = barcodeGroup;
        }

        /**
         * Normalize the read name.
         */
        @VisibleForTesting
        String normalizeReadName(final String readName) {
            final Matcher matcher = pattern.matcher(readName);
            if (!matcher.find()) {
                throw new UserException.BadInput(
                        "Wrongly encoded read name in " + name() + " format: " + readName);
            }
            final StringBuilder normalizedName = new StringBuilder(matcher.group(1));
            normalizedName.append(BarcodeMethods.NAME_BARCODE_SEPARATOR);
            normalizedName.append(matcher.group(barcodeGroup));
            // TODO: add the /0 for single end?
            if (matcher.groupCount() >= pairInfoGroup) {
                normalizedName.append(BarcodeMethods.READ_PAIR_SEPARATOR)
                        .append(matcher.group(pairInfoGroup));
            }
            return normalizedName.toString();
        }

    }

}
