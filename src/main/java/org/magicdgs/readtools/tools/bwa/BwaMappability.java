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

package org.magicdgs.readtools.tools.bwa;

import org.magicdgs.readtools.engine.ReadToolsProgram;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalList;
import htsjdk.samtools.util.IntervalTreeMap;
import htsjdk.samtools.util.ProgressLoggerInterface;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.tribble.bed.SimpleBEDFeature;
import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.QCProgramGroup;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.ReferenceDataSource;
import org.broadinstitute.hellbender.engine.Shard;
import org.broadinstitute.hellbender.engine.ShardBoundary;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAligner;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAlignment;
import org.broadinstitute.hellbender.utils.bwa.BwaMemAlignmentUtils;
import org.broadinstitute.hellbender.utils.bwa.BwaMemIndex;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: it will be nice to be able to create pair-end mappability!
// TODO: change BetaFeature for documented feature
@BetaFeature
// TODO: decide a better program group
@CommandLineProgramProperties(summary = "TODO", oneLineSummary = "TODO", programGroup = QCProgramGroup.class)
public final class BwaMappability extends ReadToolsProgram {

    @Argument(fullName = StandardArgumentDefinitions.REFERENCE_LONG_NAME, shortName = StandardArgumentDefinitions.REFERENCE_SHORT_NAME, doc = "Reference sequence file to compute mappability", optional = false)
    private String referenceFileName;

    // TODO: documentation for the argument
    @Argument(fullName = "readLength", doc = "Length for reads generated from the reference", optional = true)
    public int readLength = 100;


