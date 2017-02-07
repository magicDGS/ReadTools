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

package org.magicdgs.readtools.tools.barcodes.dictionary;

import org.magicdgs.readtools.RTDefaults;
import org.magicdgs.readtools.cmd.argumentcollections.ReadGroupArgumentCollection;
import org.magicdgs.readtools.tools.barcodes.dictionary.decoder.BarcodeMatch;

import htsjdk.samtools.SAMReadGroupRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.utils.Utils;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.text.parsers.TabbedInputParser;
import org.broadinstitute.hellbender.utils.text.parsers.TabbedTextFileWithHeaderParser;
import scala.Tuple3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Class to create/read barcode dictionaries.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeDictionaryFactory {

    private static final Logger logger = LogManager.getLogger(BarcodeDictionaryFactory.class);

    /**
     * Description for the current barcode file format. The barcode is a tab-delimited file with
     * header with the following required columns:
     *
     * - Barcode column ({@link #BARCODE_SEQUENCE_COLUMN} or {@link #BARCODE_SEQUENCE_1_COLUMN}).
     * - Sample column ({@link #SAMPLE_NAME_COLUMN} or {@link #BARCODE_NAME_COLUMN}).
     *
     * It may contain also the following columns used for include information in the read group:
     * - Library name ({@link #LIBRARY_NAME_COLUMN}).
     *
     * Note: order of the columns is not required.
     */
    public static final String BARCODE_FILE_FORMAT_DESCRIPTION =
            "Tab-delimited file with header for barcode sequences ('"
                    + BarcodeDictionaryFactory.BARCODE_SEQUENCE_COLUMN
                    + "' or '"
                    + BarcodeDictionaryFactory.BARCODE_SEQUENCE_1_COLUMN
                    + "' for the first barcode, '"
                    + BarcodeDictionaryFactory.BARCODE_SEQUENCE_COLUMN + "_$(number)"
                    + "' for subsequent if more than one index is used), "
                    + "sample name ('"
                    + BarcodeDictionaryFactory.SAMPLE_NAME_COLUMN
                    + "' or '"
                    + BarcodeDictionaryFactory.BARCODE_NAME_COLUMN
                    + "') and, optionally, library name ('"
                    + BarcodeDictionaryFactory.LIBRARY_NAME_COLUMN
                    + "'). ";

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

    /**
     * Gets a barcode dictionary from a file in the format defined in
     * {@link #BARCODE_FILE_FORMAT_DESCRIPTION}.
     *
     * @param barcodePath path to the barcode file.
     *
     * @throws UserException if the file is malformed or an IO error occurs.
     */
    public static BarcodeDictionary fromFile(final Path barcodePath, final String runId,
            final ReadGroupArgumentCollection rgInfo) {
        Utils.nonNull(barcodePath, "null barcodePath");
        Utils.nonNull(rgInfo, "null rgInfo");
        // run Id can be null
        try {
            // similar to ExtractIlluminaBarcodes
            final TabbedTextFileWithHeaderParser barcodesParser =
                    new TabbedTextFileWithHeaderParser(
                            new TabbedInputParser(false, Files.newInputStream(barcodePath)));

            // validate the required columns and get the required ones
            final Tuple3<String, String, List<String>> columns =
                    validateRequiredColumns(barcodesParser);
            if (!columns._3().isEmpty()) {
                throw new MissingColumnsBarcodeDictionaryException(barcodePath, columns._3());
            }
            final String sequenceColumn = columns._1();
            final String sampleNameColumn = columns._2();


            // get the column names and hte number of barcodes
            final List<String> columNames = new ArrayList<>(2);
            columNames.add(sequenceColumn);
            for (int numberOfBarcodes = 2; ; numberOfBarcodes++) {
                final String name = BARCODE_SEQUENCE_COLUMN + "_" + numberOfBarcodes;
                if (barcodesParser.hasColumn(name)) {
                    columNames.add(name);
                } else {
                    break;
                }
            }

            // log the result and get the dictionary from the parser
            logger.info("Detected {} barcodes.", columNames::size);
            final BarcodeDictionary dictionary = getDictionary(barcodesParser,
                    columNames, sampleNameColumn,
                    runId, rgInfo);

            // close the barcode parser
            barcodesParser.close();

            return dictionary;

        } catch (IOException e) {
            // TODO: use the Path exception after https://github.com/broadinstitute/gatk/pull/2282
            throw new UserException.CouldNotReadInputFile(barcodePath.toFile(), e);
        }
    }

    // gets the dictionary from the tabbed text file
    private static final BarcodeDictionary getDictionary(
            final TabbedTextFileWithHeaderParser barcodesParser,
            final List<String> barcodeColumns, final String sampleNameColumn,
            final String runId, final ReadGroupArgumentCollection rgInfo) {
        // debug some information
        logger.debug("Column names for barcodes: {}", barcodeColumns::toString);
        logger.debug("Column names for sample: {}", () -> sampleNameColumn);


        // create the lists for samples and barcodes
        final List<SAMReadGroupRecord> sampleReadGroups = new ArrayList<>();
        final List<List<String>> barcodes = new ArrayList<>(barcodeColumns.size());
        barcodeColumns.forEach(s -> barcodes.add(new ArrayList<>()));

        final BiConsumer<TabbedTextFileWithHeaderParser.Row, SAMReadGroupRecord> recordUpdater =
                getRecordUpdater(barcodesParser);

        // read the barcode file as a tab-delimited file, one row per sample
        for (final TabbedTextFileWithHeaderParser.Row row : barcodesParser) {

            // this is for the read group ID
            final List<String> sampleBarcodes = new ArrayList<>(barcodeColumns.size());
            // fill up the barcodes for this sample
            for (int i = 0; i < barcodeColumns.size(); i++) {
                final String iBarcode = row.getField(barcodeColumns.get(i));
                sampleBarcodes.add(iBarcode);
                barcodes.get(i).add(iBarcode);
            }

            // get the sample name and the read group ID
            final String sampleName = row.getField(sampleNameColumn);
            String rgId = (runId == null) ? sampleName : runId + "_" + sampleName;
            rgId += "_" + String.join(RTDefaults.BARCODE_INDEX_DELIMITER, sampleBarcodes);

            // generate the sample name info
            final SAMReadGroupRecord rg = rgInfo.getReadGroupFromArguments(rgId, sampleName);
            // update the record with more information
            recordUpdater.accept(row, rg);
            // add to the sample records
            sampleReadGroups.add(rg);
        }

        // set up the unknown read group
        final SAMReadGroupRecord unknownReadGroup = rgInfo.getReadGroupFromArguments(
                BarcodeMatch.UNKNOWN_STRING, BarcodeMatch.UNKNOWN_STRING);

        // creates the barcode dictionary
        return new BarcodeDictionary(sampleReadGroups, barcodes, unknownReadGroup);
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

    // validates the barcode parser columns, if all required are set and if the optional are present
    // returns the first barcode tag, sample name column and a list with missing required columns
    private static final Tuple3<String, String, List<String>> validateRequiredColumns(
            final TabbedTextFileWithHeaderParser barcodesParser) {
        final List<String> missingColumns = new ArrayList<>();

        // setting the first barcode column
        final String firstBarcodeColumn;
        if (barcodesParser.hasColumn(BARCODE_SEQUENCE_COLUMN)) {
            firstBarcodeColumn = BARCODE_SEQUENCE_COLUMN;
            // log a warning if the other is present
            if (barcodesParser.hasColumn(BARCODE_SEQUENCE_1_COLUMN)) {
                logger.warn("Both '{}' and '{}' columns are present: using {}",
                        () -> BARCODE_SEQUENCE_COLUMN, () -> BARCODE_SEQUENCE_1_COLUMN,
                        () -> BARCODE_SEQUENCE_COLUMN);
            }
        } else if (barcodesParser.hasColumn(BARCODE_SEQUENCE_1_COLUMN)) {
            firstBarcodeColumn = BARCODE_SEQUENCE_1_COLUMN;
        } else {
            missingColumns.add("'" + BARCODE_SEQUENCE_COLUMN + "' or '" + BARCODE_SEQUENCE_1_COLUMN
                    + "'");
            firstBarcodeColumn = null;
        }
        logger.debug("Using '{}' column as first barcode.", () -> firstBarcodeColumn);


        // setting the sample name column
        final String sampleNameColumn;
        if (barcodesParser.hasColumn(SAMPLE_NAME_COLUMN)) {
            sampleNameColumn = SAMPLE_NAME_COLUMN;
            if (barcodesParser.hasColumn(BARCODE_NAME_COLUMN)) {
                logger.warn("Both '{}' and '{}' columns are present: using {} for sample name.",
                        () -> SAMPLE_NAME_COLUMN, () -> BARCODE_NAME_COLUMN,
                        () -> SAMPLE_NAME_COLUMN);
            }
        } else if (barcodesParser.hasColumn(BARCODE_NAME_COLUMN)) {
            sampleNameColumn = BARCODE_NAME_COLUMN;
        } else {
            missingColumns.add("'" + SAMPLE_NAME_COLUMN + "' or '" + BARCODE_NAME_COLUMN + "'");
            sampleNameColumn = null;
        }
        logger.debug("Using '{}' column as sample name.", () -> sampleNameColumn);

        return new Tuple3<>(firstBarcodeColumn, sampleNameColumn, missingColumns);
    }

    private static class MissingColumnsBarcodeDictionaryException
            extends UserException.MalformedFile {

        public MissingColumnsBarcodeDictionaryException(final Path path,
                final List<String> missingColumns) {
            // TODO: use the Path exception after https://github.com/broadinstitute/gatk/pull/2282
            super(path.toFile(), "barcode file does not include the following required columns: "
                    + String.join(", ", missingColumns));
        }
    }
}
