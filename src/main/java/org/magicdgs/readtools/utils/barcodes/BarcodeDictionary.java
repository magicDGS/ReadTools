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

package org.magicdgs.readtools.utils.barcodes;

import htsjdk.samtools.SAMReadGroupRecord;

import java.util.List;
import java.util.Set;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeDictionary {


    public int getNumerOfIndexes() {
        // TODO: implement
        return 0;
    }

    public int getNumberOfSamples() {
        // TODO: maybe cached? unless isn't necessary
        return getReadGroups().size();
    }

    public List<SAMReadGroupRecord> getReadGroups() {
        // TODO: implement
        return null;
    }

    // TODO: maybe change signature
    public String[] getReadGroupIndexSequences(final SAMReadGroupRecord record) {
        // TODO: implement
        return null;
    }

    // TODO: maybe change signature
    public List<String> getIndexSequences(final int index) {
        // TODO: implement
        return null;
    }

    public Set<String> getUniqueIndexSequences(final int index) {
        // TODO: implement
        return null;
    }

    public boolean isIndexUnique(final int index, final String barcodeSequence) {
        return false;
    }
}
