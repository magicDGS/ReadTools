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

package org.magicdgs.readtools.utils.read.writer;

import htsjdk.samtools.BamFileIoUtils;
import htsjdk.samtools.cram.build.CramIO;
import htsjdk.samtools.fastq.FastqConstants;
import htsjdk.samtools.util.IOUtil;
import org.apache.commons.io.FilenameUtils;
import org.broadinstitute.hellbender.utils.io.IOUtils;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Interface for input/output formats of ReadTools, which contain utility methods for checking.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface ReadToolsIOFormat {

    /** Output extension for metric files. */
    public static final String DEFAULT_METRICS_EXTENSION = ".metrics";

    /** Gets the extension for this output format (including dot). */
    public String getExtension();

    /**
     * Returns {@code true} if the format is assignable to the source name; {@code false}
     * otherwise.
     *
     * Default implementation check the extension (ignoring case) and returns {@code true}.
     */
    public default boolean isAssignable(final String sourceName) {
        return this.getExtension().equalsIgnoreCase("." + FilenameUtils.getExtension(sourceName));
    }

    /** FASTQ formats. */
    public static enum FastqFormat implements ReadToolsIOFormat {
        PLAIN(FastqConstants.FastqExtensions.FQ.getExtension(),
                FastqConstants.FastqExtensions.FASTQ.getExtension()),
        GZIP(FastqConstants.FastqExtensions.FQ_GZ.getExtension(),
                FastqConstants.FastqExtensions.FASTQ_GZ.getExtension());

        private final String extension;
        private final String alternativeExtension;

        FastqFormat(final String extension, final String alternativeExtension) {
            this.extension = extension;
            this.alternativeExtension = alternativeExtension;
        }

        @Override
        public String getExtension() {
            return extension;
        }

        // override to take into account alternative extensions
        @Override
        public boolean isAssignable(final String sourceName) {
            // assume that the constants in FastqExtensions are lower case
            final String lowerCaseName = sourceName.toLowerCase();
            return Stream.of(extension, alternativeExtension)
                    .filter(lowerCaseName::endsWith)
                    .findAny().isPresent();
        }
    }

    /** BAM formats. */
    public static enum BamFormat implements ReadToolsIOFormat {
        SAM(IOUtil.SAM_FILE_EXTENSION),
        BAM(BamFileIoUtils.BAM_FILE_EXTENSION),
        CRAM(CramIO.CRAM_FILE_EXTENSION);

        private final String extension;

        BamFormat(final String extension) {
            this.extension = extension;
        }

        @Override
        public String getExtension() {
            return extension;
        }
    }

    /**
     * Creates a default metrics output without checking for the existence.
     *
     * @param prefix the prefix for the output path.
     *
     * @return the metrics path.
     */
    public static Path makeMetricsFile(final String prefix) {
        return IOUtils.getPath(prefix + DEFAULT_METRICS_EXTENSION);
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
        return Stream.of(BamFormat.values())
                .filter(f -> f.isAssignable(sourceName))
                .findAny().isPresent();
    }

    /**
     * Checks if the file is FASTQ formatted by extensions.
     *
     * @param sourceName the name of the file.
     *
     * @return {@code true} if the file ends with the extension for this format; {@code false}
     * otherwise.
     */
    public static boolean isFastq(final String sourceName) {
        return Stream.of(FastqFormat.values())
                .filter(f -> f.isAssignable(sourceName))
                .findAny().isPresent();
    }

}
