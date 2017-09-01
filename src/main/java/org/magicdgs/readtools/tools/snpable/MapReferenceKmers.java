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

import org.magicdgs.readtools.cmd.argumentcollections.BwaMemArgumentCollection;
import org.magicdgs.readtools.cmd.argumentcollections.RTOutputBamArgumentCollection;
import org.magicdgs.readtools.engine.ReadToolsProgram;
import org.magicdgs.readtools.utils.bwa.BwaUtils;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.argumentcollections.IntervalArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.OptionalIntervalArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.ReferenceInputArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.RequiredReferenceInputArgumentCollection;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.ReferenceDataSource;
import org.broadinstitute.hellbender.engine.Shard;
import org.broadinstitute.hellbender.engine.ShardBoundary;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAligner;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAlignment;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAlignmentUtils;
import org.broadinstitute.hellbender.utils.bwa.BwaMemIndex;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.SAMRecordToGATKReadAdapter;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@BetaFeature
@CommandLineProgramProperties(oneLineSummary = "Extracts and maps all overlapping k-mer sub-sequences from a FASTA reference",
        summary = "",
        programGroup = SnpableProgramGroup.class)
// TODO: this should be omitted from the CLI for now
public class MapReferenceKmers extends ReadToolsProgram {

    /** Recommended value for the gap open penalty in the <a href="http://lh3lh3.users.sourceforge.net/snpable.shtml">SNPable</a> page. */
    public static final int RECOMMENDED_GAP_OPEN_PENALTY = 3;

    /** Recommended value for the gap extension penalty in the <a href="http://lh3lh3.users.sourceforge.net/snpable.shtml">SNPable</a> page. */
    public static final int RECOMMENDED_GAP_EXTENSION_PENALTY = 3;

    // TODO: maybe it is too much? - we should set one that it is okay
    private static final int MAXIMUM_XA_HITS_IN_OUTPUT = 1000000;

    @ArgumentCollection
    public ReferenceInputArgumentCollection referenceArgs =
            new RequiredReferenceInputArgumentCollection();

    // useful for specifying only one chromosome to test mappability
    // or a specific region
    @ArgumentCollection
    public IntervalArgumentCollection intervalArgs = new OptionalIntervalArgumentCollection();

    // forced to be a BAM argument collection because it is a result of mapping
    @ArgumentCollection
    public RTOutputBamArgumentCollection output = new RTOutputBamArgumentCollection();

    // this is 35 by default in the SNPable pipeline
    @Argument(fullName = Snpable.READ_LENGTH_ARGUMENT_NAME, doc = Snpable.READ_LENGTH_ARGUMENT_DESC, minValue = 1)
    public Integer readLength;

    @ArgumentCollection
    public BwaMemArgumentCollection bwaArgs = new BwaMemArgumentCollection(null, null,
            Collections.singletonList(RECOMMENDED_GAP_OPEN_PENALTY),
            Collections.singletonList(RECOMMENDED_GAP_EXTENSION_PENALTY),
            Collections.emptyList());

    // reference data source to get overlapping regions
    private ReferenceDataSource reference;
    // the intervals to consider
    private List<SimpleInterval> intervals;

    // aligner
    private BwaMemAligner aligner;
    // output writer
    private SAMFileHeader header;
    private GATKReadWriter writer;


    @Override
    protected void onStartup() {
        // initialize reference source
        reference = ReferenceDataSource.of(referenceArgs.getReferenceFile());
        // initialize the intervals
        intervals = intervalArgs.intervalsSpecified()
                ? intervalArgs.getIntervals(reference.getSequenceDictionary())
                : IntervalUtils.getAllIntervalsForReference(reference.getSequenceDictionary());

        logger.info("Reference sequence will be divided in {} overlapping {}-mers",
                () -> intervals.stream().mapToInt(interval -> interval.size() - readLength + 1).sum(),
                () -> readLength);

        // initialize writer (unsorted as in SNPable)
        header = new SAMFileHeader(reference.getSequenceDictionary());
        writer = output.outputWriter(header, () -> getProgramRecord(header), true,
                referenceArgs.getReferenceFile());

        // initialize aligner
        initializeAligner();
    }


    private void initializeAligner() {
        // open index or generate it
        final String bwaMemIndexImage = BwaUtils
                .getDefaultIndexImageNameFromFastaFile(referenceArgs.getReferenceFileName());
        if (new File(bwaMemIndexImage).exists()) {
            logger.info("Using already generated index image: {}", bwaMemIndexImage);
        } else {
            logger.info("Indexing reference file");
            BwaMemIndex.createIndexImageFromFastaFile(referenceArgs.getReferenceFileName(),
                    bwaMemIndexImage);
            logger.info("Generated index image: {}", bwaMemIndexImage);
        }

        // initialize the aligner with the index file
        aligner = bwaArgs.getNewBwaMemAligner(bwaMemIndexImage);

        // set the maximum XA hits - we will use this ones for extract the raw mask
        // because the aligner does not extract the X0 and/or X1 tags
        // TODO: this is forced, if the BWA-mem args is setting this we should log a warn!!
        aligner.setMaxXAHitsAltOption(MAXIMUM_XA_HITS_IN_OUTPUT);
    }

