package org.magicdgs.readtools.tools.mappability.gem;

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.programgroups.MappabiltityProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsProgram;
import org.magicdgs.readtools.exceptions.RTUserExceptions;
import org.magicdgs.readtools.utils.mappability.gem.GemMappabilityReader;

import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.IOUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.ExperimentalFeature;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converts a GEM-mappability
 * (<a href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0030377">
 * Derrien <i>et al.</i> 2012</a>) file into a BED-graph format.
 *
 * <p>The GEM-mappability format is a FASTA-like file with:</p>
 *
 * <ul>
 *     <li>Header including information for the algorithm applied (e.g., k-mer size).</li>
 *     <li>
 *         Sequences in FASTA-like format: sequence name preceded by ~ and a character per-base
 *         encoded in the header. Each character encodes a different range of values,
 *         which represent the number of mappings of the reads starting at that position.
 *     </li>
 * </ul>
 *
 * <p>This tool parses the GEM-mappability file and outputs a per-position BED-graph format. For
 * each position, different scoring systems could be use (see arguments for more information).
 * </p>
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@DocumentedFeature
@CommandLineProgramProperties(oneLineSummary = "Converts a GEM-mappability file into a BED-graph format",
        summary = GemMappabilityToBed.SUMMARY,
        programGroup = MappabiltityProgramGroup.class
)
@ExperimentalFeature
public final class GemMappabilityToBed extends ReadToolsProgram {

    protected static final String SUMMARY = "Converts a GEM-mappability file (Derrien et al. 2012) "
            + "into a BED-graph format converting the number of mappings to different kind of scores.\n"
            + "The GEM-mappability format is a FASTA-like file with:\n"
            + "\t- Header including information for the algorithm applied and encoding.\n"
            + "\t- Sequences in a FASTA-like format representing number of mappings\n"
            + "Each character encodes a different range of values, which represent the number of "
            + "mappings of each base."
            + "\n\n"
            + "Find more information about this tool in "
            + RTHelpConstants.DOCUMENTATION_PAGE + "GemMappabilityToBed.html";

    @Argument(fullName = RTStandardArguments.FORCE_OVERWRITE_NAME, shortName = RTStandardArguments.FORCE_OVERWRITE_NAME, doc = RTStandardArguments.FORCE_OVERWRITE_DOC, optional = true, common = true)
    public Boolean forceOverwrite = false;

    @Argument(fullName = RTStandardArguments.INPUT_LONG_NAME, shortName = RTStandardArguments.INPUT_SHORT_NAME, doc = "GEM-mappability file (FASTA-like) to parse")
    public String input;

    /**
     * Bed-graph output with the number of mappings as score. If it contains a block-compressed
     * extension (e.g., .bgz), it will be compressed.
     */
    @Argument(fullName = RTStandardArguments.OUTPUT_LONG_NAME, shortName = RTStandardArguments.OUTPUT_SHORT_NAME, doc = "BED-graph output with the number of mappings as score")
    public String output;

    @Argument(fullName = "score-method", doc = "Method to convert number of mappings within a range into the reported score")
    public GemScoreMethod method = GemScoreMethod.MID;

    private GemMappabilityReader reader;
    private PrintStream writer;

    @Override
    protected void onStartup() {
        final Path inputPath = IOUtils.getPath(input);
        try {
            reader = new GemMappabilityReader(inputPath);
        } catch (final IOException e) {
            throw new UserException.CouldNotReadInputFile(inputPath, e);
        }
        final Path outputPath = IOUtils.getPath(output);
        writer = new PrintStream(createStream(outputPath));
    }

    // TODO: this repeats some code form ReadWriterFactory
    // TODO: we should pull out a common class for this (https://github.com/magicDGS/ReadTools/issues/493)
    private OutputStream createStream(final Path outputPath) {
        if (!forceOverwrite && Files.exists(outputPath)) {
            throw new RTUserExceptions.OutputFileExists(outputPath);
        }
        try {
            // first create the directories if needed
            Files.createDirectories(outputPath.toAbsolutePath().getParent());
            final OutputStream os = Files.newOutputStream(outputPath);
            return IOUtil.hasBlockCompressedExtension(outputPath)
                    ? new BlockCompressedOutputStream(os, null)
                    : os;
        } catch (final IOException e) {
            throw new UserException.CouldNotCreateOutputFile(
                    // using URI to be more informative
                    outputPath.toUri().toString(),
                    // use the class name, because the exception message is printed anyway
                    e.getClass().getSimpleName(),
                    e);
        }
    }

    @Override
    protected Object doWork() {
        // start the meter
        final ProgressMeter meter = new ProgressMeter();
        meter.setRecordLabel("positions");
        meter.start();

        // iterate and convert to bed-graph
        reader.stream().forEach(record -> {
            // bed-graph format: chr, pos, pos, score
            writer.println(String.format("%s\t%d\t%d\t%s",
                    record.getContig(), record.getStart() - 1, record.getEnd(),
                    method.formatScore(record.getRange())));
            meter.update(record);
        });

        // stop the meter
        meter.stop();
        return null;
    }

    @Override
    protected void onShutdown() {
        CloserUtil.close(reader);
        CloserUtil.close(writer);
    }
}
