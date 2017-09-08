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

package org.magicdgs.readtools.tools.snpable;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import scala.Tuple2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class Snpable {

    // cannot be instantiated
    private Snpable() { }

    private static final Logger logger = LogManager.getLogger(Snpable.class);

    private static final String CHROM_POS_SEPARATOR = "_";
    private static final Pattern CHROM_POS_PATTERN = Pattern.compile(CHROM_POS_SEPARATOR);

    private static final int[] conv = initConversionTable();

    /** Description of the SNPable input BAM file. */
    public static final String SNPABLE_INPUT_BAM_DESC = "Input SAM/BAM/CRAM file obtained with MapReferenceKmers (unsorted) or following the SNPable pipeline";

    /** Argument name for the read length parameter. */
    public static final String READ_LENGTH_ARGUMENT_NAME = "readLength";
    /** Argument description for the read length parameter*/
    public static final String READ_LENGTH_ARGUMENT_DESC = "Length of each overlapping k-mer sub-sequence to generate";

    /** Argument name for the raw mask output. */
    public static final String RAW_MASK_OUTPUT_NAME = "rawMaskOutput";
    /** Argument description for the raw mask output. */
    public static final String RAW_MASK_OUTPUT_DESC = "Output file to output the raw mask file";

    /** Argument name for the raw mask output. */
    public static final String MASK_OUTPUT_NAME = "maskOutput";
    /** Argument description for the raw mask output. */
    public static final String MASK_OUTPUT_DESC = "Output file to output the mask file";

    // only called once
    private static int[] initConversionTable() {
        final int[] conv = new int[127];
        // initialize the conversion table
        conv[0] = 0;
        for (int i = 1; i < conv.length; i++) {
            conv[i] = int_log2(i) + 1;
        }
        return conv;
    }

    private static int int_log2(int v) {
        int c = 0;
        if ((v & 0xffff0000) != 0) {
            v >>= 16;
            c |= 16;
        }
        if ((v & 0xff00) != 0) {
            v >>= 8;
            c |= 8;
        }
        if ((v & 0xf0) != 0) {
            v >>= 4;
            c |= 4;
        }
        if ((v & 0xc) != 0) {
            v >>= 2;
            c |= 2;
        }
        if ((v & 0x2) != 0) {
            c |= 1;
        }
        return c;
    }

    public static final SimpleInterval getIntervalFromReadName(final String readName,
            final int readLength) {
        final String[] chromPos = CHROM_POS_PATTERN.split(readName);
        if (chromPos.length != 2) {
            throw new UserException("");
        }
        // TODO: catch exception
        final int start = Integer.parseInt(chromPos[1]);
        return new SimpleInterval(chromPos[0], start, start + readLength);
    }

    public static final String getReadName(final SimpleInterval interval) {
        return String.join(CHROM_POS_SEPARATOR, interval.getContig(),
                String.valueOf(interval.getStart()));
    }

    public static Map<SAMSequenceRecord, List<Tuple2<Integer, Integer>>> computeRawMask(
            final SamReader reader, final int readLength,
            // TODO: if null, omit the encoding
            final PrintStream outputStream) {

        // assumes that it has a sequence dictionary
        final SAMSequenceDictionary dictionary = reader.getFileHeader().getSequenceDictionary();

        final Map<SAMSequenceRecord, List<Tuple2<Integer, Integer>>> rawMask = dictionary
                .getSequences().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        seq -> new ArrayList<>(seq.getSequenceLength() - readLength + 1)
                ));

        final Iterator<SAMSequenceRecord> seqNames = dictionary.getSequences().iterator();
        SAMSequenceRecord lastContig = seqNames.next();

        // some logging
        final ProgressMeter progress = new ProgressMeter();
        logger.info("Computing raw mask");
        progress.setRecordLabel(readLength + "-mers");
        progress.start();

        int c = 0;
        for (final SAMRecord record : reader) {
            final SimpleInterval refInterval = Snpable
                    .getIntervalFromReadName(record.getReadName(), readLength);
            while (!lastContig.getSequenceName().equals(refInterval.getContig())) {
                lastContig = seqNames.next();
                c = 0;
            }
            // quality check
            if (c + 1 != refInterval.getStart()) {
                // TODO: better message
                throw new UserException("Malformed input: " + reader.getResourceDescription());
            } else if (lastContig.getSequenceLength() < refInterval.getEnd()) {
                // TODO: better message
                throw new UserException("Malformed input: " + reader.getResourceDescription());
            }
            // print header
            if (c == 0) {
                logger.info("Starting {}", lastContig::getSequenceName);
                if (outputStream != null) {
                    outputStream.printf(">%s", lastContig.getSequenceName());
                }
            }

            final int a0 = getIntAttributeWithinRange(record, "X0");
            final int a1 = getIntAttributeWithinRange(record, "X1");

            // no need to encode in case of no output
            if (outputStream != null) {
                final char toPrint = (char) (63 + (conv[a0] << 3 | conv[a1]));
                if (c % 60 == 0) {
                    outputStream.println();
                }
                outputStream.print(toPrint);
            }

            rawMask.get(lastContig).add(Tuple2.apply(a0, a1));
            c++;
            progress.update(refInterval);
        }

        if (outputStream != null) {
            outputStream.println();
        }

        progress.stop();

        return rawMask;
    }


    private static int getIntAttributeWithinRange(final SAMRecord record, final String name) {
        final Integer val = record.getIntegerAttribute(name);
        if (val == null) {
            return 0;
        } else if (val > 127) {
            return 127;
        } else {
            return val;
        }
    }
}
