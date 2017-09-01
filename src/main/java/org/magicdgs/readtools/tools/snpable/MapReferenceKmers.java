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

import org.magicdgs.readtools.cmd.argumentcollections.RTOutputBamArgumentCollection;
import org.magicdgs.readtools.engine.ReadToolsProgram;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.argumentcollections.ReferenceInputArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.RequiredReferenceInputArgumentCollection;
import org.broadinstitute.hellbender.cmdline.programgroups.BwaMemUtilitiesProgramGroup;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.ReferenceDataSource;
import org.broadinstitute.hellbender.engine.Shard;
import org.broadinstitute.hellbender.engine.ShardBoundary;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAligner;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAlignment;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAlignmentUtils;
import org.broadinstitute.hellbender.utils.bwa.BwaMemIndex;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.read.SAMRecordToGATKReadAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: make DocumentedFeature
@BetaFeature
@CommandLineProgramProperties(oneLineSummary = "Extracts and maps all overlapping k-mer sub-sequences from a FASTA reference",
        summary = "",
        // TODO: this should be a FastaProgramGroup
        programGroup = BwaMemUtilitiesProgramGroup.class)
// TODO: this should be omitted from the CLI for now
public class MapReferenceKmers extends ReadToolsProgram {

    @ArgumentCollection
    public ReferenceInputArgumentCollection referenceArgs =
            new RequiredReferenceInputArgumentCollection();

    // forced to be a BAM argument collection because it is a result of mapping
    @ArgumentCollection
    public RTOutputBamArgumentCollection output = new RTOutputBamArgumentCollection();

    @Argument(fullName = "readLength", doc = "Length of each overlapping k-mer sub-sequence to generate", minValue = 1, minRecommendedValue = 35)
    public Integer readLength;

    /////////////////////////////////////////////////////////
    // ADVANCE OPTIONS FOR BWA-MEM - if null, they use defaults
    // TODO: include more?

    @Advanced
    @Argument(fullName = "matchScore", shortName = "A", doc = "score for a sequence match, which scales options -TdBOELU unless overridden", optional = true)
    public Integer matchScore = null;

    @Advanced
    @Argument(fullName = "mismatchPenalty", shortName = "B", doc = "penalty for a mismatch", optional = true)
    public Integer mismatchPenalty = null;

    @Advanced
    @Argument(fullName = "gapOpenPenalty", shortName = "O", doc = "gap open penalties for deletions and insertions", optional = true, maxElements = 2)
    public List<Integer> gapOpenPenalties = new ArrayList<>();

    @Advanced
    @Argument(fullName = "gapExtensionPenalty", shortName = "E", doc = "gap extension penalty; a gap of size k cost '{-O} + {-E}*k'", optional = true, maxElements = 2)
    public List<Integer> gapExtensionPenalties = new ArrayList<>();

    @Advanced
    @Argument(fullName = "clippingPenalty", shortName = "L", doc = "penalty for 5'- and 3'-end clipping", optional = true, maxElements = 2)
    public List<Integer> clippingPenalties = new ArrayList<>();

    @Override
    protected String[] customCommandLineValidation() {
        // TODO: implement validation
        return super.customCommandLineValidation();
    }

    // reference data source to get overlapping regions
    private ReferenceDataSource reference;
    // aligner
    private BwaMemAligner aligner;
    // output writer
    private SAMFileHeader header;
    private GATKReadWriter writer;


    @Override
    protected void onStartup() {
        // initialize reference source
        reference = ReferenceDataSource.of(referenceArgs.getReferenceFile());
        // initialize aligner
        initializeAligner();

        // initialize writer
        // TODO: maybe set sort order?
        header = new SAMFileHeader(reference.getSequenceDictionary());
        writer = output.outputWriter(header, () -> getProgramRecord(header), true,
                referenceArgs.getReferenceFile());
    }


    private void initializeAligner() {
        // open index or generate it
        final String bwaMemIndexImage =
                getBwaMemIndexImageName(referenceArgs.getReferenceFileName());
        if (new File(bwaMemIndexImage).exists()) {
            logger.info("Using already generated index image: {}", bwaMemIndexImage);
        } else {
            logger.info("Indexing reference file");
            BwaMemIndex.createIndexImageFromFastaFile(referenceArgs.getReferenceFileName(),
                    bwaMemIndexImage);
            logger.info("Generated index image: {}", bwaMemIndexImage);
        }

        // initialize the aligner with the index file
        aligner = new BwaMemAligner(new BwaMemIndex(bwaMemIndexImage));
        // and setting options
        setAlignerOption(matchScore, BwaMemAligner::setMatchScoreOption, "match score");
        setAlignerOption(mismatchPenalty, BwaMemAligner::setMismatchPenaltyOption,
                "mismatch penalty");
        setMaybeTwoOptions(gapOpenPenalties, BwaMemAligner::setDGapOpenPenaltyOption,
                BwaMemAligner::setIGapOpenPenaltyOption, "gap open penalty", "deletion",
                "insertion");
        setMaybeTwoOptions(gapExtensionPenalties, BwaMemAligner::setDGapExtendPenaltyOption,
                BwaMemAligner::setIGapExtendPenaltyOption, "gap extension penalty", "deletion",
                "insertion");
        setMaybeTwoOptions(clippingPenalties, BwaMemAligner::setClip5PenaltyOption,
                BwaMemAligner::setClip3PenaltyOption, "clipping penalty", "5'-end", "3'-end");
    }

