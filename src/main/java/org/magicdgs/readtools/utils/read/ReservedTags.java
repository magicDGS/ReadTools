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

package org.magicdgs.readtools.utils.read;

import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Reserved SAM tags for ReadTools. Some of the tags will be output to the final user, but some
 * are just used internally.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class ReservedTags {

    /** Cannot be instantiated. */
    private ReservedTags() {}

    /**
     * Integer flag representing the start trim point for a read after trimming. It is used in
     * combination with the {@link #te} tag to trim a read.
     *
     * If {@link #te} is present, the not trimmed read extends the range [ts, te);
     * if not, [ts, read length).
     *
     * @see RTReadUtils#getTrimmingStartPoint(GATKRead).
     * @see RTReadUtils#updateTrimmingStartPointTag(GATKRead, int).
     * @see RTReadUtils#updateTrimmingPointTags(GATKRead, int, int).
     */
    public static final String ts = "ts";

    /**
     * Integer flag representing the end trim point for a read after trimming. It is used in
     * combination with the {@link #ts} tag to trim a read.
     *
     * If {@link #ts} is present, the not trimmed read extends the range [ts, te);
     * if not, [0, te).
     *
     * @see RTReadUtils#getTrimmingEndPoint(GATKRead).
     * @see RTReadUtils#updateTrimmingEndPointTag(GATKRead, int).
     * @see RTReadUtils#updateTrimmingPointTags(GATKRead, int, int).
     */
    public static final String te = "te";

    /**
     * Integer flag indicating if a read that is completely trimmed. If this flag is set to 0, the
     * read is not completely trimmed; other values indicates that the read was trimmed. The values
     * are implementation-dependent.
     *
     * @see RTReadUtils#isCompletelyTrimRead(GATKRead).
     * @see RTReadUtils#updateCompletelyTrimReadFlag(GATKRead).
     * @see RTReadUtils#updateTrimmingPointTags(GATKRead, int, int).
     */
    public static final String ct = "ct";

}
