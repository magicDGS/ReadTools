/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Daniel Gómez-Sánchez
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
package org.magicdgs.readtools.cmd;

import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;

/**
 * Standard argument names for ReadTools. This encapsulates the names for all arguments that does
 * not belong to legacy tools alone.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RTStandardArguments {

    /** Cannot be instantiated. */
    private RTStandardArguments() {}

    // OUTPUT PARAMS

    /** Output for force overwrite in the tools. */
    public static final String FORCE_OVERWRITE_NAME = "forceOverwrite";

    /** Output format of the tool. */
    public static final String OUTPUT_FORMAT_NAME = "outputFormat";

    /** Parameter for creates MD5 for FASTQ files. */
    public static final String CREATE_OUTPUT_FASTQ_MD5_LONG_NAME = "createOutputFastqMD5";
    public static final String CREATE_OUTPUT_FASTQ_MD5_SHORT_NAME = "OFM";

    /** Parameter for interleave output FASTQ files. */
    public static final String INTERLEAVED_OUTPUT_FASTQ_LONG_NAME = "interleavedFastqOutput";
    public static final String INTERLEAVED_OUTPUT_FASTQ_SHORT_NAME = "IFO";

    /** Parameter for keep the discarded records from an output. */
    public static final String KEEP_DISCARDED_NAME = "keepDiscarded";

    // INPUT PARAMS

    /** Parameter for the second of the pair (if pair-end split files). */
    public static final String INPUT_PAIR_LONG_NAME =
            StandardArgumentDefinitions.INPUT_LONG_NAME + "2";
    public static final String INPUT_PAIR_SHORT_NAME =
            StandardArgumentDefinitions.INPUT_SHORT_NAME + "2";

    /** Parameter for interleaved pair-end input. */
    public static final String INTERLEAVED_INPUT_LONG_NAME = "interleavedInput";
    public static final String INTERLEAVED_INPUT_SHORT_NAME = "interleaved";

    /** Parameter for forcing a concrete encoding of the input. */
    public static final String FORCE_QUALITY_ENCODING_NAME = "forceEncoding";

    // BARCODE PARAMS

    public static final String RAW_BARCODE_SEQUENCE_TAG_NAME = "rawBarcodeSequenceTags";
    public static final String RAW_BARCODE_QUALITIES_TAG_NAME = "rawBarcodeQualityTag";
    public static final String USER_READ_NAME_BARCODE_NAME = "barcodeInReadName";

    // ARGUMENTS FOR TRIMMER PLUGIN

    public static final String TRIMMER_LONG_NAME = "trimmer";
    public static final String TRIMMER_SHORT_NAME = "TM";
    public static final String DISABLE_TRIMMER_LONG_NAME = "disableTrimmer";
    public static final String DISABLE_TRIMMER_SHORT_NAME = "DTM";
    public static final String DISABLE_ALL_DEFAULT_TRIMMERS_NAME = "disableAllDefaultTrimmers";
    public static final String DISABLE_5P_TRIMING_LONG_NAME = "disable5pTrim";
    public static final String DISABLE_5P_TRIMING_SHORT_NAME = "D5PT";
    public static final String DISABLE_3P_TRIMING_LONG_NAME = "disable3pTrim";
    public static final String DISABLE_3P_TRIMING_SHORT_NAME = "D3PT";
}
