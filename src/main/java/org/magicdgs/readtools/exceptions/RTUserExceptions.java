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

package org.magicdgs.readtools.exceptions;

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;

import org.broadinstitute.hellbender.exceptions.UserException;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collection of {@link UserException} from ReadTools.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTUserExceptions extends UserException {

    public RTUserExceptions(final String msg) {
        super(msg);
    }

    /**
     * Exception for files which exists when {@link RTStandardArguments#FORCE_OVERWRITE_NAME} is
     * not provided.
     */
    // TODO: extend CouldNotCreateOutputFile instead if there is path support
    public static final class OutputFileExists extends RTUserExceptions {

        public OutputFileExists(final String outputSource) {
            super("Couldn't write file " + outputSource
                    + " because file already exists. "
                    + "Please, use --" + RTStandardArguments.FORCE_OVERWRITE_NAME
                    + " if you are sure that the file should be overridden");
        }

        public OutputFileExists(final Path outputPath) {
            this(outputPath.toString());
        }
    }

    /**
     * Exception for invalid output formats.
     */
    // TODO: extend CouldNotCreateOutputFile instead if there is path support
    public static final class InvalidOutputFormat extends RTUserExceptions {

        public InvalidOutputFormat(final String outputSource, final String message) {
            super(String.format("Invalid output format for %s: %s", outputSource, message));
        }

        public InvalidOutputFormat(final String outputSource,
                final ReadToolsIOFormat... allowedFormats) {
            this(outputSource, "extension should match one of " + String.join(", ",
                    Stream.of(allowedFormats).map(ReadToolsIOFormat::getExtension)
                            .collect(Collectors.toList())));
        }
    }
}
