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

import static org.magicdgs.readtools.tools.cmd.OptionUtils.getIntArrayOptions;
import static org.magicdgs.readtools.tools.cmd.OptionUtils.getUniqueIntOption;
import static org.magicdgs.readtools.tools.cmd.OptionUtils.getUniqueValue;

import org.magicdgs.readtools.cmd.ReadToolsLegacyArgumentDefinitions;
import org.magicdgs.readtools.tools.ToolNames;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionaryFactory;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.util.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;

/**
 * Default options for barcode detectors
 *
 * @author Daniel Gómez-Sánchez
 */
public class BarcodeArgumentCollection {

    public static final String BARCODES_LONG_NAME = "barcodes";
    public static final String BARCODES_SHORT_NAME = "bc";
    public static final String BARCODES_DOC =
            "White-space delimited (tabs or spaces) file with the first column with the sample name, the second with the library name and the following containing the barcodes (1 or 2 depending on the barcoding method)";

    public static final String MAXIMUM_MISMATCH_LONG_NAME = "maximum-mismatches";
    public static final String MAXIMUM_MISMATCH_SHORT_NAME = "M";
    public static final String MAXIMUM_MISMATCH_DOC =
            "Maximum number of mismatches allowed for a matched barcode. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file. [Default="
                    + BarcodeDecoder.DEFAULT_MAXIMUM_MISMATCHES + "]";

    public static final String MINIMUM_DISTANCE_LONG_NAME = "minimum-distance";
    public static final String MINIMUM_DISTANCE_SHORT_NAME = "d";
    public static final String MINIMUM_DISTANCE_DOC =
            "Minimum distance (in difference between number of mismatches) between the best match and the second to consider a match. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file. [Default="
                    + BarcodeDecoder.DEFAULT_MIN_DIFFERENCE_WITH_SECOND + "]";

    public static final String N_NO_MISMATCH_LONG_NAME = "n-no-mismatch";
    public static final String N_NO_MISMATCH_SHORT_NAME = "nnm";
    public static final String N_NO_MISMATCH_DOC = "Do not count Ns as mismatch";

    public static final String SPLIT_LONG_NAME = "split";
    public static final String SPLIT_SHORT_NAME = "x";
    public static final String SPLIT_DOC =
            "Split each sample from the barcode dictionary in a different file.";


    public static final String MAXIMUM_N_LONG_NAME = "maximum-N";
    public static final String MAXIMUM_N_SHORT_NAME = "N";
    public static final String MAXIMUM_N_DOC =
            "Maximum number of Ns allowed in the barcode to discard it.";

    /**
     * Default option for barcodes file (it is required)
     */
    public static final Option barcodes = Option.builder(BARCODES_SHORT_NAME)
            .longOpt(BARCODES_LONG_NAME).desc(BARCODES_DOC)
            .hasArg().numberOfArgs(1).argName("BARCODES.tab").required().build();

    /**
     * Option for maximum number of mismatches
     */
    public static final Option max = Option.builder(MAXIMUM_MISMATCH_SHORT_NAME)
            .longOpt(MAXIMUM_MISMATCH_LONG_NAME).desc(MAXIMUM_MISMATCH_DOC).hasArg().numberOfArgs(1)
            .argName("INT").required(false)
            .build();

    /**
     * Option for minimum distance between matches in barcodes
     */
    public static final Option dist = Option.builder(MINIMUM_DISTANCE_SHORT_NAME)
            .longOpt(MINIMUM_DISTANCE_LONG_NAME).desc(MINIMUM_DISTANCE_DOC).hasArg()
            .numberOfArgs(1).argName("INT")
            .required(false).build();

    /**
     * Option for does not count N as mismatches
     */
    public static final Option nNoMismatch = Option.builder(N_NO_MISMATCH_SHORT_NAME)
            .longOpt(N_NO_MISMATCH_LONG_NAME)
            .desc(N_NO_MISMATCH_DOC).hasArg(false).required(false)
            .build();

    /**
     * Option for split the output by barcode
     */
    public static final Option split = Option.builder(SPLIT_SHORT_NAME)
            .longOpt(SPLIT_LONG_NAME)
            .desc(SPLIT_DOC)
            .hasArg(false).required(false).build();

    /**
     * Option for maximum number of Ns
     */
    public static final Option maxN = Option.builder(MAXIMUM_N_SHORT_NAME)
            .longOpt(MAXIMUM_N_LONG_NAME)
            .desc(MAXIMUM_N_DOC + " By default, no N threshold for match a barcode is applied.")
            .hasArg().numberOfArgs(1).argName("INT").required(false).build();

    /**
     * Option for read group ID
     */
    public static final Option run = Option
            .builder(ReadToolsLegacyArgumentDefinitions.RG_ID_SHORT_NAME)
            .longOpt(ReadToolsLegacyArgumentDefinitions.RG_ID_LONG_NAME)
            .desc(ReadToolsLegacyArgumentDefinitions.RG_ID_DOC)
            .hasArg()
            .numberOfArgs(1).argName("RUN_ID").required(false).build();

    /**
     * Option for the platform in the read group
     */
    public static final Option platform = Option
            .builder(ReadToolsLegacyArgumentDefinitions.RG_PLATFORM_SHORT_NAME)
            .longOpt(ReadToolsLegacyArgumentDefinitions.RG_PLATFORM_LONG_NAME)
            .desc(ReadToolsLegacyArgumentDefinitions.RG_PLATFORM_DOC + " By default, nothing is added. It should be one of the following: "
                    + getFormattedPlatformValues()).hasArg().numberOfArgs(1).argName("PLATFORM")
            .required(false).build();

