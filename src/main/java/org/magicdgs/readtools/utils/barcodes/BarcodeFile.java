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

package org.magicdgs.readtools.utils.barcodes;

import org.magicdgs.readtools.cmd.argumentcollections.ReadGroupArgumentCollection;
import org.magicdgs.readtools.exceptions.RTUserExceptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.barclay.utils.Utils;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.text.parsers.TabbedInputParser;
import org.broadinstitute.hellbender.utils.text.parsers.TabbedTextFileWithHeaderParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The barcode file format of <i>ReadTools</i> follows the same format as the
 * <a href="https://broadinstitute.github.io/picard/command-line-overview.html#ExtractIlluminaBarcodes">ExtractIlluminaBarcodes</a>
 * tool from <a href="https://broadinstitute.github.io/picard/">Picard</a>: tab-delimited table
 * with named columns for including information.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @ReadTools.note Order of the columns is not required.
 * @ReadTools.warning All the barcodes present in the multiplexed file should be included in the barcode columm. This requirement may be removed in the future
 */
// TODO: fill the group attributes
@DocumentedFeature(summary = "Barcode file format and related arguments", groupName = "TODO", groupSummary = "TODO")
public final class BarcodeFile {

    // TODO: this should contain the ReadGroupArgumentCollection instead of storing in a different place
    // TODO: like that everything will be encapsulated in the barcode file help

    private final Logger logger = LogManager.getLogger(this);

    ////////////////////////////////////
    // COLUMN DEFINITIONS

    /** Column header name for the sample name. */
    public static final String SAMPLE_NAME_COLUMN = "sample_name";

    // these constants comes from https://github.com/broadinstitute/picard/blob/master/src/main/java/picard/illumina/ExtractIlluminaBarcodes.java
    // TODO: PR for make then public in either Picard or GATK, or both
    /**
     * Preferred column header name for first barcode sequence.
     */
    public static final String BARCODE_SEQUENCE_COLUMN = "barcode_sequence";

    /**
     * Column header name for first barcode sequence if {@link #BARCODE_SEQUENCE_COLUMN} is not
     * present.
     */
    public static final String BARCODE_SEQUENCE_1_COLUMN = "barcode_sequence_1";

    /**
     * Column header name for the barcode name. It is used as sample name if {@link
     * #SAMPLE_NAME_COLUMN} is not present.
     */
    public static final String BARCODE_NAME_COLUMN = "barcode_name";

    /**
     * Column header name for the library.
     */
    public static final String LIBRARY_NAME_COLUMN = "library_name";

    ///////////////////////////
    // ARGUMENTS

    // TODO: we should make both the argument description and javadoc compatible with the
    // TODO: table for arguments and the CLI help.
    /**
     * Tab-delimited file with the information for each sequence barcode:
     *
     * <dl>
     *     <dt>Required columns</dt>
     *     <dd><b>barcode_sequence</b> or <b>barcode_sequence_1</b>: sequence for the first barcode.</dd>
     *     <dd><b>barcode_sequence_2</b>: sequence for the second barcode in dual indexed libraries. Only
     * required if more than one barcode is expected.</dd>
     * <dd><b>sample_name</b> or <b>barcode_name</b>: name for the sample, which will appear in the SM
     * Read Group field.</dd>
     * </dl>
     *
     * <dl>
     *     <dt>Optional columns</dt>
     *     <dd>library_name</b>: the name for the library, which will appear in the LB Read Group
     * field.</dd>
     * </dl>
     *
     * @ReadTools.note Barcode file will overwrite any of Read Group arguments for the same information.
     */
    @Argument(fullName = "barcodeFile", shortName = "bc", optional = false, doc =
            "Tab-delimited file with header for barcode sequences "
                    + "('" + BARCODE_SEQUENCE_COLUMN + "' for the first barcode, '"
                    + BARCODE_SEQUENCE_COLUMN + "_$(number)' "
                    + "for subsequent if more than one index is used) "
                    + "and sample name ('" + SAMPLE_NAME_COLUMN + "'). "
                    + "Other columns will be used for update the Read Group information (see online help).")
    public String barcodeFile = null;

