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

package org.magicdgs.readtools.tools.mapped;

import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.util.function.Function;

/**
 * TODO: document
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: substitutes threshold
@Deprecated
public abstract class IntTagFunction implements Function<GATKRead, Boolean> {

    protected final String tag;
    protected final int value;

    private IntTagFunction(final String tag, final int value) {
        ReadUtils.assertAttributeNameIsLegal(tag);
        this.tag = tag;
        this.value = value;
    }

    public static IntTagFunction getLowerThan(final String tag, final int value) {
        return new LowerThanTag(tag, value);
    }

    public static IntTagFunction getLargerThan(final String tag, final int value) {
        return new LargerThanTag(tag, value);
    }

    private static class LargerThanTag extends IntTagFunction {

        private LargerThanTag(String tag, int value) {
            super(tag, value);
        }

        @Override
        public Boolean apply(final GATKRead read) {
            return read.getAttributeAsInteger(tag) > value;
        }

        @Override
        public String toString() {
            return tag + ">" + value;
        }
    }

    private static class LowerThanTag extends IntTagFunction {

        private LowerThanTag(String tag, int value) {
            super(tag, value);
        }

        @Override
        public Boolean apply(final GATKRead read) {
            return read.getAttributeAsInteger(tag) < value;
        }

        @Override
        public String toString() {
            return tag + "<" + value;
        }
    }

}