    // TODO: this output should be similar to GEM-mappability or being a BED formatted file
    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME, shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, doc = "Output BED file with mappability information.")
    public String outputName;

    // TODO: hide? this is mostly for debug purposes...
    @Advanced
    @Argument(fullName = "outputBam", doc = "Output SAM/BAM/CRAM file with reference reads", optional = true)
    public String outputBam;

    // instance here to be closed
    private BwaMemAligner aligner;
    private ReferenceDataSource reference;
    private List<SimpleInterval> windows;
    private SAMFileWriter samWriter;
    private PrintStream writer;

    // initialize the reference data source
    @Override
    public void onStartup() {
        // initialize the reference
        // TODO: this requires to be able to use path (should have a GATK/HTSJDK patch)
        reference = ReferenceDataSource.of(new File(referenceFileName));
        // generate the windows
        initializeWindows();
        // create index and initialize aligner
        initializeAligner();
        // initialize the outputs
        initializeWriters();
    }

    private void initializeWindows() {
        windows = IntervalUtils.getAllIntervalsForReference(reference.getSequenceDictionary())
                .stream()
                .flatMap(chr -> Shard.divideIntervalIntoShards(chr, readLength, readLength, 0, reference.getSequenceDictionary()).stream())
                .map(ShardBoundary::getInterval)
                .collect(Collectors.toList());
        logger.info("Generated {} single-end reads ({} bp) from reference",
                () -> windows.size(), () -> readLength);
    }


    private void initializeAligner() {
        final String bwaMemIndexImage = getBwaMemIndexImageName(referenceFileName);
        if (new File(bwaMemIndexImage).exists()) {
            logger.info("Using already generated index image: {}", bwaMemIndexImage);
        } else {
            logger.info("Indexing reference file");
            BwaMemIndex.createIndexImageFromFastaFile(referenceFileName, bwaMemIndexImage);
            logger.info("Generated index image: {}", bwaMemIndexImage);
        }
        aligner = new BwaMemAligner(new BwaMemIndex(bwaMemIndexImage));
    }

    private void initializeWriters() {
        // initialize output - TODO: check if it exists and throw if it does
        writer = new PrintStream(IOUtil.openFileForWriting(new File(outputName)));

        // initialize output bam
        if (outputBam != null) {
            // TODO: this does not accept Paths yet (because the reference does not use it anyway)
            samWriter = new SAMFileWriterFactory().makeWriter(new SAMFileHeader(reference.getSequenceDictionary()), true, new File(outputBam), new File(referenceFileName));
        } else {
            samWriter = new NullSAMFileWriter();
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
        logger.debug("Initializing work");

        final List<String> refNames = reference.getSequenceDictionary().getSequences().stream()
                .map(SAMSequenceRecord::getSequenceName).collect(Collectors.toList());

        final Stream<SAMRecord> mapped = windows.stream()
                .flatMap(s -> {
                    final byte[] bases = new ReferenceContext(reference, s).getBases();
                    // TODO: if only Ns, do not try to align (it does not make sense
                    final List<BwaMemAlignment> alignments =
                            aligner.alignSeqs(Collections.singletonList(bases)).get(0);
                    // TODO: error if not alignments? Could this happen? if so, add empty coverages?
                    return alignments.stream().map(al -> BwaMemAlignmentUtils.applyAlignment(
                                s.toString(), bases, null, null,
                                al, refNames, samWriter.getFileHeader(), false, false));

                });

        final IntervalTreeMap<Integer> coverages = new IntervalTreeMap<>();

        mapped.forEach(record -> {
            samWriter.addAlignment(record);
            final Cigar cigar = record.getCigar();
            // no coverage for this region
            if (cigar == null) {
                for (int i = record.getStart(); i < record.getEnd(); i++) {
                    coverages.putIfAbsent(new Interval(record.getContig(), i, i), 0);
                }
            } else {
                // TODO: we have to walk over each cigar element and not block instead
                // TODO: to solve issue with the no-covered regions
                // TODO: or do a pileup for the single read
                record.getAlignmentBlocks().forEach(block -> {
                    final int max = block.getReferenceStart() + block.getLength();
                    for (int i = block.getReferenceStart(); i < max; i++) {
                        final Interval blockInterval =
                                new Interval(record.getReferenceName(), i, i);
                        coverages.compute(blockInterval, (a, c) -> c == null ? 1 : c + 1);
                    }
                });
            }
        });

        final IntervalTreeMap<Integer> optimizedIntervals = new IntervalTreeMap<>();


        final Iterator<Map.Entry<Interval, Integer>> it = coverages.entrySet().iterator();
        Map.Entry<Interval, Integer> first = it.next();
        IntervalList firstAccumulator = new IntervalList(reference.getSequenceDictionary());
        firstAccumulator.add(first.getKey());
        while(it.hasNext()) {
            final Map.Entry<Interval, Integer> second = it.next();
            if (first.getValue().equals(second.getValue()) && first.getKey().abuts(second.getKey())) {
                firstAccumulator.add(second.getKey());
            } else {
                final IntervalList optimized = firstAccumulator.uniqued();
                if (optimized.size() != 1) {
                    throw new GATKException.ShouldNeverReachHereException("Wrong code");
                }
                optimizedIntervals.put(optimized.getIntervals().get(0), first.getValue());
                firstAccumulator = new IntervalList(reference.getSequenceDictionary());
                firstAccumulator.add(second.getKey());
            }
            // reset the first
            first = second;
        }



        for (final Map.Entry<Interval, Integer> entry: optimizedIntervals.entrySet()) {
            // TODO: write a correct BED file
            final String bedFeature = String.format("%s\t%s\t%s\t%s",
                    entry.getKey().getContig(), entry.getKey().getStart(), entry.getKey().getEnd(),
                    entry.getValue());
            writer.println(bedFeature);
        }

        // TODO: this is debugging code
//        // entry to IntervalList
//        final Function<Map.Entry<Interval, Integer>, IntervalList> valueMapper = (entry) -> {
//            final IntervalList list = new IntervalList(reference.getSequenceDictionary());
//            list.add(entry.getKey());
//            return list;
//        };
//
//        final Map<Integer, IntervalList> byCoverage = coverages.entrySet().stream()
//                .collect(Collectors.toMap(Map.Entry::getValue, valueMapper, INTERVAL_LIST_MERGER));
//
//        for (final Map.Entry<Integer, IntervalList> entry : byCoverage.entrySet()) {
//            System.out.println(String.format("%s bases map produce %s mappings",
//                    entry.getValue().getBaseCount(), entry.getKey()));
//        }

        return null;
    }

    @Override
    public void onShutdown() {
        CloserUtil.close(reference);
        CloserUtil.close(aligner);
        CloserUtil.close(samWriter);
        CloserUtil.close(writer);
    }

    // STATIC CLASSES FOR REUSE

    private static final BinaryOperator<IntervalList> INTERVAL_LIST_MERGER = (left, right) -> {
        left.addall(right.getIntervals());
        return left.uniqued(false);
    };

    private static final class NullSAMFileWriter implements SAMFileWriter {

        @Override
        public void addAlignment(SAMRecord alignment) {
            // no-op
        }

        @Override
        public SAMFileHeader getFileHeader() {
            return null;
        }

        @Override
        public void setProgressLogger(ProgressLoggerInterface progress) {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }
    }

}
