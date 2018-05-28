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

package org.magicdgs.readtools.engine;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.RTHelpConstants;
import org.magicdgs.readtools.utils.read.RTReadUtils;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Abstract class for all the ReadTools programs.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class ReadToolsProgram extends CommandLineProgram {

    @Override
    protected void printLibraryVersions() {
        // print versions from the MANIFEST
        try {
            final String classPath = getClass().getResource(getClass().getSimpleName() + ".class").toString();
            if (classPath.startsWith("jar")) {
                final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
                try ( final InputStream manifestStream = new URL(manifestPath).openStream() ) {
                    final Attributes manifestAttributes = new Manifest(manifestStream).getMainAttributes();
                    final String htsjdkVersion = manifestAttributes.getValue("htsjdk-Version");
                    final String picardVersion = manifestAttributes.getValue("GATK-Version");

                    logger.info("HTSJDK Version: " + (htsjdkVersion != null ? htsjdkVersion : "unknown"));
                    logger.info("GATK Version: " + (picardVersion != null ? picardVersion : "unknown"));

                }
            }
        }
        catch (IOException ignored) {
        }
        // log that we are using a patched version of GATK
        // TODO: remove once https://github.com/magicDGS/ReadTools/issues/443 is fixed
        logger.info("Using GATK patch from https://github.com/bioinformagik/gatk");
    }

    @Override
    protected void printSettings() {
        super.printSettings();
        logger.info("Barcode sequence ({}) separator: '{}'",
                () -> RTReadUtils.RAW_BARCODE_TAG,
                () -> RTDefaults.BARCODE_INDEX_DELIMITER);
        logger.info("Barcode quality ({}) separator: '{}'",
                () -> RTReadUtils.RAW_BARCODE_QUALITY_TAG,
                () -> RTDefaults.BARCODE_QUALITY_DELIMITER);
        logger.info("Number of records to detect quality: {}",
                () -> RTDefaults.MAX_RECORDS_FOR_QUALITY);
        // for debugging
        logger.debug("sampling_quality_checking_frequency : {}",
                () -> RTDefaults.SAMPLING_QUALITY_CHECKING_FREQUENCY);
        logger.debug("force_overwrite : {}",
                () -> RTDefaults.FORCE_OVERWRITE);
        logger.debug("discarded_output_suffix : {}",
                () -> RTDefaults.DISCARDED_OUTPUT_SUFFIX);
    }

    /**
     * Returns a program tag to the header with a program version {@link #getVersion()}, program
     * name {@link #getToolName()} and command line {@link #getCommandLine()}.
     *
     * @param header the header to get an unique program group ID.
     *
     * @return the program record.
     */
    protected final SAMProgramRecord getProgramRecord(final SAMFileHeader header) {
        final SAMProgramRecord programRecord = new SAMProgramRecord(createProgramGroupID(header));
        programRecord.setProgramVersion(getVersion());
        programRecord.setCommandLine(getCommandLine());
        programRecord.setProgramName(getToolName());
        return programRecord;
    }

    /**
     * Returns the program group ID that will be used in the SAM writer.
     * Starts with {@link #getToolName} and looks for the first available ID by appending
     * consecutive integers.
     */
    private final String createProgramGroupID(final SAMFileHeader header) {
        final String toolName = getToolName();

        String pgID = toolName;
        SAMProgramRecord record = header.getProgramRecord(pgID);
        int count = 1;
        while (record != null) {
            pgID = toolName + "." + String.valueOf(count++);
            record = header.getProgramRecord(pgID);
        }
        return pgID;
    }

    /**
     * Returns the name of this tool, which is the combination of
     * {@link RTHelpConstants#PROGRAM_NAME} and the simple class name.
     */
    public final String getToolName() {
        return RTHelpConstants.PROGRAM_NAME + " " + getClass().getSimpleName();
    }
}
