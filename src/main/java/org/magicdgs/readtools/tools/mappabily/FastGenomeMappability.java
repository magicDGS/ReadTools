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

import org.magicdgs.readtools.cmd.argumentcollections.BwaMemArgumentCollection;
import org.magicdgs.readtools.engine.ReadToolsProgram;
import org.magicdgs.readtools.utils.bwa.BwaUtils;


import htsjdk.samtools.SAMFlag;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.Locatable;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.argumentcollections.IntervalArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.OptionalIntervalArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.ReferenceInputArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.RequiredReferenceInputArgumentCollection;
import org.broadinstitute.hellbender.cmdline.programgroups.FastaProgramGroup;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.ReferenceDataSource;
import org.broadinstitute.hellbender.engine.Shard;
import org.broadinstitute.hellbender.engine.ShardBoundary;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAligner;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAlignment;
import org.broadinstitute.hellbender.utils.bwa.BwaMemIndex;
import org.broadinstitute.hellbender.utils.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@BetaFeature
@CommandLineProgramProperties(oneLineSummary = "Extracts and maps all overlapping k-mer sub-sequences from a FASTA reference",
        summary = "",
        programGroup = FastaProgramGroup.class)
public class FastGenomeMappability extends ReadToolsProgram {

    private static final int MAXIMUM_XA_HITS_IN_OUTPUT = 1000000;

