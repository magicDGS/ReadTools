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

package org.magicdgs.readtools.tools.quality;

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.cmd.argumentcollections.FixBarcodeAbstractArgumentCollection;
import org.magicdgs.readtools.engine.ReadToolsWalker;
import org.magicdgs.readtools.metrics.ReadsChecksumMetric;
import org.magicdgs.readtools.utils.read.RTReadUtils;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.metrics.StringHeader;
import org.apache.commons.lang3.ArrayUtils;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.QCProgramGroup;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Computes a checksum for the reads contained in any kind of read source. This checksum is
 * independent of the order of the reads in the file, which is useful for check for data integrity
 * after conversion between formats or sorting.
 *
 * <p>This tool is similar to <a href="https://academic.oup.com/bioinformatics/article-lookup/doi/10.1093/bioinformatics/btv539">BamHash</a>,
 * with the difference that it allows to include tags in the hash computation.</p>
 *
 * <p>The information used to compute the MD5 checksum string is the following:</p>
 *
 * <ul>
 *     <li><b>Read name:</b> the read name does not include the barcode information for FASTQ files.</li>
 *     <li><b>Paired information:</b> flags indicating if the read is paired (0x1), first of pair (0x40) or second of pair (0x80).</li>
 *     <li><b>Vendor/Platform quality check:</b> flag indicating that the read does not pass the control check (0x200).</li>
 *     <li><b>Bases and qualities:</b> always in the forward strand (reverse-complement if necessary)</li>
 *     <li><b>Custom tags:</b> include SAM tags in the hash computation in an order-independent way. By default, the barcode information stored in the ' BC' tag is included.</li>
 * </ul>
 *
 * @ReadTools.note The checksum computed by this tool does not contain mapping information
 * (from flags or other SAM columns) because it is not designed to assess integrity of mapping
 * information.
 * @ReadTools.warning Different ReadTools versions may generate a different checksums if a bug was
 * found in the hash computation. In case of doubt, run the tool again with the original file.
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Computes an order-independent checksum for the reads contained in any read source",
        summary = "Computes a checksum hash for the reads contained in a SAM/BAM/CRAM/FASTQ file, "
                + "independent of the order to assess data integrity after conversion or sorting.\n"
                + "The hash code does not include mapping information and it is independent "
                + "of the order of appearance of the reads.\n"
                + "This tool is useful for assess integrity of data after mapping or processing, "
                + "when the information of the read itself will not be changed.\n\n"
                + "Find more information about this tool in "
                + RTHelpConstants.DOCUMENTATION_PAGE + "ReadsChecksum.html",
        programGroup = QCProgramGroup.class)
@BetaFeature
@DocumentedFeature
public class ReadsChecksum extends ReadToolsWalker {

    // TODO: include barcode information fixer arguments, to assess that it will check correctly
    // TODO: that they are respresenting the same

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME, shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, doc = "Output the checksum metric to this file")
    public String outputFile;

    @Argument(fullName = "tag", optional = true, doc = "Include the provided tag(s) in the hash computation. If provided, it will override the default value. Set to null to remove the default value.")
    public Set<String> tagsToInclude = new TreeSet<>(RTReadUtils.RAW_BARCODE_TAG_LIST);

    @ArgumentCollection
    public FixBarcodeAbstractArgumentCollection fixBarcodeArguments =
            FixBarcodeAbstractArgumentCollection.getArgumentCollection(true);

    // we use MD5 hashing for the reads
    private final static HashFunction MD5_HASH_FUNCTION = Hashing.md5();

    // should initialize the accumulator using the same function
    // this assess that the combine method works (requires the same number of bytes)
    private HashCode hashAccumulator = MD5_HASH_FUNCTION.hashInt(0);
    // checksum for the file
    private ReadsChecksumMetric checksum = new ReadsChecksumMetric();

    @Override
    public String[] customCommandLineValidation() {
        fixBarcodeArguments.validateArguments();
        return super.customCommandLineValidation();
    }

    @Override
    protected void apply(final GATKRead read) {
        final GATKRead fixed = fixBarcodeArguments.fixBarcodeTags(read);
        accumulateHash(getHash(fixed));
    }

    @Override
    protected void apply(final Tuple2<GATKRead, GATKRead> pair) {
        // fix the barcode pair tag
        final Tuple2<GATKRead, GATKRead> fixed = fixBarcodeArguments.fixBarcodeTags(pair);
        accumulateHash(getHash(fixed._1), getHash(fixed._2));
    }

    // helper method
    private HashCode getHash(final GATKRead read) {
        return RTReadUtils.readHash(read, tagsToInclude, MD5_HASH_FUNCTION);
    }

    // helper method
    private void accumulateHash(final HashCode... hashes) {
        // because this is sum, it is order independent
        // maybe we should come out with a better solution, because this may explode
        for (HashCode hash: hashes) {
            hashAccumulator = Hashing.combineUnordered(Arrays.asList(hashAccumulator, hash));
            checksum.NUMBER_OF_READS++;
        }
    }

    /** Returns the read hash code. */
    @Override
    public Object onTraversalSuccess() {
        checksum.READS_CHECKSUM = hashAccumulator.toString();
        writeOutput();
        return checksum.READS_CHECKSUM;
    }

    /** Writes the metrics file. */
    private void writeOutput() {
        // create the metrics file with default headers
        // TODO: maybe we shouldn't include this headers, but just the extra information
        final MetricsFile<ReadsChecksumMetric, Integer> metricsFile = getMetricsFile();
        // add String headers with information included
        if (!tagsToInclude.isEmpty()) {
            metricsFile.addHeader(new StringHeader(
                    "Extra information in tag(s): " + String.join(", ", tagsToInclude)));
        }
        metricsFile.addMetric(checksum);

        final Path path = IOUtils.getPath(outputFile);
        try (final Writer writer = Files.newBufferedWriter(path)) {
            metricsFile.write(writer);
        } catch (final IOException e) {
            // just log the exception to error
            logger.error("Cannot write output file {}.", path::toUri);
            logger.error("Checksum has not failed. Hash for file: {}", checksum.READS_CHECKSUM);
            throw new UserException.CouldNotCreateOutputFile(outputFile, e.getMessage(), e);
        }
    }
}
