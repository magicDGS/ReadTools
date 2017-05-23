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

package org.magicdgs.readtools.tools.distmap;

import org.magicdgs.readtools.cmd.programgroups.DistmapProgramGroup;
import org.magicdgs.readtools.engine.ReadToolsProgram;

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.argparser.Hidden;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Download, sort and merge the alignments generated by DistMap. See the summary for more
 * information.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@CommandLineProgramProperties(oneLineSummary = "Download, sort and merge the alignments generated by DistMap.",
        summary = "Download, sort and merge the results for running DistMap "
                + "(Pandey & Schlötterer (PLoS ONE 8, 2013, e72614).\n"
                + "This tool scan the folder provided as input for multi-part BAM/SAM/CRAM files (e.g. 'part-*'), "
                + "sort and merge them by batches (in the temp directory) and finally merge "
                + "all the batches into a single output file.\n"
                + "Note: The results are expected to be located in the Hadoop FileSystem (HDFS) and the "
                + "output file in the local computer for following usage, but it is not restricted.",
        programGroup = DistmapProgramGroup.class)
@DocumentedFeature
public final class DownloadDistmapResult extends ReadToolsProgram {

    @Argument(fullName = StandardArgumentDefinitions.INPUT_LONG_NAME, shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME, doc = "Input folder to look for Distmap multi-part file results. Expected to be in an HDFS file system.", common = true, optional = false)
    public String inputFolder;

    @Hidden
    @Argument(fullName = "partName", shortName = "partName", doc = "Only download this part(s). For debugging.", optional = true)
    public Set<String> partNames = new HashSet<>();

    @ArgumentCollection
    public DistmapPartDownloader downloader = new DistmapPartDownloader();

    private final List<Path> partFiles = new ArrayList<>();

    /** Scans the input folder for part files to be downloaded. */
    @Override
    protected void onStartup() {
        final Path inputPath = IOUtils.getPath(inputFolder);
        if (!inputPath.toUri().getScheme().startsWith("hdfs")) {
            logger.warn("Input folder is not in HDFS: {}", inputPath::toUri);
        }
        try {
            Files.list(inputPath)
                    // add only part files
                    .filter(path -> path.getFileName().toString().startsWith("part-"))
                    .forEach(partFiles::add);
        } catch (IOException e) {
            throw new UserException.CouldNotReadInputFile(inputPath, e.getMessage(), e);
        }
        if (partFiles.isEmpty()) {
            throw new UserException.BadInput(inputFolder + " does not contain part files");
        }
        logger.info("Found {} parts in {}.", partFiles::size, inputPath::toUri);
        logger.debug("Parts: {}", partFiles::toString);

        subsetPartsToDebug();

    }

    /** Subsets the part files if the hidden argument for download only some parts is provided. */
    private void subsetPartsToDebug() {
        // only for debugging
        if (!partNames.isEmpty()) {
            partFiles.removeIf(p -> !partNames.contains(p.getFileName().toString()));

            if (partFiles.size() != partNames.size()) {
                throw new UserException.BadInput("Not found"
                        + (partFiles.size() - partNames.size())
                        + " parts specified with --partNames");
            }

            logger.warn("Only {} parts will be downloaded.", partNames::size);
            logger.debug("Parts to download: {}",
                    () -> partFiles.stream().map(Path::toUri).collect(Collectors.toList()));

        }
    }

    /** Runs the downloader {@link DistmapPartDownloader#downloadParts(List, Function)}. */
    @Override
    protected Object doWork() {
        downloader.downloadParts(partFiles, this::getProgramRecord);
        return null;
    }
}
