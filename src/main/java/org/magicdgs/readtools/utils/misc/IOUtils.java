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

import htsjdk.samtools.BamFileIoUtils;
import htsjdk.samtools.fastq.FastqConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utils for the inputs FASTQ and BAM/SAM files
 *
 * @author Daniel Gómez-Sánchez
 */
public class IOUtils {

    public static final String DEFAULT_SAM_EXTENSION = ".sam";

    public static final String DEFAULT_METRICS_EXTENSION = ".metrics";

    /**
     * Check if the file is BAM or SAM formatted
     *
     * @param input the input file
     *
     * @return <code>true</code> if it is a BAM/SAM; <code>false</code> otherwise
     */
    public static boolean isBamOrSam(final File input) {
        return BamFileIoUtils.isBamFile(input) || input.getName().endsWith(DEFAULT_SAM_EXTENSION);
    }

    /**
     * Make an output FASTQ with the default extensions {@link FastqConstants.FastqExtensions#FQ_GZ}
     * or {@link FastqConstants.FastqExtensions#FQ_GZ} if gzip is requested
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
     * Create a default metrics file without checking
     *
     * @param prefix the prefix for the file
     *
     * @return the metrics file
     */
    public static File makeMetricsFile(final String prefix) {
        return new File(String.format("%s%s", prefix, DEFAULT_METRICS_EXTENSION));
    }

    /**
     * Create a new output file, generating all the sub-directories and checking for the existence
     * of the file if
     * requested
     *
     * @param output        the output file
     * @param checkIfExists <code>true</code> if the file should be check, <code>false</code>
     *                      otherwise
     *
     * @return the file object
     *
     * @throws IOException if the file already exists or an IO error occurs
     */
    public static File newOutputFile(final String output, final boolean checkIfExists) throws IOException {
        final File file = new File(output);
        // first check if the file already exists
        if (checkIfExists) {
            exceptionIfExists(file);
        }
        // if not, create all the directories
        createDirectoriesForOutput(file);
        // return the file
        return file;
    }

    /**
     * Create all the directories from an output file
     *
     * @param output the output file
     *
     * @throws IOException if IO errors occur
     */
    public static void createDirectoriesForOutput(final File output) throws IOException {
        final Path parentDirectory = Paths.get(output.getAbsolutePath()).getParent();
        Files.createDirectories(parentDirectory);
    }

    /**
     * Check if the file exists and throw an exception if so
     *
     * @param file the file to check
     *
     * @throws IOException if the file exists
     */
    public static void exceptionIfExists(final File file) throws IOException {
        if (file.isFile()) {
            throw new IOException("File " + file.getAbsolutePath() + " already exists");
        }
    }
}
