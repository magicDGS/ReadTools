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
public final class GemMappabilityException extends UserException {

    /**
     * Private constructor.
     *
     * <p>Should have specialized exceptions for different use-cases.
     *
     * @param path the path for the GEM-mappability file.
     * @param msg  exception message.
     */
    private GemMappabilityException(final Path path, final String msg) {
        super(String.format("Invalid GEM-mappability file %s %s",
                getPathName(path), msg));
    }

    /**
     * Constructor for an exception parsing a sequence sequence line.
     *
     * @param path     input file.
     * @param sequence current sequence.
     * @param position position at the current sequence.
     * @param msg      error message.
     */
    public GemMappabilityException(final Path path, final String sequence, final long position,
            final String msg) {
        this(path, String.format("at position %s:%d - %s", sequence, position, msg));
    }

    /**
     * Constructor for an exception parsing a header line.
     *
     * @param path   input file.
     * @param header current header.
     * @param msg    error message.
     */
    public GemMappabilityException(final Path path, final String header, final String msg) {
        this(path, String.format("at '%s' header - %s", header, msg));
    }

    // helper method to get the Path name (if known)
    private static final String getPathName(final Path path) {
        return (path == null) ? "unknown" : path.toUri().toString();
    }
}
