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

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.SAMReadGroupRecord;
import org.broadinstitute.hellbender.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BarcodeDictionary {

    /**
     * Value associated with unknown Read Groups (no index sequence).
     * @see #getUnknownReadGroup()
     */
    public final String[] UNKNOWN_INDEX_SEQUENCE = new String[0];

    // this is a LinkedHashMap on construction, so we know that the elements are sorted
    private final Map<SAMReadGroupRecord, String[]> readGroupIndexes = new LinkedHashMap<>();
    private final SAMReadGroupRecord unknownReadGroup;
    private final int numberOfIndexes;

    // TODO - we can also have a constructor from a SAMFileHeader with RG
    // TODO - if https://github.com/samtools/hts-specs/issues/249 is added to the specs, we can build from SAMFileHeader
    @VisibleForTesting
    BarcodeDictionary(final List<SAMReadGroupRecord> readGroups,
            final List<String[]> readGroupIndexes,
            final SAMReadGroupRecord unknownReadGroup) {
        // non-null args and initialize
        this.unknownReadGroup = Utils.nonNull(unknownReadGroup, "null unknown read group");

        // validate sizes
        Utils.validateArg(readGroups.size() == readGroupIndexes.size(), () -> "unequal sizes for read groups and indexes");
        // handling the first sample to get the number of indexes for validation
        final Iterator<String[]> indexIterator = readGroupIndexes.iterator();
        final String[] first = indexIterator.next();
        this.numberOfIndexes = first.length;
        this.readGroupIndexes.put(readGroups.get(0),first);

        // now iterating over the indexes
        for (int i = 1; i < readGroups.size(); i++) {
            final String[] indexes = readGroupIndexes.get(i);
            Utils.validateArg(this.numberOfIndexes == indexes.length, () -> "no-matching number of indexes");
            this.readGroupIndexes.put(readGroups.get(i), indexes);
        }
    }


    public int getNumberOfIndexes() {
        return numberOfIndexes;
    }

    public int getNumberOfSamples() {
        return getReadGroups().size();
    }

    /**
     * Gets the sample Read Groups in the barcode dictionary.
     *
     * @return the list of sample read groups in the dictionary.
     */
    public List<SAMReadGroupRecord> getReadGroups() {
        // TODO - maybe cache on construction?
        return new ArrayList<>(readGroupIndexes.keySet());
    }

    // TODO: probably remove
    public SAMReadGroupRecord getReadGroupAt(final int index) {
        return getReadGroups().get(index);
    }

    public SAMReadGroupRecord getUnknownReadGroup() {
        return unknownReadGroup;
    }

    /**
     * Gets the index sequences for the Read Group.
     * @param readGroup the requested Read Group.
     * @return index sequence(s) from the Read Group; {@link #UNKNOWN_INDEX_SEQUENCE} if not found.
     */
    public String[] getReadGroupIndexSequences(final SAMReadGroupRecord readGroup) {
        return readGroupIndexes.getOrDefault(readGroup, UNKNOWN_INDEX_SEQUENCE);
    }

    /**
     * Gets all the index sequences at index {@code i} (0-based).
     * @param i the requested index.
     * @return list with the index sequences at index {@code i}.
     */
    public List<String> getIndexSequences(final int i) {
        Utils.validIndex(i, numberOfIndexes);
        // TODO - maybe it should be cached to improve efficiency!
        return readGroupIndexes.values().stream().map(seqs -> seqs[i])
                .collect(Collectors.toList());
    }

    public Set<String> getUniqueIndexSequences(final int index) {
        // TODO - maybe it should be cached to improve efficiency
        return null;
    }

    public boolean isIndexUnique(final int index, final String barcodeSequence) {
        return Collections.frequency(getIndexSequences(index), barcodeSequence) != 1;
    }
}
