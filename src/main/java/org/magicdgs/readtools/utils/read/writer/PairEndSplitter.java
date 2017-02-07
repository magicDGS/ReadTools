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

package org.magicdgs.readtools.utils.read.writer;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.tools.readersplitters.ReaderSplitter;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Arrays;
import java.util.List;

/**
 * Split readers read by pair-end status:
 *
 * - First of pair: 1
 * - Second of pair: 2
 * - Single end: SE
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class PairEndSplitter extends ReaderSplitter<String> {

    private final static List<String> SPLIT_BY_LIST = Arrays.asList("1", "2", "SE");

    @Override
    public List<String> getSplitsBy(SAMFileHeader header) {
        return SPLIT_BY_LIST;
    }

    @Override
    public String getSplitBy(final GATKRead record, final SAMFileHeader header) {
        if (record.isFirstOfPair()) {
            return "1";
        } else if (record.isSecondOfPair()) {
            return "2";
        } else {
            return "SE";
        }
    }
}
