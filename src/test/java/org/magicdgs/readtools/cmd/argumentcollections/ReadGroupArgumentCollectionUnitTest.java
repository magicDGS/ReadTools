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

import org.magicdgs.readtools.utils.tests.BaseTest;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.util.Iso8601Date;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadGroupArgumentCollectionUnitTest extends BaseTest {

    @Test
    public void testGetNoArgumentsReadGroup() throws Exception {
        final SAMReadGroupRecord expected = new SAMReadGroupRecord("RGID");
        expected.setProgramGroup("ReadTools");
        expected.setSample("sampleName");
        Assert.assertEquals(new ReadGroupArgumentCollection()
                        .getReadGroupFromArguments(expected.getReadGroupId(), expected.getSample()),
                expected);
    }

    @Test
    public void testGetReadGroupWithArguments() throws Exception {
        // setting expected RG
        final SAMReadGroupRecord expected = new SAMReadGroupRecord("RGID");
        expected.setProgramGroup("ReadTools");
        expected.setSample("sampleName");
        // setting args
        final ReadGroupArgumentCollection rgargs = new ReadGroupArgumentCollection();

        // starting setting params
        expected.setLibrary("LB");
        rgargs.readGroupLibrary = "LB";

        expected.setPlatform("ILLUMINA");
        rgargs.readGroupPlatform = SAMReadGroupRecord.PlatformValue.ILLUMINA;

        expected.setPlatformUnit("PU");
        rgargs.readGroupPlatformUnit = expected.getPlatformUnit();

        expected.setSequencingCenter("CN");
        rgargs.readGroupSequencingCenter = expected.getSequencingCenter();

        final Iso8601Date date = new Iso8601Date("2007-11-03");
        expected.setRunDate(date);
        rgargs.readGroupRunDate = date;

        expected.setPredictedMedianInsertSize(100);
        rgargs.readGroupPredictedInsertSize = expected.getPredictedMedianInsertSize();

        expected.setPlatformModel("PM");
        rgargs.readGroupPlatformModel = expected.getPlatformModel();

        // testing
        Assert.assertEquals(rgargs
                        .getReadGroupFromArguments(expected.getReadGroupId(), expected.getSample()),
                expected);
    }

}