    private <T> void setAlignerOption(final T optionValue,
            final BiConsumer<BwaMemAligner, T> setter,
            final String optionName) {
        if (optionValue != null) {
            logger.debug("Setting advance option: {}", optionName);
            setter.accept(aligner, optionValue);
        }
    }

    private <T> void setMaybeTwoOptions(final List<T> optionValues,
            final BiConsumer<BwaMemAligner, T> firstSetter,
            final BiConsumer<BwaMemAligner, T> secondSetter,
            final String optionName,
            final String firstValueName,
            final String secondValueName) {
        if (optionValues != null && !optionValues.isEmpty()) {

            if (optionValues.size() == 1) {
                logger.debug("Setting advance option: {} and {} {} (same value)", firstValueName,
                        secondValueName, optionName);
                firstSetter.accept(aligner, optionValues.get(0));
                secondSetter.accept(aligner, optionValues.get(0));
            } else if (optionValues.size() == 2) {
                setAlignerOption(optionValues.get(0), firstSetter,
                        optionName + " " + firstValueName);
                setAlignerOption(optionValues.get(1), firstSetter,
                        optionName + " " + secondValueName);
            } else {
                throw new GATKException.ShouldNeverReachHereException(
                        "Argument parser failed to set maxElements");
            }
        }
    }

    // TODO: we should add a patch to the bwa-mem JNI to get the default name for the BWA-MEM index image
    private String getBwaMemIndexImageName(final String fasta) {
        final Optional<String> extension = BwaMemIndex.FASTA_FILE_EXTENSIONS.stream()
                .filter(fasta::endsWith).findFirst();
        if (!extension.isPresent()) {
            throw new UserException(String.format(
                    "the fasta file provided '%s' does not have any of the standard fasta extensions: %s",
                    fasta,
                    BwaMemIndex.FASTA_FILE_EXTENSIONS.stream().collect(Collectors.joining(", "))));
        }
        final String prefix = fasta.substring(0, fasta.length() - extension.get().length());
        return prefix + BwaMemIndex.IMAGE_FILE_EXTENSION;
    }

    @Override
    protected Object doWork() {
        final SAMSequenceDictionary dictionary = header.getSequenceDictionary();

        // get the senquence names
        final List<String> seqNames = dictionary.getSequences().stream()
                .map(SAMSequenceRecord::getSequenceName).collect(Collectors.toList());

        final ProgressMeter progressMeter = new ProgressMeter();
        progressMeter.setRecordLabel("kmer");

        progressMeter.start();
        // iterate over each contig
        IntervalUtils.getAllIntervalsForReference(dictionary).stream()
                // divide into shards without padding
                .flatMap(interval -> Shard
                        .divideIntervalIntoShards(interval, readLength, 1, 0, dictionary).stream())
                // update the progress meter
                .peek(progressMeter::update)
                // convert to simple interval (and add to the counter)
                .map(ShardBoundary::getInterval)
                // perform alignment
                .flatMap(interval -> {
                    final byte[] bases = new ReferenceContext(reference, interval).getBases();
                    final List<BwaMemAlignment> alignments =
                            aligner.alignSeqs(Collections.singletonList(bases)).get(0);
                    // TODO: error if not alignments? Could this happen? if so, add empty coverages?
                    return alignments.stream().map(al -> BwaMemAlignmentUtils.applyAlignment(
                            // same name as the SNPable splitfa
                            interval.getContig() + "_" + interval.getStart(),
                            bases, null, // no qualities
                            null, // no read group
                            al, seqNames, header,
                            // TODO: maybe generate the tags?
                            false, false));
                    // TODO: maybe add the SA tag with the extra information?
                })
                // write each read after mapping
                .forEach(r -> writer.addRead(new SAMRecordToGATKReadAdapter(r)));

        progressMeter.stop();

        return null;
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(reference);
        CloserUtil.close(writer);
        CloserUtil.close(aligner);
    }
}
