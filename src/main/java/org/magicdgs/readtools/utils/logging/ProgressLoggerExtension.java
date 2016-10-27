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
 * SOFTWARE.
 */
package org.magicdgs.readtools.utils.logging;

import static org.magicdgs.readtools.utils.misc.Formats.timeFmt;

import org.magicdgs.readtools.utils.misc.Formats;

import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.utils.runtime.ProgressLogger;


/**
 * Extension of {@link ProgressLogger}
 *
 * @author Daniel Gómez-Sánchez
 */
public class ProgressLoggerExtension extends ProgressLogger {

    private final String verb;
    private final String noun;

    public ProgressLoggerExtension(final Logger log, final int n, final String verb,
            final String noun) {
        super(log, n, verb, noun);
        this.verb = verb;
        this.noun = noun;
    }

    public ProgressLoggerExtension(final Logger log, final int n, final String verb) {
        this(log, n, verb, "records");
    }

    public ProgressLoggerExtension(final Logger log, final int n) {
        this(log, n, "Processed");
    }

    public ProgressLoggerExtension(final Logger log) {
        this(log, 1000000);
    }

    /**
     * Formats a number of seconds into hours:minutes:seconds.
     *
     * @param seconds seconds to format
     */
    private String formatElapseTime(final long seconds) {
        final long s = seconds % 60;
        final long allMinutes = seconds / 60;
        final long m = allMinutes % 60;
        final long h = allMinutes / 60;
        return timeFmt.format(h) + ":" + timeFmt.format(m) + ":" + timeFmt.format(s);
    }

    /**
     * Get the total number of variants processed now and the elapsed time
     *
     * @return formatted String with the number of variants processed and the elapsed time for this
     * logger
     */
    public synchronized String numberOfVariantsProcessed() {
        final long seconds = getElapsedSeconds();
        final String elapsed = formatElapseTime(seconds);
        return String
                .format("%s %s %s. Elapsed time: %s", verb, Formats.commaFmt.format(getCount()),
                        noun, elapsed);
    }

    /**
     * Log the number of variants processed in this logger
     */
    public synchronized void logNumberOfVariantsProcessed() {
        log(numberOfVariantsProcessed());
    }
}
