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

package org.magicdgs.readtools.tools.mappabily;

import org.magicdgs.readtools.cmd.programgroups.MappabilityProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsProgram;
import org.magicdgs.readtools.utils.read.ReadReaderFactory;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import scala.Tuple2;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@BetaFeature
@CommandLineProgramProperties(oneLineSummary = "",
        summary = "",
        programGroup = MappabilityProgramGroup.class)
public class GenerateMask extends ReadToolsProgram {

    // TODO: better doc
    @Argument(fullName = StandardArgumentDefinitions.INPUT_LONG_NAME, shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME, doc = Snpable.SNPABLE_INPUT_BAM_DESC)
    public String inputPath;

    // TODO: better doc and make hidden/advance for debugging

    @Argument(fullName = Snpable.READ_LENGTH_ARGUMENT_NAME, doc = Snpable.READ_LENGTH_ARGUMENT_DESC, minValue = 1)
    public Integer readLength;

    @Argument(fullName = Snpable.MASK_OUTPUT_NAME, doc = Snpable.MASK_OUTPUT_DESC)
    public String outputMask = null;

    @Argument(fullName = Snpable.RAW_MASK_OUTPUT_NAME, doc = Snpable.RAW_MASK_OUTPUT_DESC + "(if not provided, no raw mask will be output)", optional = true)
    public String outputRawMask = null;

    // TODO: better doc
    @Argument(fullName = "stringency")
    public Double stringency;


    private SamReader reader;
    private PrintStream maskOutputStream;

    // optional output
    private PrintStream rawMaskStream = null;

    @Override
    protected void onStartup() {
        try {
            // input path
            reader = new ReadReaderFactory().openSamReader(IOUtils.getPath(inputPath));
            if (outputRawMask != null) {
                rawMaskStream = new PrintStream(Files.newOutputStream(IOUtils.getPath(outputRawMask)));
            }
            maskOutputStream = new PrintStream(Files.newOutputStream(IOUtils.getPath(outputMask)));
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }

    }

    @Override
    protected Object doWork() {
        // assumes that it has a sequence dictionary
        final SAMSequenceDictionary dictionary = reader.getFileHeader().getSequenceDictionary();
        // compute and output the rawMask
        final Map<SAMSequenceRecord, List<Tuple2<Integer, Integer>>> rawMask = Snpable
                .computeRawMask(reader, readLength, rawMaskStream);
        // compute the mask and output it
        computeMask(dictionary, rawMask);
        return null;
    }

    private void computeMask(final SAMSequenceDictionary dictionary,
            final Map<SAMSequenceRecord, List<Tuple2<Integer, Integer>>> rawMask) {
        final long[] cnt = new long[5];
        for (final SAMSequenceRecord seq : dictionary.getSequences()) {
            int n_good = 0, n_all = 0, n_mid = 0;
            maskOutputStream.printf(">%s %d %.3f", seq.getSequenceName(), readLength, stringency);

            final List<Tuple2<Integer, Integer>> seqRawMask = rawMask.get(seq);

            logger.debug("{}: length={}, rawMaskLength={}",
                    seq::getSequenceName,
                    seq::getSequenceLength,
                    seqRawMask::size);

            for (int i = 0; i < seq.getSequenceLength(); ++i) {
                int c1, c2;
                if (i < seqRawMask.size()) {
                    final Tuple2<Integer, Integer> mask = seqRawMask.get(i);
                    c1 = mask._1;
                    c2 = mask._2;
                } else {
                    c1 = 0;
                    c2 = 0;
                }
                if (c1 == 1) {
                    ++cnt[4];
                }
                if (c1 != 0) {
                    ++n_all;
                    if (is_good(c1, c2)) {
                        ++n_good;
                    }
                    if (c1 == 1) {
                        ++n_mid; // this means that X0:i:1 - only one best hit (perfect hit), but it may contain mismatches
                    }
                }
                if (i >= readLength && i < seqRawMask.size()) {
                    final Tuple2<Integer, Integer> mask = seqRawMask.get(i - readLength);
                    c1 = mask._1;
                    c2 = mask._2;
                } else {
                    c1 = 0;
                    c2 = 0;
                }
                if (c1 != 0) {
                    --n_all;
                    if (is_good(c1, c2)) {
                        --n_good;
                    }
                    if (c1 == 1) {
                        --n_mid;
                    }
                }
                // assertion in c++, reverse here
                if (!(n_all <= readLength && n_good <= n_all)) {
                    throw new IllegalStateException(String.format(
                            "n_all=%s, readLength=%s, n_good=%s: " +
                                    "n_all <= readLength (%s) &&  n_good <= n_all (%s)",
                            n_all, readLength, n_good,
                            n_all <= readLength,
                            n_good <= n_all));
                }
                if (i % 60 == 0) {
                    maskOutputStream.println();
                }
                final int x = n_all == 0 ? 0 : (double) n_good / n_all >= stringency ? 3
                        : (double) n_mid / n_all >= stringency ? 2 : 1;
                maskOutputStream.print(x);
                cnt[x]++;
            }
            maskOutputStream.println();
        }

        final long tot = cnt[1] + cnt[2] + cnt[3];

        logger.info("{}, {}, {}, {}, {}", cnt[0], cnt[1], cnt[2], cnt[3], cnt[4]);
        // using String.format instead of logger placeholder to format the doubles
        logger.info(() -> String.format("%.6f, %.6f, %.6f",
                (double) cnt[3] / tot,
                (double) (cnt[2] + cnt[3]) / tot,
                (double) cnt[4] / tot));
    }

    private static boolean is_good(int c1, int c2) {
        // this means X0:i:1 and X1:i:0
        // X0:i:1 -> only one best hit (perfect hit)
        // X1:i:0 -> no sub-optimal hit (the perfect hit has 0 mismatches)
        return (c1 == 1 && c2 == 0);
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(reader);
        CloserUtil.close(rawMaskStream);
        CloserUtil.close(maskOutputStream);
    }
}
