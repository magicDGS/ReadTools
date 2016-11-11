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

package org.magicdgs.readtools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default constants for ReadTools. All this constants are settable by a java property
 * as '-Dreadtools.propertyname'.
 *
 * This class is similar to {@link htsjdk.samtools.Defaults}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class RTDefaults {

    // logger for the defauls
    private static final Logger logger = LogManager.getLogger(RTDefaults.class);

    /** Maximum number of record used to guess the quality of a file. Default=1000000. */
    public static final long MAX_RECORDS_FOR_QUALITY;

    /** Force overwrite of output files. Default=false. */
    public static final boolean FORCE_OVERWRITE;

    static {
        MAX_RECORDS_FOR_QUALITY = (long) getIntProperty("max_record_for_quality", 1000000);
        FORCE_OVERWRITE = getBooleanProperty("force_overwrite", false);
    }


    /**
     * Gets a string system property, prefixed with "readtools." using the default if the property
     * does not exist or if the java.security manager raises an exception for applications started
     * with -Djava.security.manager .
     */
    private static String getStringProperty(final String name, final String def) {
        try {
            return System.getProperty("readtools." + name, def);
        } catch (final java.security.AccessControlException error) {
            logger.warn(
                    "java Security Manager forbids 'System.getProperty(\"{}\"), returning default value: {}",
                    name, def);
            logger.debug(error);
            return def;
        }
    }

    /**
     * Gets a boolean system property, prefixed with "readtools." using the default if the property
     * does not exist.
     */
    private static boolean getBooleanProperty(final String name, final boolean def) {
        final String value = getStringProperty(name, Boolean.toString(def));
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets an int system property, prefixed with "readtools." using the default if the property
     * does not exist.
     */
    private static int getIntProperty(final String name, final int def) {
        final String value = getStringProperty(name, Integer.toString(def));
        return Integer.parseInt(value);
    }

}