    /**
     * Option for the platform unit in the read group
     */
    public static final Option platformUnit = Option
            .builder(ReadToolsLegacyArgumentDefinitions.RG_UNIT_SHORT_NAME)
            .longOpt(ReadToolsLegacyArgumentDefinitions.RG_UNIT_LONG_NAME)
            .desc(ReadToolsLegacyArgumentDefinitions.RG_UNIT_DOC + " By default, nothing is added.")
            .hasArg().numberOfArgs(1)
            .argName("PLATFORM_UNIT").required(false).build();

    /**
     * Add all the options for the barcodes that have read groups information to a set of options
     *
     * @param options the set of options
     */
    public static void addAllReadGroupCommonOptionsTo(Options options) {
        options.addOption(run);
        options.addOption(platform);
        options.addOption(platformUnit);
    }

    /**
     * Add all the options for the barcodes to a set of options
     *
     * @param options the set of options
     */
    public static void addAllBarcodeCommonOptionsTo(Options options) {
        options.addOption(barcodes);
        options.addOption(max);
        options.addOption(dist);
        options.addOption(nNoMismatch);
        options.addOption(split);
        options.addOption(maxN);
    }

    /**
     * Get the barcode dictionary option using the command line
     *
     * @param logger the logger to log results
     * @param cmd    the command line already parsed
     * @param length the expected number of barcodes; <code>null</code> if combined
     *
     * @return the combined barcode
     */
    public static BarcodeDictionary getBarcodeDictionaryFromOption(Log logger, CommandLine cmd,
            Integer length)
            throws IOException {
        final File inputFile = new File(getUniqueValue(cmd, BARCODES_SHORT_NAME));
        final BarcodeDictionary dictionary;
        SAMReadGroupRecord readGroupInfo = new SAMReadGroupRecord(BarcodeMatch.UNKNOWN_STRING,
                BarcodeDictionaryFactory.UNKNOWN_READGROUP_INFO);
        String pl = getUniqueValue(cmd, ReadToolsLegacyArgumentDefinitions.RG_PLATFORM_SHORT_NAME);
        if (pl != null) {
            try {
                SAMReadGroupRecord.PlatformValue.valueOf(pl);
            } catch (IllegalArgumentException e) {
                throw new ToolNames.ToolException(
                        "Platform could be only one of the following: "
                                + getFormattedPlatformValues());
            }
            readGroupInfo.setPlatform(pl);
        }
        readGroupInfo.setPlatformUnit(getUniqueValue(cmd, ReadToolsLegacyArgumentDefinitions.RG_UNIT_SHORT_NAME));
        String runID = getUniqueValue(cmd, ReadToolsLegacyArgumentDefinitions.RG_ID_SHORT_NAME);
        if (length == null) {
            throw new IllegalArgumentException("length cannot be null");
        } else {
            dictionary = BarcodeDictionaryFactory
                    .createDefaultDictionary(runID, inputFile, readGroupInfo, length);
        }
        logger.info("Loaded barcode file for ", dictionary.numberOfUniqueSamples(),
                " samples with ",
                dictionary.numberOfSamples(), " different barcode sets");
        return dictionary;
    }

    /**
     * Get a barcode decoder from the command line
     *
     * @param logger the logger to log results
     * @param cmd    the command line already parsed
     * @param length the expected number of barcodes; <code>null</code> if combined
     *
     * @return the barcode decoder
     */
    public static BarcodeDecoder getBarcodeDecoderFromOption(Log logger, CommandLine cmd,
            Integer length)
            throws IOException, ToolNames.ToolException {
        try {
            BarcodeDictionary dictionary = getBarcodeDictionaryFromOption(logger, cmd, length);
            int[] mismatches = getIntArrayOptions(cmd, MAXIMUM_MISMATCH_SHORT_NAME);
            int[] minDist = getIntArrayOptions(cmd, MINIMUM_DISTANCE_SHORT_NAME);
            Integer maxNallowed = getUniqueIntOption(cmd, MAXIMUM_N_SHORT_NAME);
            return new BarcodeDecoder(dictionary,
                    (maxNallowed == null) ? BarcodeDecoder.DEFAULT_MAXIMUM_N : maxNallowed,
                    !cmd.hasOption(N_NO_MISMATCH_SHORT_NAME), mismatches, minDist);
        } catch (IllegalArgumentException e) {
            throw new ToolNames.ToolException("Number of barcodes and thresholds does not match");
        }
    }

    /**
     * Check if the split output is set
     *
     * @return <code>true</code> if split; <code>false</code> otherwise
     */
    public static boolean isSplit(Log logger, CommandLine cmd) {
        if (cmd.hasOption(SPLIT_SHORT_NAME)) {
            logger.info("Output will be splitted");
            return true;
        }
        return false;
    }

    /**
     * Get a commma-separated String with the valid values for platform
     *
     * @return the formatted string
     */
    private static String getFormattedPlatformValues() {
        StringBuilder builder = new StringBuilder();
        for (SAMReadGroupRecord.PlatformValue val : SAMReadGroupRecord.PlatformValue.values()) {
            builder.append(val);
            builder.append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        return builder.toString();
    }
}
