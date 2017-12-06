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

package org.magicdgs.readtools.tools.trimming;

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.argumentcollections.TrimmerPluginArgumentCollection;

import com.google.common.annotations.VisibleForTesting;
import org.broadinstitute.barclay.argparser.Argument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Argument collection for {@link TrimReads} trimmers.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
@VisibleForTesting
public class TrimReadsTrimmerPluginArgumentCollection extends TrimmerPluginArgumentCollection {

    @Argument(fullName = RTStandardArguments.TRIMMER_LONG_NAME, shortName = RTStandardArguments.TRIMMER_SHORT_NAME, doc = "Trimmers to be applied. Note: default trimmers are applied first and then the rest of them in order.", optional = true)
    public final List<String> userTrimmerNames = new ArrayList<>(); // preserve order

    // mutex because if we disable all, we cannot disable one by one
    @Argument(fullName = RTStandardArguments.DISABLE_TRIMMER_LONG_NAME, shortName = RTStandardArguments.DISABLE_TRIMMER_SHORT_NAME, doc = "Default trimmers to be disabled.", optional = true, mutex = {
            RTStandardArguments.DISABLE_ALL_DEFAULT_TRIMMERS_NAME})
    public final Set<String> disabledTrimmers = new HashSet<>();

    /**
     * Disable all default trimmers. It may be useful to reorder the trimmers.
     */
    @Argument(fullName = RTStandardArguments.DISABLE_ALL_DEFAULT_TRIMMERS_NAME, shortName = RTStandardArguments.DISABLE_ALL_DEFAULT_TRIMMERS_NAME, doc = "Disable all default trimmers", optional = true, mutex = {
            RTStandardArguments.DISABLE_TRIMMER_LONG_NAME})
    public boolean disableAllDefaultTrimmers = false;

    @Override
    public List<String> getUserEnabledTrimmerNames() {
        return userTrimmerNames;
    }

    @Override
    public Set<String> getUserDisabledTrimmerNames() {
        return disabledTrimmers;
    }

    @Override
    public boolean getDisableToolDefaultTrimmers() {
        return disableAllDefaultTrimmers;
    }
}
