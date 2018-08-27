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

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.cmd.RTStandardArguments;

import org.broadinstitute.barclay.argparser.Argument;

import java.util.List;
import java.util.Set;

/**
 * Argument collection to use with {@link org.magicdgs.readtools.cmd.plugin.TrimmerPluginDescriptor}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class TrimmerPluginArgumentCollection {

    /**
     * Disable 5'-trimming. May be useful for downstream mark of duplicate reads, usually identified
     * by the 5' mapping position.
     * Cannot be true when disable3pTrim (D3P) is true.
     */
    // TODO - javadoc should use the {@value} pragma - and this should be parsed by the DocGen code
    @Argument(fullName = RTStandardArguments.DISABLE_5P_TRIMING_LONG_NAME, shortName = RTStandardArguments.DISABLE_5P_TRIMING_SHORT_NAME,
            doc = "Disable 5'-trimming."
                    // This is a custom mutex argument, specify as in Barclay but it could be specify in the command line
                    // TODO: see also https://github.com/broadinstitute/barclay/issues/26
                    + " Cannot be true when argument "
                    + RTStandardArguments.DISABLE_3P_TRIMING_LONG_NAME
                    + " (" + RTStandardArguments.DISABLE_3P_TRIMING_SHORT_NAME + ") is true.",
            optional = true)
    public boolean disable5pTrim = false;

    @Argument(fullName = RTStandardArguments.DISABLE_3P_TRIMING_LONG_NAME, shortName = RTStandardArguments.DISABLE_3P_TRIMING_SHORT_NAME,
            doc = "Disable 3'-trimming."
                    // This is a custom mutex argument, specify as in Barclay but it could be specify in the command line
                    // TODO: see also https://github.com/broadinstitute/barclay/issues/26
                    + " Cannot be true when argument "
                    + RTStandardArguments.DISABLE_5P_TRIMING_LONG_NAME
                    + " (" + RTStandardArguments.DISABLE_5P_TRIMING_SHORT_NAME + ") is true.",
            optional = true)
    public boolean disable3pTrim = false;


    /**
     * Gets the trimmers enabled by the user in the command line.
     *
     * <p>Note: should preserve the order.
     *
     * @return non {@code null} list of trimmers to use.
     */
    public abstract List<String> getUserEnabledTrimmerNames();


    /**
     * Gets the trimmers disabled by the user in the command line.
     *
     * @return non {@code null} list of trimmers to be disabled. It might be empty if disabling
     * trimmers is unsupported.
     */
    public abstract Set<String> getUserDisabledTrimmerNames();


    /**
     * Checks how default trimmers should be handled.
     *
     * @return {@code true} if default trimmers should be disabled; {@code false} otherwise.
     */
    public abstract boolean getDisableToolDefaultTrimmers();

}
