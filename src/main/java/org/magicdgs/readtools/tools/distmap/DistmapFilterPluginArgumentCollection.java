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

package org.magicdgs.readtools.tools.distmap;

import org.magicdgs.readtools.cmd.RTStandardArguments;

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.cmdline.GATKPlugin.GATKReadFilterArgumentCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Argument collection for DistMap filters.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class DistmapFilterPluginArgumentCollection extends GATKReadFilterArgumentCollection {

    @Argument(fullName = RTStandardArguments.READ_FILTER_LONG_NAME,
            shortName = RTStandardArguments.READ_FILTER_SHORT_NAME,
            doc="Read filters to be applied in the distmap pipeline", optional=true)
    public final List<String> userEnabledReadFilterNames = new ArrayList<>();

    @Override
    public List<String> getUserEnabledReadFilterNames() {
        return userEnabledReadFilterNames;
    }

    /**
     * For distmap, no default filters are supported.
     *
     * <p>There is nothing to disable for the distmap filters.
     *
     * @return empty list.
     */
    @Override
    public List<String> getUserDisabledReadFilterNames() {
        return Collections.emptyList();
    }

    /**
     * For distmap, no default trimmers are supported.
     *
     * @return {@code false}.
     */
    @Override
    public boolean getDisableToolDefaultReadFilters() {
        return false;
    }
}
