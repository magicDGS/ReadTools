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

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.cmd.argumentcollections.ReadGroupArgumentCollection;
import org.magicdgs.readtools.exceptions.RTUserExceptions;

import htsjdk.samtools.SAMReadGroupRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.barclay.utils.Utils;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.text.parsers.TabbedInputParser;
import org.broadinstitute.hellbender.utils.text.parsers.TabbedTextFileWithHeaderParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The barcode file format of <i>ReadTools</i> follows the same format as the
 * <a href="https://broadinstitute.github.io/picard/command-line-overview.html#ExtractIlluminaBarcodes">ExtractIlluminaBarcodes</a>
 * tool from <a href="https://broadinstitute.github.io/picard/">Picard</a>. It is a  tab-delimited
 * table
 * with named columns for including information for each barcode:
 *
 * TODO: remove this table from here and populate from the arguments the required columns
 * <dl>
 * <dt>Required columns</dt>
 * <dd><b>barcode_sequence</b> or <b>barcode_sequence_1</b>: sequence for the first barcode.</dd>
 * <dd><b>barcode_sequence_2</b>: sequence for the second barcode in dual indexed libraries. Only
 * required if more than one barcode is expected.</dd>
 * <dd><b>sample_name</b> or <b>barcode_name</b>: name for the sample, which will appear in the SM
 * Read Group field.</dd>
 * </dl>
 *
 * <dl>
 * <dt>Optional columns</dt>
 * <dd>library_name</b>: the name for the library, which will appear in the LB Read Group
 * field.</dd>
 * </dl>
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @ReadTools.note Order of the columns is not required.
 * @ReadTools.warning All the barcodes present in the multiplexed file should be included in the
 * barcode columm. This requirement may be removed in the future
 */
// TODO: fill the group attributes
// TODO: document separately the ReadGroupArgumentCollection to add as extraDocs
// TODO: document that this shouldn't be an argument container!
@DocumentedFeature(summary = "Barcode file format and related arguments", groupName = "TODO", groupSummary = "TODO")
public final class BarcodeFile {

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


    @Argument(fullName = SAMPLE_NAME_COLUMN, doc = "Column header name for the sample name", optional = false)
    public static final String dummy1 = null;

    @Argument(fullName = BARCODE_SEQUENCE_COLUMN, doc = "Column header name for first barcode sequence", optional = false, mutex = {BARCODE_SEQUENCE_1_COLUMN})
    public static final String dummy2 = null;

    @Argument(fullName = BARCODE_SEQUENCE_1_COLUMN, doc = "Column header name for the first barcode sequence if '" + BARCODE_SEQUENCE_COLUMN  + "' is not present", optional = false, mutex = {BARCODE_SEQUENCE_COLUMN})
    public static final String dummy3 = null;

    @Argument(fullName = BARCODE_NAME_COLUMN, doc = "Column header name for the barcode name if '" + SAMPLE_NAME_COLUMN + "' is not present", optional = false, mutex = {BARCODE_SEQUENCE_COLUMN})
    public static final String dummy4 = null;

    @Argument(fullName = LIBRARY_NAME_COLUMN, doc = "Column header name for the library", optional = true)
    public static final String dummy5 = null;

    /////////////////////////////
    // FIELDS DESCRIBING THE FILE FORMAT

    private final Path path;
    private final List<String> sequenceColumns;
    private final String sampleNameColumn;

    /**
     * Intialize a barcode file from a path.
     *
     * @param path the path to get the barcode file from.
     */
    public BarcodeFile(final Path path) {
        // store the path to come back to it
        this.path = Utils.nonNull(path, "null path");

        // initialize the parser to read the columns
        final TabbedTextFileWithHeaderParser parser = openParser();
        try {
            // keep track of the missing columsn
            final List<String> missingColumns = new ArrayList<>();

            // setting the sample name column
            this.sampleNameColumn = parseTwoOptionsColumn(parser,
                    SAMPLE_NAME_COLUMN, BARCODE_NAME_COLUMN,
                    () -> "sample name", missingColumns);

            // setting the first barcode column
            final String firstBarcodeColumn = parseTwoOptionsColumn(parser,
                    BARCODE_SEQUENCE_COLUMN, BARCODE_SEQUENCE_1_COLUMN,
                    () -> "first barcode", missingColumns);

            // throw if there are missing columns
            if (!missingColumns.isEmpty()) {
                throw new RTUserExceptions.MissingColumnsBarcodeDictionaryException(path,
                        missingColumns);
            }

            // we do not expect more than two indexes, but it could happen
            this.sequenceColumns = new ArrayList<>(2);
            sequenceColumns.add(firstBarcodeColumn);
            // iterate over the possible barcode_sequence_* names
            for (int numberOfBarcodes = 2; ; numberOfBarcodes++) {
                final String name = BARCODE_SEQUENCE_COLUMN + "_" + numberOfBarcodes;
                if (parser.hasColumn(name)) {
                    sequenceColumns.add(name);
                } else {
                    break;
                }
            }

            logger.debug("Found {} barcode columns in file", sequenceColumns::size);
        } finally {
            parser.close();
        }
    }

