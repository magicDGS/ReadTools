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
package org.magicdgs.readtools.tools.barcodes.dictionary;

import org.magicdgs.io.readers.SpaceDelimitedReader;

import htsjdk.samtools.SAMReadGroupRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.exceptions.UserException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to create/read barcode dictionaries. Barcode files have the following columns: sampleName,
 * library, and several barcodes. They are space-delimited (tabs or other white-spaces).
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeDictionaryFactory {

    private static final Logger logger = LogManager.getLogger(BarcodeDictionaryFactory.class);

    /**
     * Reads a barcode dictionary from a file. Each of the barcodes is stored independently.
     *
     * @param run              run name. May be {@code null}.
     * @param barcodeFile      file to read the barcode information from.
     * @param unknownReadGroup read group record for unknown samples. The tags will be used
     *                         (except ID and SN) for the rest of barcodes.
     *
     * @return the barcode dictionary
     */
    public static BarcodeDictionary createDefaultDictionary(final String run,
            final Path barcodeFile, final SAMReadGroupRecord unknownReadGroup) {
        try (final SpaceDelimitedReader reader = new SpaceDelimitedReader(barcodeFile)) {
            // read the first line
            String[] nextLine = reader.next();
            if (nextLine == null) {
                throwWrongFormatException(barcodeFile);
            }
            final int numberOfBarcodes = nextLine.length - 2;
            if (numberOfBarcodes < 1) {
                throwWrongFormatException(barcodeFile);
            }
            logger.debug("Detected {} barcodes.", numberOfBarcodes);
            // at this point, we know the number of barcodes
            final List<List<String>> barcodes = new ArrayList<>(numberOfBarcodes);
            // initialize all the barcodes
            for (int i = 0; i < numberOfBarcodes; i++) {
                barcodes.add(new ArrayList<>());
            }
            // create the lists with samples and libraries
            final List<String> samples = new ArrayList<>();
            final List<String> libraries = new ArrayList<>();
            // reading the rest of the lines
            while (nextLine != null) {
                logger.debug(Arrays.toString(nextLine));
                if (numberOfBarcodes != nextLine.length - 2) {
                    throwWrongFormatException(barcodeFile);
                }
                // the first item is the sample name
                samples.add(nextLine[0]);
                libraries.add(nextLine[1]);
                // get the barcodes
                for (int i = 2; i < nextLine.length; i++) {
                    barcodes.get(i - 2).add(nextLine[i]);
                }
                nextLine = reader.next();
            }
            // construct the barcode dictionary
            return new BarcodeDictionary(run, samples, barcodes, libraries, unknownReadGroup);
        } catch (final IOException e) {
            // TODO: use the Path exception after https://github.com/broadinstitute/gatk/pull/2282
            throw new UserException.CouldNotReadInputFile(barcodeFile.toFile(), e);
        }
    }

    private static void throwWrongFormatException(final Path barcodeFile) {
        // TODO: use the Path exception after https://github.com/broadinstitute/gatk/pull/2282
        throw new UserException.MalformedFile(barcodeFile.toFile(),
                "wrong barcode file format: Each line should have two first columns (for the sample and the library) and the same number of barcodes after them.");
    }
}
