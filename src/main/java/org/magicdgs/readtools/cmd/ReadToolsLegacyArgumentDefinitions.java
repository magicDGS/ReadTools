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
package org.magicdgs.readtools.cmd;

/**
 * Legacy argument definitions for ReadTools pre-releases.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadToolsLegacyArgumentDefinitions {

    // TODO: deprecate and use GATK4 names for beta
    public static final String INPUT_LONG_NAME = "input";
    public static final String INPUT_SHORT_NAME = "i";
    public static final String OUTPUT_LONG_NAME = "output";
    public static final String OUTPUT_SHORT_NAME = "o";

    // Option for maintain the format instead of standardize
    // TODO: remove this option for beta
    public static final String MAINTAIN_FORMAT_LONG_NAME = "non-standardize-output";
    public static final String MAINTAIN_FORMAT_SHORT_NAME = "nstd";
    public static final String MAINTAIN_FORMAT_DOC =
            "By default, the output of this program is encoding in Sanger. If you disable this behaviour, the format of the output will be the same as the input (not recommended)";

    // Option for disable zipped output
    // TODO: remove this option for beta
    public static final String DISABLE_ZIPPED_OUTPUT_LONG_NAME = "disable-zipped-output";
    public static final String DISABLE_ZIPPED_OUTPUT_SHORT_NAME = "dgz";
    public static final String DISABLE_ZIPPED_OUTPUT_DOC = "Disable zipped output";

    // Option for allow higher qualities in sanger
    // TODO: remove this option for beta
    public static final String ALLOW_HIGHER_SANGER_QUALITIES_LONG_NAME = "allow-higher-qualities";
    public static final String ALLOW_HIGHER_SANGER_QUALITIES_SHORT_NAME = "ahq";
    public static final String ALLOW_HIGHER_SANGER_QUALITIES_DOC =
            "Allow higher qualities for Standard encoding";

    // TODO: remove this option for beta
    public static final String PARALLEL_LONG_NAME = "number-of-threads";
    public static final String PARALLEL_SHORT_NAME = "nt";
    public static final String PARALLEL_DOC =
            "Specified the number of threads to use. Warning: real multi-thread is not quality; if using more than one thread the option is a switch and the number of threads depends on the number of outputs.";

    // legacy options for ReadTools read groups
    // TODO: remove this parameters to be in the Picard tool for the read group parameters
    public static final String RG_ID_LONG_NAME = "run-id";
    public static final String RG_ID_SHORT_NAME = "run";
    public static final String RG_ID_DOC =
            "Run name to add to the ID in the read group information.";
    public static final String RG_PLATFORM_LONG_NAME = "platform";
    public static final String RG_PLATFORM_SHORT_NAME = "pl";
    public static final String RG_PLATFORM_DOC = "Platform to add to the Read Group information";
    public static final String RG_UNIT_LONG_NAME = "platform-unit";
    public static final String RG_UNIT_SHORT_NAME = "pu";
    public static final String RG_UNIT_DOC = "Platform Unit to add to the Read Group information.";

}
