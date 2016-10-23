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

import static org.magicdgs.readtools.utils.misc.Formats.timeFmt;

/**
 * Simple time watch to get the elapsed times
 *
 * @author Daniel Gómez-Sánchez
 */
public class TimeWatch {

    // value for store the starting time
    private long start;

    /**
     * Start a new TimeWatch
     *
     * @return a new TimeWatch from the current time
     */
    public static TimeWatch start() {
        return new TimeWatch();
    }

    /**
     * Private constructor
     */
    private TimeWatch() {
        reset();
    }

    /**
     * Reset the TimeWatch and set the start to the current time
     */
    public void reset() {
        start = System.currentTimeMillis();
    }

    /**
     * Get the elapsed time from the call of start/reset
     *
     * @return the elapsed time
     */
    public long time() {
        long end = System.currentTimeMillis();
        return end - start;
    }

    /**
     * Get the elapsed time (formatted)
     *
     * @return the elapsed time
     */
    public String toString() {
        return formatElapseTime(time() / 1000);
    }

    /**
     * Private formatter for the elapsed time in seconds
     *
     * @param seconds the time in seconds
     *
     * @return formatted time
     */
    private String formatElapseTime(final long seconds) {
        final long s = seconds % 60;
        final long allMinutes = seconds / 60;
        final long m = allMinutes % 60;
        final long h = allMinutes / 60;
        return String.format("%s:%s:%s", timeFmt.format(h), timeFmt.format(m), timeFmt.format(s));
    }
}
