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
package org.magicdgs.io.readers.bam;

import org.magicdgs.readtools.utils.read.ReadReaderFactory;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.util.FastqQualityFormat;

import java.io.File;

/**
 * SamReader implementation that returns only records with Sanger formatting. It wraps a {@link
 * htsjdk.samtools.SamReader} It only could be constructed by a File
 *
 * @author Daniel Gómez-Sánchez
 */
public class SamReaderSanger extends SamReaderAbstract {

    public SamReaderSanger(File file, ReadReaderFactory factory,
            boolean allowHigherSangerQualities) {
        super(file, factory, allowHigherSangerQualities);
    }

    protected SAMRecordIterator toReturnIterator(final SAMRecordIterator iterator) {
        return SamRecordIteratorWithStandardizer.of(iterator, checker, true);
    }

    @Override
    public FastqQualityFormat getFastqQuality() {
        return FastqQualityFormat.Standard;
    }

    @Override
    public SAMRecord queryMate(SAMRecord rec) {
        return checker.standardize(reader.queryMate(rec));
    }
}
