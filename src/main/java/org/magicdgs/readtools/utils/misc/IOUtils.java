/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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
 */
package org.magicdgs.readtools.utils.misc;

import htsjdk.samtools.fastq.FastqConstants;
import org.apache.commons.io.FilenameUtils;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Utils for the inputs FASTQ and BAM/SAM files.
 *
 * @author Daniel Gómez-Sánchez
 */
public class IOUtils {

    /** Default extension for recognize sam files. */
    public static final String DEFAULT_SAM_EXTENSION = ".sam";

    /** Default extension for metric files. */
    public static final String DEFAULT_METRICS_EXTENSION = ".metrics";

    /** Suffix for discarded output. */
    public static final String DISCARDED_SUFFIX = "discarded";

    /**
     * Checks if the extension of the path represents a SAM/BAM/CRAM formatted input.
     *
     * @param input the path to check.
     *
     * @return {@code true} if is is SAM/BAM/CRAM; {@code false} otherwise.
     */
    public static boolean isSamBamOrCram(final Path input) {
        return isSamBamOrCram(input.toString());
    }

    /**
     * Checks if the file is SAM/BAM/CRAM formatted by extension.
     *
     * @param sourceName the name of the file.
     *
     * @return {@code true} if the file ends with the extension for this format; {@code false}
     * otherwise.
     */
    public static boolean isSamBamOrCram(final String sourceName) {
        return org.broadinstitute.hellbender.utils.io.IOUtils.isBamFileName(sourceName)
                || org.broadinstitute.hellbender.utils.io.IOUtils.isCramFileName(sourceName)
                || DEFAULT_SAM_EXTENSION
                .equalsIgnoreCase("." + FilenameUtils.getExtension(sourceName));
    }

    /**
     * Checks if the file is a FASTQ formatted used the extensions in
     * {@link FastqConstants.FastqExtensions}.
     *
     * @param sourceName the name of the file.
     *
     * @return {@code true} if the file ends with the extension for this format; {@code false}
     * otherwise.
     */
    public static boolean isFastq(final String sourceName) {
        // assume that the constants in FastqExtensions are lower case
        final String lowerCase = sourceName.toLowerCase();
        return Stream.of(FastqConstants.FastqExtensions.values())
                .map(FastqConstants.FastqExtensions::getExtension)
                .anyMatch(lowerCase::endsWith);
    }

    /**
     * Makes an output FASTQ with the default extensions {@link FastqConstants.FastqExtensions#FQ_GZ}
     * or {@link FastqConstants.FastqExtensions#FQ_GZ} if gzip is requested.
     *
     * @param prefix the prefix for the file
     * @param gzip   {@code true} indicates that the output will be gzipped
     *
     * @return the formatted output name
     */
    public static String makeOutputNameFastqWithDefaults(final String prefix, final boolean gzip) {
        return prefix + ((gzip) ? FastqConstants.FastqExtensions.FQ_GZ.getExtension()
                : FastqConstants.FastqExtensions.FQ.getExtension());
    }

    /**
     * Creates a default metrics output without checking for the existence.
     *
     * @param prefix the prefix for the output path.
     *
     * @return the metrics path.
     */
    public static Path makeMetricsFile(final String prefix) {
        return org.broadinstitute.hellbender.utils.io.IOUtils
                .getPath(prefix + DEFAULT_METRICS_EXTENSION);
    }

    /**
     * Creates a new output file, generating all the sub-directories and checking for the existence
     * of the file if requested.
     *
     * @param output        the output file name.
     * @param checkIfExists if {@code true} it will throw an IOException if the file exists.
     *
     * @return the path object.
     *
     * @throws UserException if the file already exists or an I/O error occurs.
     */
    public static Path newOutputFile(final String output, final boolean checkIfExists) {
        try {
            final Path path = org.broadinstitute.hellbender.utils.io.IOUtils.getPath(output);
            // first check if the file already exists
            if (checkIfExists) {
                exceptionIfExists(path);
            }
            // if not, create all the directories
            Files.createDirectories(path.getParent());
            // return the file
            return path;
        } catch (IOException e) {
            // we catch the user exception in
            throw new UserException.CouldNotCreateOutputFile(output, e.getMessage(), e);
        }
    }

    /**
     * Check if the path exists and throw an exception if so.
     *
     * @param path the path to check.
     *
     * @throws IOException if the file exists.
     */
    public static void exceptionIfExists(final Path path) throws IOException {
        if (Files.exists(path)) {
            throw new IOException("File " + path.toString() + " already exists");
        }
    }
}