    @Override
    protected Object doWork() {
        final SAMSequenceDictionary dictionary = header.getSequenceDictionary();

        // get the senquence names
        final List<String> seqNames = dictionary.getSequences().stream()
                .map(SAMSequenceRecord::getSequenceName).collect(Collectors.toList());

        final ProgressMeter progressMeter = new ProgressMeter();
        progressMeter.setRecordLabel(readLength + "-mers");
        progressMeter.start();

        // iterate over each interval
        intervals.stream()
                // divide into shards without padding
                .flatMap(interval -> Shard.divideIntervalIntoShards(interval, readLength, 1, 0, dictionary).stream())
                // convert to simple interval
                .map(ShardBoundary::getInterval)
                // filter out intervals that are not of the requested length
                // this is necessary because the Sard.dividiveIntervalIntoShards generates at the end
                // of the reference slices of shorter size, that we do not require
                .filter(interval -> interval.size() == readLength)
                // map the interval from the reference, returning just primary alignment
                .forEach(interval -> {
                    final GATKRead read = mapReferenceInterval(interval, seqNames);
                    writer.addRead(read);
                    progressMeter.update(interval);
                });

        progressMeter.stop();

        return null;
    }

    // TODO: this should be moved at some point to a engine class
    private GATKRead mapReferenceInterval(final SimpleInterval interval, final List<String> seqNames) {
        final byte[] bases = new ReferenceContext(reference, interval).getBases();
        // TODO: maybe we can cache the results for common kmers in the sequence
        // TODO: this will speed-up stuff - we can either
        // TODO: 1. cache bases -> List<BwaMemAlignment>
        // TODO: 2. cache bases -> GATKRead (only primary alignment with SA tags)
        // TODO: the second case may require to set the name to the current chr_pos
        // TODO: setting a 'cached' name previously
        final List<BwaMemAlignment> alignments =
                aligner.alignSeqs(Collections.singletonList(bases)).get(0);

        if (alignments.size() == 0) {
            throw new GATKException.ShouldNeverReachHereException("No alignment for read?");
        }

        // TODO: add to documentation with the difference from bwa-aln
        // custom tags got from the SA alignments and XA tag
        // X0:i:[0-9]* - number of SA/XA alignments with 0 mismatches (perfect match)
        // X1:i:[0-9]* - number of SA/XA alignments with 1 mismatch
        final AtomicInteger x0 = new AtomicInteger(0);
        final AtomicInteger x1 = new AtomicInteger(0);

        final Map<BwaMemAlignment, String> saTags = BwaMemAlignmentUtils.createSATags(alignments, seqNames);

        final List<GATKRead> reads = alignments.stream()
                // first accumulate statistics of mismatches (X0, X1 and X2)
                .peek(al -> accumulateMismatches(al.getNMismatches(), x0, x1))
                // convert to GATKRead each of the alignments
                .map(al -> {
                    final SAMRecord record = BwaMemAlignmentUtils.applyAlignment(
                            // same name as the SNPable splitfa
                            Snpable.getReadName(interval),
                            bases, null, // no qualities
                            null, // no read group
                            al, seqNames, header,
                            false, false);
                    record.setAttribute(SAMTag.SA.name(), saTags.get(al));
                    return new SAMRecordToGATKReadAdapter(record);
                })
                // filter out the secondary/supplementary alignments
                .filter(BwaUtils.PRIMARY_LINE_FILTER)
                // accumulate the statistics from the XA tag
                .peek(read -> accumulateXaInCustomTags(read, x0, x1))
                // collect the secondary/supplementary alignmets
                .collect(Collectors.toList());

        if (reads.size() > 1) {
            throw new GATKException.ShouldNeverReachHereException("Only 1 primary alignment should be reported");
        }
        // get the read and add the custom tags
        final GATKRead read = reads.get(0);
        // setting custom X0 and X1 tags
        if (!read.isUnmapped()) {
            // TODO: in some example what I found is that thsi produces an '@' symbol in downstream scripts
            // TODO: which is bad because the parser for FASTX files considers the '@' symbol as a header
            // TODO: thus, I cap to 3 to get rid of the '@' symbol
            // TODO: we should remove this cap and/or add a command-line-option
            read.setAttribute("X0", Math.min(x0.intValue(), 3));
            read.setAttribute("X1", Math.min(x1.intValue(), 3));
        }

        return read;
    }

    private void accumulateXaInCustomTags(final GATKRead read, final AtomicInteger x0, final AtomicInteger x1) {
        final String xa = read.getAttributeAsString("XA");
        // no XA tag - no update
        if (xa == null || xa.isEmpty()) {
            return;
        }
        // TODO: pre-compile!
        // XA tag pattern: (chr,pos,CIGAR,NM;)*
        final String[] alignments = xa.split(";");
        for (final String al: alignments) {
            final int nm = Integer.valueOf(al.split(",")[3]);
            accumulateMismatches(nm, x0, x1);
        }
    }


    private static void accumulateMismatches(final int nMismatches, final AtomicInteger x0, final AtomicInteger x1) {
        switch (nMismatches) {
            case 0:
                x0.incrementAndGet();
                break;
            case 1:
                x1.incrementAndGet();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(reference);
        CloserUtil.close(writer);
        CloserUtil.close(aligner);
    }
}
