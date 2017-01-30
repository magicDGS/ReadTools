/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gómez-Sánchez
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

import org.magicdgs.readtools.ProjectProperties;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.util.Iso8601Date;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.tools.picard.sam.AddOrReplaceReadGroups;

import java.io.Serializable;

/**
 * Picard AddOrReplaceReadGroup arguments, except library, sample name, read group description
 * and program group.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadGroupArgumentCollection implements Serializable {

    @Argument(fullName = AddOrReplaceReadGroups.RGLB_LONG_NAME, shortName = AddOrReplaceReadGroups.RGLB_SHORT_NAME, doc = "Read Group Library", optional = true)
    public String readGroupLibrary;

    // This is a modification w.r.t. Picard, because there is an enum for platform values that may be useful for limiting this value
    @Argument(fullName = AddOrReplaceReadGroups.RGPL_LONG_NAME, shortName = AddOrReplaceReadGroups.RGPL_SHORT_NAME, doc = "Read Group platform (e.g. illumina, solid)", optional = true)
    public SAMReadGroupRecord.PlatformValue readGroupPlatform;

    @Argument(fullName = AddOrReplaceReadGroups.RGPU_LONG_NAME, shortName = AddOrReplaceReadGroups.RGPU_SHORT_NAME, doc = "Read Group platform unit (eg. run barcode)", optional = true)
    public String readGroupPlatformUnit;

    @Argument(fullName = AddOrReplaceReadGroups.RGCN_LONG_NAME, shortName = AddOrReplaceReadGroups.RGCN_SHORT_NAME, doc = "Read Group sequencing center name", optional = true)
    public String readGroupSequencingCenter;

    @Argument(fullName = AddOrReplaceReadGroups.RGDT_LONG_NAME, shortName = AddOrReplaceReadGroups.RGDT_SHORT_NAME, doc = "Read Group run date", optional = true)
    public Iso8601Date readGroupRunDate;

    @Argument(fullName = AddOrReplaceReadGroups.RGPI_LONG_NAME, shortName = AddOrReplaceReadGroups.RGPI_SHORT_NAME, doc = "Read Group predicted insert size", optional = true)
    public Integer readGroupPredictedInsertSize;

    @Argument(fullName = AddOrReplaceReadGroups.RGPM_LONG_NAME, shortName = AddOrReplaceReadGroups.RGPM_SHORT_NAME, doc = "Read Group platform model", optional = true)
    public String readGroupPlatformModel;

    /**
     * Gets a basic Read Group from the arguments.
     *
     * Note: the program group is set to {@link ProjectProperties#getName()}.
     *
     */
    public SAMReadGroupRecord getReadGroupFromArguments(final String id, final String sampleName) {
        final SAMReadGroupRecord rg = new SAMReadGroupRecord(id);
        rg.setProgramGroup(ProjectProperties.getName());
        rg.setSample(sampleName);
        // the program group is the one in the project properties
        rg.setProgramGroup(ProjectProperties.getName());
        if (readGroupLibrary != null) {
            rg.setLibrary(readGroupLibrary);
        }
        if (readGroupPlatform != null) {
            rg.setPlatform(readGroupPlatform.toString());
        }
        if (readGroupPlatformUnit != null) {
            rg.setPlatformUnit(readGroupPlatformUnit);
        }
        if (readGroupSequencingCenter != null) {
            rg.setSequencingCenter(readGroupSequencingCenter);
        }
        if (readGroupRunDate != null) {
            rg.setRunDate(readGroupRunDate);
        }
        if (readGroupPredictedInsertSize != null) {
            rg.setPredictedMedianInsertSize(readGroupPredictedInsertSize);
        }
        if (readGroupPlatformModel != null) {
            rg.setPlatformModel(readGroupPlatformModel);
        }
        // return it
        return rg;
    }

}
