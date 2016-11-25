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

package org.magicdgs.readtools.utils.read.writer;

import org.magicdgs.readtools.utils.read.ReadWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.tools.readersplitters.ReaderSplitter;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Read writer to add readers splitting by different parameters.
 *
 * This class have a lot of code in common with {@link org.broadinstitute.hellbender.tools.SplitReads}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class SplitGATKWriter implements GATKReadWriter {

    /** This is the unknown prefix. */
    public static final String UNKNOWN_OUT_PREFIX = "unknown";

    /** This is different from the GATK4 framework. */
    public static final String KEY_SPLIT_SEPARATOR = "_";

    // this is the map for every key and the writer
    private final Map<String, GATKReadWriter> outs;

    private final String outputPrefix;
    private final ReadToolsOutputFormat format;

    // the header for all the writers
    private final SAMFileHeader header;
    // the list of splitters, in order
    private final List<ReaderSplitter<?>> splitters;
    // the writer factory to create the factories
    private final ReadWriterFactory factory;
    private final boolean presorted;

    /**
     * Constructor.
     *
     * @param outputPrefix common output prefix for all the output files.
     * @param format       output extension for the writers. Note that some extensions will not
     *                     be allowed.
     * @param splitters    ordered list with the splitters to use, one after the other.
     * @param header       output header for all the readers.
     * @param presorted    if {@code true}, reads are expected to be already sorted.
     * @param factory      factory to create the writers.
     * @param onDemand     if {@code true}, creates the readers on demand; otherwise, all the
     *                     writers will be generated except the unknown.
     */
    public SplitGATKWriter(final String outputPrefix, final ReadToolsOutputFormat format,
            final List<ReaderSplitter<?>> splitters, final SAMFileHeader header,
            final boolean presorted, final ReadWriterFactory factory, final boolean onDemand) {
        // storing all parameters
        this.outputPrefix = Utils.nonNull(outputPrefix, "null prefix");
        this.format = Utils.nonNull(format, "null outputExtension");
        this.header = Utils.nonNull(header, "null header");
        this.splitters = Utils.nonEmpty(splitters, "splitters");
        this.factory = Utils.nonNull(factory, "null factory");
        this.presorted = presorted;
        // create the outputs
        this.outs = new LinkedHashMap<>();
        if (!onDemand) {
            initWriters();
        }
    }

    @Override
    public void addRead(final GATKRead read) {
        outs.computeIfAbsent(getKey(read), this::createWriterOnDemand).addRead(read);
    }

    @Override
    public void close() throws IOException {
        for (final GATKReadWriter w : outs.values()) {
            w.close();
        }
    }

    /**
     * Initializes all the writers for all splitters.
     */
    private void initWriters() {
        // Build up a list of key options at each level.
        final List<List<?>> splitKeys = splitters.stream()
                .map(splitter -> splitter.getSplitsBy(header))
                .collect(Collectors.toList());
        // For every combination of keys, add a GATKWriter.
        addKey(splitKeys, 0, "", key -> {
            outs.put(key, createWriterOnDemand(key));
        });
    }

    /**
     * Recursively builds up a key, then when it reaches the bottom of the list, calls the adder on
     * the generated key.
     *
     * @param listKeys  A outer list, where each inner list contains the output options for that
     *                  level.
     * @param listIndex The current recursive index within the listKeys.
     * @param key       The built up key recursively.
     * @param adder     Function to run on the recursively generated key once the bottom of the
     *                  outer list is reached.
     */
    private void addKey(final List<List<?>> listKeys, final int listIndex,
            final String key, final Consumer<String> adder) {
        if (listIndex < listKeys.size()) {
            for (final Object newKey : listKeys.get(listIndex)) {
                addKey(listKeys, listIndex + 1, key + KEY_SPLIT_SEPARATOR + newKey, adder);
            }
        } else {
            adder.accept(key);
        }
    }

    /**
     * Traverses the splitters generating a key for this particular record.
     *
     * @param record the record to analyze.
     *
     * @return the generated key that may then be used to find the appropriate writer.
     */
    private String getKey(final GATKRead record) {
        // if a read is missing the value for the target split, return the constant "unknown" which will
        // result in a new output stream being created on demand to hold uncategorized reads
        return splitters.stream()
                .map(s -> {
                    final Object key = s.getSplitBy(record, header);
                    return key == null ? UNKNOWN_OUT_PREFIX : key.toString();
                })
                .reduce("", (acc, item) -> acc + KEY_SPLIT_SEPARATOR + item);
    }

    // helper method to use the compute if absent
    private GATKReadWriter createWriterOnDemand(final String attributeValue) {
        return factory.createWriter(outputPrefix + attributeValue + format.getExtension(),
                header, presorted);
    }

}
