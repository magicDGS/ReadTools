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

package org.magicdgs.readtools.engine;

import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.utils.read.transformer.CheckQualityReadTransformer;

import org.broadinstitute.hellbender.engine.ReadWalker;
import org.broadinstitute.hellbender.transformers.ReadTransformer;

/**
 * Wrapper around {@link ReadWalker} for ReadTools.
 *
 * <p>Includes the following specificities for ReadTools.
 *
 * <ul>
 *     <li>Logging is done with {@link RTHelpConstants}.</li>
 *     <li>Uses {@link CheckQualityReadTransformer} to throw for wrongly encoded qualities.</li>
 * </ul>
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: decide if we should add other defaults (filters/transformers/arguments)
public abstract class RTReadWalker extends ReadWalker {

    @Override
    public ReadTransformer makePreReadFilterTransformer() {
        // this should not be disabled
        return new CheckQualityReadTransformer();
    }

    @Override
    protected final void printLibraryVersions() {
        RTHelpConstants.printLibraryVersions(this.getClass(), logger);
    }

    @Override
    protected final void printSettings() {
        RTHelpConstants.printSettings(logger);
    }

    @Override
    protected final String getSupportInformation() {
        return RTHelpConstants.getSupportInformation();
    }

    @Override
    protected String getToolkitShortName() {
        return RTHelpConstants.PROGRAM_NAME;
    }
}