    // TODO: we should add a javadoc saying what will happen if it is null
    // TODO: for the online documentation
    @Argument(fullName = "runName", shortName = "runName", optional = true, doc = "Run name to add to the ID in the read group information.")
    public String runID = null;

    // TODO: we should check the documentation for the argument collection
    // TODO: or put the arguments here instead
    @ArgumentCollection
    public ReadGroupArgumentCollection rgArguments = new ReadGroupArgumentCollection();

    private TabbedTextFileWithHeaderParser parser = null;
    private List<String> sequenceColumns = null;
    private String sampleNameColumn = null;

    private BarcodeDictionary barcodeDictionary = null;

    public void initBarcodFile() {
        if (parser == null) {
            Utils.nonNull(barcodeFile, "cannot initialize null barcodeFile");
            final Path path = IOUtils.getPath(barcodeFile);
            try {
                this.parser = new TabbedTextFileWithHeaderParser(
                        new TabbedInputParser(false, Files.newInputStream(path)));

                final List<String> missingColumns = new ArrayList<>();

                // setting the sample name column
                sampleNameColumn = parseTwoOptionsColumn(SAMPLE_NAME_COLUMN, BARCODE_NAME_COLUMN,
                        () -> "sample name", missingColumns);

                // setting the first barcode column
                final String firstBarcodeColumn = parseTwoOptionsColumn(BARCODE_SEQUENCE_COLUMN,
                        BARCODE_SEQUENCE_1_COLUMN,
                        () -> "first barcode", missingColumns);

                // throw if there are missing columns
                if (!missingColumns.isEmpty()) {
                    throw new RTUserExceptions.MissingColumnsBarcodeDictionaryException(path,
                            missingColumns);
                }

                // we do not expect more than two indexes, but it could happen
                this.sequenceColumns = new ArrayList<>(2);
                sequenceColumns.add(firstBarcodeColumn);
                // iterate over the possibel barcode_sequence_* names
                for (int numberOfBarcodes = 2; ; numberOfBarcodes++) {
                    final String name = BARCODE_SEQUENCE_COLUMN + "_" + numberOfBarcodes;
                    if (parser.hasColumn(name)) {
                        sequenceColumns.add(name);
                    } else {
                        break;
                    }
                }

                logger.debug("Found {} barcode columns in file", sequenceColumns::size);
            } catch (IOException e) {
                // TODO: use the Path exception after https://github.com/broadinstitute/gatk/pull/2282
                throw new UserException.CouldNotReadInputFile(path, e.getMessage(), e);
            }
        }
    }


    // TODO: document
    private final String parseTwoOptionsColumn(final String firstOption, final String secondOption,
            final Supplier<String> columnInfo, final List<String> missingColumns) {
        final String columnName;
        // setting the sample name column
        if (parser.hasColumn(firstOption)) {
            columnName = firstOption;
            if (parser.hasColumn(secondOption)) {
                logger.warn("Both '{}' and '{}' columns are present: using {} for {}.",
                        () -> firstOption, () -> secondOption,
                        () -> firstOption, columnInfo);
            }
        } else if (parser.hasColumn(secondOption)) {
            columnName = secondOption;
        } else {
            missingColumns.add("'" + firstOption + "' or '" + secondOption + "'");
            columnName = null;
        }
        logger.debug("Using '{}' column as {}.", () -> columnName, columnInfo);

        return columnName;
    }


    /** Gets the barcode dictionary associated with this file. */
    public BarcodeDictionary getBarcodeDictionary() {
        // make sure that the file is initialize
        initBarcodFile();
        if (barcodeDictionary == null) {
            // TODO: implement parsing for the new BarcodeDictionary
        }
        return barcodeDictionary;
    }

}
