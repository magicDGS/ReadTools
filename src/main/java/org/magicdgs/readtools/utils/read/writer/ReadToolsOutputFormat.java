/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.utils.misc.IOUtils;

import htsjdk.samtools.BamFileIoUtils;
import htsjdk.samtools.cram.build.CramIO;
import htsjdk.samtools.fastq.FastqConstants;

/**
 * Interface for output formats of ReadTools.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface ReadToolsOutputFormat {

    /** Gets the extension for this output format (including dot). */
    public String getExtension();

    /** FASTQ formats. */
    public static enum FastqFormat implements ReadToolsOutputFormat {
        PLAIN(FastqConstants.FastqExtensions.FQ.getExtension()),
        GZIP(FastqConstants.FastqExtensions.FQ_GZ.getExtension());

        private final String extension;

        FastqFormat(final String extension) {
            this.extension = extension;
        }

        @Override
        public String getExtension() {
            return extension;
        }
    }

    /** BAM formats. */
    public static enum BamFormat implements ReadToolsOutputFormat {
        SAM(IOUtils.DEFAULT_SAM_EXTENSION),
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

}
