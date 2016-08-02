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
package org.magicdgs.io.readers.fastq.paired;

import org.magicdgs.io.FastqPairedRecord;
import org.magicdgs.utils.fastq.QualityUtils;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;

import java.io.File;

/**
 * Implementation for pair-end reader with two files that always returns a Sanger encoded record
 *
 * @author Daniel Gómez-Sánchez
 */
public class FastqReaderPairedSanger extends FastqReaderPairedAbstract {


    public FastqReaderPairedSanger(FastqReader reader1, FastqReader reader2,
            boolean allowHighQualities) throws QualityUtils.QualityException {
        super(reader1, reader2, allowHighQualities);
    }

    public FastqReaderPairedSanger(File reader1, File reader2, boolean allowHighQualities)
            throws QualityUtils.QualityException {
        super(reader1, reader2, allowHighQualities);
    }

    /**
     * The returning format is always Sanger
     *
     * @return {@link htsjdk.samtools.util.FastqQualityFormat#Standard}
     */
    @Override
    public FastqQualityFormat getFastqQuality() {
        return FastqQualityFormat.Standard;
    }

    @Override
    public FastqPairedRecord next() {
        return checker.standardize(nextUnchangedRecord());
    }
}