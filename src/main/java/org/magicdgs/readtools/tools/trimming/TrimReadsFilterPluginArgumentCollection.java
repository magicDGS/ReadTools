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

import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.cmdline.GATKPlugin.GATKReadFilterArgumentCollection;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;

import java.util.ArrayList;
import java.util.List;

/**
 * Argument collection for ReadFilters, which use the same names as the
 * {@link org.broadinstitute.hellbender.cmdline.GATKPlugin.DefaultGATKReadFilterArgumentCollection}
 * but changing the description to specify where in the pipeline they are applied.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class TrimReadsFilterPluginArgumentCollection extends GATKReadFilterArgumentCollection {

    @Argument(fullName = StandardArgumentDefinitions.READ_FILTER_LONG_NAME,
            shortName = StandardArgumentDefinitions.READ_FILTER_SHORT_NAME,
            doc="Read filters to be applied after trimming", optional=true)
    public final List<String> userEnabledReadFilterNames = new ArrayList<>();

    @Argument(fullName = StandardArgumentDefinitions.DISABLE_READ_FILTER_LONG_NAME,
            shortName = StandardArgumentDefinitions.DISABLE_READ_FILTER_SHORT_NAME,
            doc="Read filters to be disabled after trimming", optional=true)
    public final List<String> userDisabledReadFilterNames = new ArrayList<>();

    @Argument(fullName = StandardArgumentDefinitions.DISABLE_TOOL_DEFAULT_READ_FILTERS,
            shortName = StandardArgumentDefinitions.DISABLE_TOOL_DEFAULT_READ_FILTERS,
            doc = "Disable all tool default read filters for trimming", optional = true)
    public boolean disableToolDefaultReadFilters = false;

    @Override
    public List<String> getUserEnabledReadFilterNames() {
        return userEnabledReadFilterNames;
    }

    @Override
    public List<String> getUserDisabledReadFilterNames() {
        return userDisabledReadFilterNames;
    }

    @Override
    public boolean getDisableToolDefaultReadFilters() {
        return disableToolDefaultReadFilters;
    }
}