    // TODO: better documentation
    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME, shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME)
    public String outputPath;

    // TODO: use other argument name and doc, not the SNPable one
    @Argument(fullName = Snpable.READ_LENGTH_ARGUMENT_NAME, doc = Snpable.READ_LENGTH_ARGUMENT_DESC, minValue = 1)
    public Integer readLength;

    // TODO: better documentation and make optional
    @Argument(fullName = "approximationParameter", minValue = 0)
    public Integer t;

    @Argument(fullName = "maxMismatches", minValue = 0)
    public Integer m;

    // TODO: this should be only for debugging - should be removed from the CLI parsing
    @ArgumentCollection
    public ReferenceInputArgumentCollection referenceArgs =
            new RequiredReferenceInputArgumentCollection();

    // useful for specifying only one chromosome to test mappability
    // or a specific region
    @ArgumentCollection
    public IntervalArgumentCollection intervalArgs = new OptionalIntervalArgumentCollection();

    @ArgumentCollection
    public BwaMemArgumentCollection bwaArgs = new BwaMemArgumentCollection();

    // reference data source to get overlapping regions
    private ReferenceDataSource reference;
    // the intervals to consider
    private List<SimpleInterval> intervals;
    // aligner
    private BwaMemAligner aligner;
    // output
    private PrintStream output;

    @Override
    protected void onStartup() {
        // initialize reference source
        reference = ReferenceDataSource.of(referenceArgs.getReferenceFile());
        // initialize the intervals
        intervals = intervalArgs.intervalsSpecified()
                ? intervalArgs.getIntervals(reference.getSequenceDictionary())
                : IntervalUtils.getAllIntervalsForReference(reference.getSequenceDictionary());

        logger.info("Reference sequence will be divided in {} overlapping {}-mers",
                () -> intervals.stream().mapToInt(interval -> interval.size() - readLength + 1)
                        .sum(),
                () -> readLength);

        // initialize aligner
        initializeAligner();
        // initialize output
        initializeOutput();
    }

    private void initializeOutput() {
        try {
            output = new PrintStream(Files.newOutputStream(IOUtils.getPath(outputPath)));
            // TODO: similar output to GEM-mappability
            // print header first - TODO: print also parameters from the BWA-MEM algorithm
            output.println("~~K-MER LENGTH");
            output.println(readLength);
            output.println("~~APPROXIMATION THRESHOLD");
            output.println(t);
            output.println("~~MAX MISMATCHES");
            output.println(m);
            output.println("~~ENCODING");
            int i = 0;
            for (; i < MAX_CARDINALITY_VALUE; i++) {
                final char encoding = getCharForCardinality(i);
                logger.debug("Cardinality {} encoded as '{}'", i, encoding);
                output.printf("'%c'~[%d-%d]\n", encoding, i, i);
            }
        } catch (IOException e) {
            throw new UserException(e.getMessage(), e);
        }
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

        // set the maximum XA hits - used for the cardinality
        // TODO: this is forced, if the BWA-mem args is setting this we should log a warn!!
        aligner.setMaxXAHitsAltOption(MAXIMUM_XA_HITS_IN_OUTPUT);
    }

    @Override
    protected Object doWork() {
        final SAMSequenceDictionary dictionary = reference.getSequenceDictionary();

        // this is the F by chromosome (simpler for output)
        final Map<String, int[]> mappability = new LinkedHashMap<>(dictionary.size());

        for (final SAMSequenceRecord seq: dictionary.getSequences()) {
            // allocate the mappability per-contig
            // TODO: remove sequences that aren't in the specified intervals
            mappability.put(seq.getSequenceName(), new int[seq.getSequenceLength()]);
        }

        final ProgressMeter progressMeter = new ProgressMeter();
        progressMeter.setRecordLabel(readLength + "-mers");
        progressMeter.start();

        // storing the maximum cardinality
        final int maxCardinality = intervals.stream()
                // divide into shards without padding
                .flatMap(interval -> Shard
                        .divideIntervalIntoShards(interval, readLength, 1, 0, dictionary).stream())
                // convert to simple interval
                .map(ShardBoundary::getInterval)
                // filter out intervals that are not of the requested length
                // this is necessary because the Sard.dividiveIntervalIntoShards generates at the end
                // of the reference slices of shorter size, that we do not require
                // TODO: what happen with the last bases of the reference??? - where is the mappability there
                .filter(interval -> interval.size() == readLength)
                .mapToInt(currentKmer -> {
                    final ContigPosition kmer = new ContigPosition(currentKmer);
                    // perform alignment if necessary
                    int cardinality = 0;
                    if (mappability.get(currentKmer.getContig())[currentKmer.getStart()] == 0) {
                        final Set<ContigPosition> mappedCoordinates = mapKmer(currentKmer);
                        // get the cardinality (number of alignments - only SA or SA and XA)
                        cardinality = mappedCoordinates.size();
                        // and set to the current contig
                        mappability.get(kmer.getContig())[kmer.getStart()] = cardinality;
                        // remove the same position if included
                        mappedCoordinates.remove(kmer);
                        // if the cardinality is larger than t
                        if (cardinality != 0 && cardinality > t) {
                            // for each of the alignment coordinates (contig/start)
                            // update the mappability.get(contig)[start] -> if it is 0, set to the cardinality; otherwise, to the maximum
                            for (final ContigPosition alignment : mappedCoordinates) {
                                final int[] map = mappability.get(alignment.getContig());
                                if (map[alignment.getStart()] == 0) {
                                    map[alignment.getStart()] = cardinality;
                                } else {
                                    map[alignment.getStart()] =
                                            Math.max(cardinality, map[alignment.getStart()]);
                                }
                            }
                        }
                    } else {
                        logger.debug("Already computed kmer: {} (fast algorithm)", currentKmer);
                    }
                    progressMeter.update(currentKmer);
                    return cardinality;
                }).max().orElse(0);

        progressMeter.stop();
        logger.info("Output result in {}", outputPath);
        logger.debug("Maximum cardinality: {}", maxCardinality);

        // print the last encoding system reference
        final int upperBoundMaxCardinality = Math.max(MAX_CARDINALITY_VALUE, maxCardinality);
        logger.debug("Cardinality {} encoded as '{}' (upper-bound is {})", MAX_CARDINALITY_VALUE, MAX_CARDINALITY_CODE, upperBoundMaxCardinality);
        output.printf("'%c'~[%d-%d]\n", MAX_CARDINALITY_CODE, MAX_CARDINALITY_VALUE, upperBoundMaxCardinality);
        for (final Map.Entry<String, int[]> map: mappability.entrySet()) {
            output.print("~");
            output.print(map.getKey());
            int j = 0;
            for (final int val: map.getValue()) {
                if (j % 60 == 0) {
                    output.println();
                }
                output.print(getCharForCardinality(val));
                j++;
            }
            if (j % 60 != 0) {
                output.println();
            }
        }

        return null;
    }

    private static final char MAX_CARDINALITY_CODE = '}';
    private static final char MIN_CARNINALITY_CODE = ' ';
    private static final int MAX_CARDINALITY_VALUE = MAX_CARDINALITY_CODE - MIN_CARNINALITY_CODE;

    private static final char getCharForCardinality(final int value) {
        // ASCII range - (20, 125) - ' ', '}'
        if (value < MAX_CARDINALITY_VALUE) {
            return (char) (MIN_CARNINALITY_CODE + value);
        } else {
            return MAX_CARDINALITY_CODE;
        }
    }


    // TODO: add params
    private Set<ContigPosition> mapKmer(final SimpleInterval kmer) {
        final byte[] bases = new ReferenceContext(reference, kmer).getBases();

        // map the sequence
        final List<BwaMemAlignment> alignments = aligner.alignSeqs(
                Collections.singletonList(bases))
                .get(0);

        final Set<ContigPosition> locations = new LinkedHashSet<>(alignments.size());

        // TODO: maybe we can output the alignments for debugging?
        for (final BwaMemAlignment al : alignments) {
            // only if it is mapped
            if (SAMFlag.READ_UNMAPPED.isUnset(al.getSamFlag())) {
                if (al.getNMismatches() <= m) {
                    locations.add(new ContigPosition(
                            reference.getSequenceDictionary().getSequence(al.getRefId()).getSequenceName(),
                            // TODO: the plus one comes from the BwaMemAlignmentUtils
                            // TODO: I guess that it is 0-based in bwa compared to 1-based in the framework
                            al.getRefStart() + 1));
                }
                // we parse the XA tag even if the number of mismatches is not hold
                // TODO: should we parse even if it is unmapped? - I do not expect to have XA tags in that case
                // XA tag pattern: (chr,pos,CIGAR,NM;)*
                // TODO: we can also pre-compile the pattern of (chr,pos,CIGAR,NM;)*
                final String xa = al.getXATag();
                if (xa != null && !xa.isEmpty()) {
                    final String[] xalingments = xa.split(";");
                    for (final String xal : xalingments) {
                        final String[] comp = xal.split(",");
                        if (Integer.parseInt(comp[3]) <= m) {
                            locations.add(new ContigPosition(
                                    comp[0],
                                    // TODO: check if the XA tag is 0-based or 1-based
                                    // it may be negative due to mapping to the reverse strand
                                    Math.abs(Integer.parseInt(comp[1]))));
                        }
                    }
                }

            }
        }
        return locations;
    }


    // light-weight contig start that allows to
    private static class ContigPosition {

        private final String contig;
        private final int start;

        private ContigPosition(final Locatable loc) {
            this(loc.getContig(), loc.getStart());
        }

        private ContigPosition(final String contig, final int start) {
            this.contig = contig;
            this.start = start;
        }

        public String getContig() {
            return contig;
        }

        public int getStart() {
            return start;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null || !(obj instanceof ContigPosition)) {
                return false;
            }
            final ContigPosition that = (ContigPosition) obj;
            return start == that.start && this.contig.equals(that.contig);
        }

        @Override
        public int hashCode() {
            return  31 * start + contig.hashCode();
        }
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(reference);
        CloserUtil.close(aligner);
        CloserUtil.close(output);
    }
}
