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
package org.magicdgs.readtools.tools.cmd;

import static org.magicdgs.readtools.tools.cmd.OptionUtils.getIntArrayOptions;
import static org.magicdgs.readtools.tools.cmd.OptionUtils.getUniqueIntOption;
import static org.magicdgs.readtools.tools.cmd.OptionUtils.getUniqueValue;

import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionary;
import org.magicdgs.readtools.tools.barcodes.dictionary.BarcodeDictionaryFactory;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeDecoder;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;
import org.magicdgs.readtools.tools.ToolNames;

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
public class BarcodeOptions {

    /**
     * Default option for barcodes file (it is required)
     */
    public static final Option barcodes = Option.builder("bc").longOpt("barcodes").desc(
            "White-space delimited (tabs or spaces) file with the first column with the sample name, the second with the library name and the following containing the barcodes (1 or 2 depending on the barcoding method)")
            .hasArg().numberOfArgs(1).argName("BARCODES.tab").required().build();

    /**
     * Option for maximum number of mismatches
     */
    public static final Option max = Option.builder("m").longOpt("maximum-mismatches").desc(
            "Maximum number of mismatches allowed for a matched barcode. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file. [Default="
                    + BarcodeDecoder.DEFAULT_MAXIMUM_MISMATCHES + "]").hasArg().numberOfArgs(1)
            .argName("INT").required(false)
            .build();

    /**
     * Option for minimum distance between matches in barcodes
     */
    public static final Option dist = Option.builder("d").longOpt("minimum-distance").desc(
            "Minimum distance (in difference between number of mismatches) between the best match and the second to consider a match. It could be provided only once for use in all barcodes or the same number of times as barcodes provided in the file. [Default="
                    + BarcodeDecoder.DEFAULT_MIN_DIFFERENCE_WITH_SECOND + "]").hasArg()
            .numberOfArgs(1).argName("INT")
            .required(false).build();

    /**
     * Option for does not count N as mismatches
     */
    public static final Option nNoMismatch = Option.builder("nnm").longOpt("n-no-mismatch")
            .desc("Do not count Ns as mismatch").hasArg(false).required(false)
            .build();

    /**
     * Option for split the output by barcode
     */
    public static final Option split = Option.builder("x").longOpt("split")
            .desc("Split each sample from the barcode dictionary in a different file.")
            .hasArg(false).required(false).build();

    /**
     * Option for maximum number of Ns
     */
    public static final Option maxN = Option.builder("N").longOpt("maximum-N").desc(
            "Maximum number of Ns allowed in the barcode to discard it. By default, no N threshold for match a barcode is applied.")
            .hasArg().numberOfArgs(1).argName("INT").required(false).build();

    /**
     * Option for read group ID
     */
    public static final Option run = Option.builder("run").longOpt("run-id").desc(
            "Run name to add to the ID in the read group information. By default, nothing is added")
            .hasArg()
            .numberOfArgs(1).argName("RUN_ID").required(false).build();

    /**
     * Option for the platform in the read group
     */
    public static final Option platform = Option.builder("pl").longOpt("platform").desc(
            "Platform to add to the Read Group information. By default, nothing is added. It should be one of the following: "
                    + getFormattedPlatformValues()).hasArg().numberOfArgs(1).argName("PLATFORM")
            .required(false).build();

    /**
     * Option for the platform unit in the read group
     */
    public static final Option platformUnit = Option.builder("pu").longOpt("platform-unit").desc(
            "Platform Unit to add to the Read Group information. By default, nothing is added.")
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
        final File inputFile = new File(getUniqueValue(cmd, barcodes.getOpt()));
        final BarcodeDictionary dictionary;
        SAMReadGroupRecord readGroupInfo = new SAMReadGroupRecord(BarcodeMatch.UNKNOWN_STRING,
                BarcodeDictionaryFactory.UNKNOWN_READGROUP_INFO);
        String pl = getUniqueValue(cmd, platform.getOpt());
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
        readGroupInfo.setPlatformUnit(getUniqueValue(cmd, platformUnit.getOpt()));
        String runID = getUniqueValue(cmd, run.getOpt());
        if (length == null) {
            dictionary = BarcodeDictionaryFactory
                    .createCombinedDictionary(runID, inputFile, readGroupInfo);
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
            int[] mismatches = getIntArrayOptions(cmd, max.getOpt());
            int[] minDist = getIntArrayOptions(cmd, dist.getOpt());
            Integer maxNallowed = getUniqueIntOption(cmd, maxN.getOpt());
            return new BarcodeDecoder(dictionary,
                    (maxNallowed == null) ? BarcodeDecoder.DEFAULT_MAXIMUM_N : maxNallowed,
                    !cmd.hasOption(nNoMismatch.getOpt()), mismatches, minDist);
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
        if (cmd.hasOption(split.getOpt())) {
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
