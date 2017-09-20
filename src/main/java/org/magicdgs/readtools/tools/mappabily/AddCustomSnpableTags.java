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

import org.magicdgs.readtools.cmd.argumentcollections.RTOutputBamArgumentCollection;
import org.magicdgs.readtools.cmd.programgroups.MappabilityProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsProgram;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.engine.ReadsDataSource;
import org.broadinstitute.hellbender.utils.LoggingUtils;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@BetaFeature
@CommandLineProgramProperties(oneLineSummary = "Add X0/X1 tags to a SAM/BAM/CRAM files from the XA and SA tags",
        summary = "",
        programGroup = MappabilityProgramGroup.class)
// TODO: this should be omitted from the CLI for now
public class AddCustomSnpableTags extends ReadToolsProgram {

    @Argument(fullName = StandardArgumentDefinitions.INPUT_LONG_NAME, shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME, doc = "BAM/SAM/CRAM/FASTQ source of reads.", common = true, optional = false)
    public String inputSource;

    // forced to be a BAM argument collection because it is a result of mapping
    @ArgumentCollection
    public RTOutputBamArgumentCollection output = new RTOutputBamArgumentCollection();

    @Argument(fullName = "include-sa-tag", doc = "Include supplementary alignments in the SA tag for compute perfect (X0) and 1-mismatch (X1) hits")
    public boolean includeSA = true;

    @Argument(fullName = "include-xa-tag", doc = "Include extra alignments in the XA tag (bwa) for compute perfect (X0) and 1-mismatch (X1) hits")
    public boolean includeXA = true;

    @Advanced
    @Argument(fullName = "perfect-hit-mismatches", doc = "Number of mismatches allowed to consider an alignment a perfect hit (exclusive). "
            + "Alignments with this exact number of mismatches will be counted in the X1 tag; if they have less, it will be counted in the X0 tag.")
    public int perfectHitMismatches = 1;

    ReadsDataSource reads;
    GATKReadWriter writer;

    @Override
    protected String[] customCommandLineValidation() {
        if (!(includeXA && includeSA)) {
            // TODO: depends on the name of the parameters
            throw new CommandLineException(
                    "At least one of the tags with extra-alignment information should be included (--include-sa-tag or --include-xa-tag)");
        }

        return super.customCommandLineValidation();
    }

    @Override
    protected void onStartup() {
        reads = new ReadsDataSource(IOUtils.getPath(inputSource));
        final SAMFileHeader header = reads.getHeader();
        // TODO: we need the reference for output CRAM
        writer = output.outputWriter(header, () -> this.getProgramRecord(header), true, null);
    }

    @Override
    protected Object doWork() {

        // set progress meter
        final ProgressMeter progressMeter = new ProgressMeter();
        progressMeter.setRecordLabel("reads");
        progressMeter.start();

        // iterate over each read and accumulate the mismatches in the X0/X1 tags
        reads.iterator().forEachRemaining(read -> {
            // only set for mapped reads
            if (!read.isUnmapped()) {
                // TODO: add to documentation with the difference from bwa-aln
                // TODO: adding this tags should be a differnt tool - the main idea is to have a common tool
                // TODO: to generate the reference mapped BAM, and then use different tools to compute
                // TODO: different mappabilities/alignabilities - grabbing the required information from
                // TODO: XA and SA tags
                // custom tags got from the SA alignments and XA tag
                // X0:i:[0-9]* - number of SA/XA alignments with 0 mismatches (perfect match)
                // X1:i:[0-9]* - number of SA/XA alignments with 1 mismatch
                final AtomicInteger x0 = new AtomicInteger(0);
                final AtomicInteger x1 = new AtomicInteger(0);
                // TODO: in some example what I found is that this produces an '@' symbol in downstream scripts
                // TODO: which is bad because the parser for FASTX files considers the '@' symbol as a header
                // TODO: thus, I cap to 3 to get rid of the '@' symbol
                // TODO: we should remove this cap and/or add a command-line-option
                accumulateMismatchesFromXA(read, x0, x1);
                accumulateMismatchesFromSA(read, x0, x1);
                read.setAttribute("X0", x0.intValue());
                read.setAttribute("X1", x1.intValue());
            }
            writer.addRead(read);
            progressMeter.update(read);
        });

        progressMeter.stop();

        return null;
    }

    private void accumulateMismatchesFromSA(final GATKRead read, final AtomicInteger x0,
            final AtomicInteger x1) {
        if (includeSA) {
            final String saTag = read.getAttributeAsString("SA");
            if (saTag == null || saTag.isEmpty()) {
                return;
            }
            // TODO: pre-compile!
            // SA tag pattern: (rname,pos,strand,CIGAR,mapQ,NM;)+
            final String[] alignments = saTag.split(";");
            for (final String al : alignments) {
                final int nm = Integer.valueOf(al.split(",")[5]);
                accumulateMismatches(nm, x0, x1);
            }
        }
    }

    private void accumulateMismatchesFromXA(final GATKRead read, final AtomicInteger x0,
            final AtomicInteger x1) {
        if (includeXA) {
            final String xaTag = read.getAttributeAsString("XA");
            if (xaTag == null || xaTag.isEmpty()) {
                return;
            }

            // TODO: pre-compile!
            final String[] alignments = xaTag.split(";");

            for (final String al : alignments) {
                final int nm = Integer.valueOf(al.split(",")[3]);
                accumulateMismatches(nm, x0, x1);
            }
        }
    }

    private void accumulateMismatches(final int nMismatches, final AtomicInteger x0,
            final AtomicInteger x1) {
        if (nMismatches < perfectHitMismatches) {
            x0.incrementAndGet();
        } else if (nMismatches == perfectHitMismatches) {
            x1.incrementAndGet();
        }
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(reads);
        CloserUtil.close(writer);
    }
}
