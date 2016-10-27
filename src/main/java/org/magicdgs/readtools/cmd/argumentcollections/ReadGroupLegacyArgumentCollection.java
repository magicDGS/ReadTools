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
 * SOFTWARE.
 */

package org.magicdgs.readtools.cmd.argumentcollections;

import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionaryFactory;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;

import htsjdk.samtools.SAMReadGroupRecord;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;

/**
 * Legacy argument collection for get read groups from barcodes.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadGroupLegacyArgumentCollection implements ArgumentCollectionDefinition {

    /** Run ID in read groups. */
    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.RG_ID_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.RG_ID_SHORT_NAME, optional = true, doc = ReadToolsLegacyArgumentDefinitions.RG_ID_DOC)
    public String runId = null;

    /** Platform in read groups. */
    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.RG_PLATFORM_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.RG_PLATFORM_SHORT_NAME, optional = true, doc = ReadToolsLegacyArgumentDefinitions.RG_PLATFORM_DOC)
    public SAMReadGroupRecord.PlatformValue platform = null;


    /** Platform unit in read groups. */
    @Argument(fullName = ReadToolsLegacyArgumentDefinitions.RG_UNIT_LONG_NAME, shortName = ReadToolsLegacyArgumentDefinitions.RG_UNIT_SHORT_NAME, optional = true, doc = ReadToolsLegacyArgumentDefinitions.RG_UNIT_DOC)
    public String platformUnit = null;

    /**
     * Gets basic Read Group from the arguments. The ID is set to
     * {@link BarcodeMatch#UNKNOWN_STRING} and using the information from
     * {@link BarcodeDictionaryFactory#UNKNOWN_READGROUP_INFO}.
     */
    public SAMReadGroupRecord getUnknownBasicReadGroup() {
        final SAMReadGroupRecord readGroupInfo = new SAMReadGroupRecord(BarcodeMatch.UNKNOWN_STRING,
                BarcodeDictionaryFactory.UNKNOWN_READGROUP_INFO);
        if (platform != null) {
            readGroupInfo.setPlatform(platform.toString());
        }
        readGroupInfo.setPlatformUnit(platformUnit);
        return readGroupInfo;
    }

}
