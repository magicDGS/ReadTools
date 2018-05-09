/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.utils.mappability.gem;

import org.broadinstitute.hellbender.exceptions.UserException;

import java.nio.file.Path;

/**
 * Exceptions related with GEM-mappability.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GemMappabilityException extends UserException {

    /**
     * Constructor for a concrete file and line number.
     *
     * @param path       the path for the GEM-mappability file.
     * @param lineNumber line number on the file (approx.)
     * @param msg        exception message.
     */
    public GemMappabilityException(final Path path, final int lineNumber, final String msg) {
        super(String.format("Invalid GEM-mappability file %s at line %s: %s",
                path, lineNumber, msg));
    }

    /**
     * Private constructor for adding a cause to the exception.
     *
     * @param msg       message.
     * @param throwable cause.
     */
    private GemMappabilityException(final String msg, final Throwable throwable) {
        super(msg, throwable);
    }

    /**
     * Constructs an exception coming from a reading problem.
     *
     * @param path       the path for the GEM-mappability file.
     * @param lineNumber line number on the file (approx.)
     * @param exception  exception causing the error (usually {@link java.io.IOException} or {@link
     *                   htsjdk.samtools.util.RuntimeIOException}.
     *
     * @return exception with a message related with reading.
     */
    public static GemMappabilityException readingException(final Path path, final int lineNumber,
            final Exception exception) {
        return new GemMappabilityException(String.format("Error reading GEM-mappability file %s at %s",
                        path.toUri().toString(), lineNumber), exception);
    }
}