    // helper method to open a new parser
    private final TabbedTextFileWithHeaderParser openParser() {
        try {
            return new TabbedTextFileWithHeaderParser(
                    new TabbedInputParser(false, Files.newInputStream(path)));
        } catch (IOException e) {
            throw new UserException.CouldNotReadInputFile(path, "unable to read barcode file", e);
        }
    }

    // helper method to parse the header column when two options are possible
    // the method logs a warning if both are present, and also fills in the missing columns
    // for logging afterwards
    private final String parseTwoOptionsColumn(final TabbedTextFileWithHeaderParser parser,
            final String firstOption, final String secondOption,
            final Supplier<String> columnInfo, final List<String> missingColumns) {
        final String columnName;
        // setting the sample name column
        if (parser.hasColumn(firstOption)) {
            columnName = firstOption;
            if (parser.hasColumn(secondOption)) {
                logger.warn("Both '{}' and '{}' columns are present: using {} for {}",
                        () -> firstOption, () -> secondOption,
                        () -> firstOption, columnInfo);
            }
        } else if (parser.hasColumn(secondOption)) {
            columnName = secondOption;
        } else {
            missingColumns.add("'" + firstOption + "' or '" + secondOption + "'");
            columnName = null;
        }
        logger.debug("Using '{}' column as {}", () -> columnName, columnInfo);

        return columnName;
    }

    /**
     * Gets the barcode dictionary associated with this file.
     *
     * <p>TODO: more detauls
     *
     * @param runID       run ID to add to the Read Group. If {@code null}, it will be the sample
     *                    name.
     * @param rgArguments Read Group arguments to apply if the barcode dictionary does not contain
     *                    enough information.
     *
     * @return a new instance of the barcode dictionary represented by this file, with Read Groups
     * containing the information provided.
     */
    public BarcodeDictionary getBarcodeDictionary(final String runID,
            final ReadGroupArgumentCollection rgArguments) {
        // initialize the parser to read the columns
        final TabbedTextFileWithHeaderParser parser = openParser();

        try {
            // debug some information
            logger.debug("Column names for barcodes: {}", sequenceColumns::toString);
            logger.debug("Column names for sample: {}", () -> sampleNameColumn);


            // create the lists for samples and barcodes
            final List<SAMReadGroupRecord> sampleReadGroups = new ArrayList<>();
            final List<String[]> rgSequences = new ArrayList<>();

            final BiConsumer<TabbedTextFileWithHeaderParser.Row, SAMReadGroupRecord>
                    recordUpdater =
                    getRecordUpdater(parser);

            // read the barcode file as a tab-delimited file, one row per sample
            for (final TabbedTextFileWithHeaderParser.Row row : parser) {

                // sequences for the barcode
                final String[] seqs = new String[sequenceColumns.size()];

                // fill up the barcodes for this sample
                for (int i = 0; i < sequenceColumns.size(); i++) {
                    final String iBarcode = row.getField(sequenceColumns.get(i));
                    seqs[i] = iBarcode;
                }
                rgSequences.add(seqs);

                // get the sample name and the read group ID
                final String sampleName = row.getField(sampleNameColumn);
                String rgId = (runID == null) ? sampleName : runID + "_" + sampleName;
                rgId += "_" + String.join(RTDefaults.BARCODE_INDEX_DELIMITER, seqs);

                // generate the sample name info
                final SAMReadGroupRecord rg =
                        rgArguments.getReadGroupFromArguments(rgId, sampleName);
                // update the record with more information
                recordUpdater.accept(row, rg);
                // add to the sample records
                sampleReadGroups.add(rg);
            }

            // set up the unknown read group
            final SAMReadGroupRecord unknownReadGroup = rgArguments.getReadGroupFromArguments(
                    BarcodeMatch.UNKNOWN_STRING, BarcodeMatch.UNKNOWN_STRING);

            // TODO: should the dict be cached?
            return new BarcodeDictionary(sampleReadGroups, rgSequences, unknownReadGroup);
        } finally {
            parser.close();
        }
    }

    // helper funciton to update the record
    private static final BiConsumer<TabbedTextFileWithHeaderParser.Row, SAMReadGroupRecord> getRecordUpdater(
            final TabbedTextFileWithHeaderParser barcodesParser) {
        // TODO: update more stuff
        return (barcodesParser.hasColumn(LIBRARY_NAME_COLUMN))
                ? (row, rg) -> rg.setLibrary(row.getField(LIBRARY_NAME_COLUMN))
                // TODO: this should be changed and do not output anything?
                : (row, rg) -> rg.setLibrary(rg.getId());
    }

}
