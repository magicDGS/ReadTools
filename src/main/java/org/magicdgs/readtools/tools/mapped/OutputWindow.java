/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Daniel Gomez-Sanchez
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

package org.magicdgs.readtools.tools.mapped;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.Locatable;
import htsjdk.tribble.SimpleFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;

import java.util.Arrays;
import java.util.HashMap;

/**
 * TODO: remove class
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class OutputWindow implements Locatable {
    private final SimpleInterval interval;
    private boolean isLast;

    private HashMap<String, Boolean[]> visited = new HashMap<>();
    private int total;
    private int proper;
    private int softclip;
    private boolean sc;
    private int indel;
    private boolean ind;
    private int[] values;
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Public constructor
     *
     * @param ref	Reference name of the window
     * @param start	Start position of the window
     * @param end	End position of the window
     * @param headerContext	Context	Context of the window (header of the file)
     * @param nTags	Number of tags recorded for this window
     * @param softclip
     * @param indel
     */
    public OutputWindow(String ref, int start, int end, SAMFileHeader headerContext, int nTags, boolean softclip, boolean indel) throws IllegalArgumentException {
        interval = IntervalUtils.trimIntervalToContig(ref, start, end, headerContext.getSequence(ref).getSequenceLength());
        if (interval == null) {
            // TODO: something downstream relies on this exception!!!
            throw new IllegalArgumentException(String.format("BUGBUG: %s:%s-%s (%s) provides null contig", ref, start, end, headerContext.getSequence(ref)));
        }
        isLast = interval.getEnd() != end;

        total = 0;
        proper = 0;
        this.softclip = 0;
        this.indel = 0;
        sc = softclip;
        ind = indel;
        values = new int[nTags];
        Arrays.fill(values, 0);
    }

    @Override
    public String getContig() {
        return interval.getContig();
    }

    @Override
    public int getStart() { return interval.getStart(); }

    @Override
    public int getEnd() { return interval.getEnd(); }

    public boolean isLast() { return isLast; }



    /**
     * Check if the position is present in this window.
     *
     * @param contig chromosome to check.
     * @param start start coordinate for the position.
     * @return {@code true} if the position overlaps the window; {@code false} otherwise.
     */
    public boolean isInWin(final String contig, final int start) {
        // simplification of previously implemented isMateInWin and isInWin
        return new SimpleFeature(contig, start, start).overlaps(interval);
    }


    /**
     * Add 1 to the total
     */
    private void addTotal() { total++; }

    /**
     * Add 1 to proper
     */
    private void addProper() { proper++; }

    /**
     * Add 1 to soft clip
     */
    private void addSoftClip() {	softclip++; }

    /**
     * Add 1 to indels
     */
    private void addIndel() { indel++; }

    /**
     * Add some values to the values in the window
     *
     * @param vals	values to add
     * @param times	times to add them
     */
    private void addValues(int[] vals, int times) {
        for(int n = 0; n < vals.length; n++ ) {
            this.values[n]+=vals[n]*times;
        }
    }


    public void addValues(int[] vals) {
        addValues(vals, 1);
    }

    /**
     * Add the record to the window an perform all the operations
     *
     * @param record	Record to add
     * @param proper	If is proper pair
     * @param values	The values for the record
     * @param softclip	If is softclip
     * @param indel	If is indel
     */
    public void addRecord(SAMRecord record, boolean proper, Boolean[] values, boolean softclip, boolean indel) {
        // add to the total
        addTotal();
        // if is proper, perform the rest
        if(proper) {
            // add to the proper
            addProper();
            // add to soft clip and indel if performing
            if(softclip) addSoftClip();
            if(indel) addIndel();
            // if visited
            if(visited.containsKey(record.getReadName())) {
                // Add the values
                addValues(getVisitedValue(record.getReadName(), values), 2);
                // remove from the hash
                removeVisited(record.getReadName());
                // if not add to the visited only if the mate is downstream
            } else {
                if(RecordOperation.isMateDownstream(record)) visited.put(record.getReadName(), values);
            }
        }
    }

    /**
     * Remove the record from visited
     *
     * @param readName Record name to remove
     */
    public void removeVisited(final String readName) {
        visited.remove(readName);
    }

    /**
     * Update the window for a mate. Return the values for the updated record; array of 0s otherwise
     *
     * @param readName	Record name to update the mate
     * @param values	Values from this record
     * @return	values for this record-mate pair
     */
    public int[] mateUpdate(String readName, Boolean[] values) {
        int[] vals = getVisitedValue(readName, values);
        addValues(vals, 1);
        removeVisited(readName);
        return vals;
    }

    /**
     * Check if the visited is empty.
     *
     * @return tru if visited is empty; false otherwise
     */
    public boolean visitedEmpty() {
        return visited.isEmpty();
    }

    /**
     * Get the visited values for a record
     *
     * @param readName	Record name to recover the mate in the window
     * @param values	The values for the record
     * @return	The values for this window
     */
    private int[] getVisitedValue(String readName, Boolean[] values) {
        Boolean[] vals = visited.get(readName);
        int[] return_vals = new int[this.values.length];
        Arrays.fill(return_vals, 0);
        if(vals != null) {
            for(int n = 0; n < vals.length; n++ ) {
                if(vals[n] && values[n]) return_vals[n]++;
            }
        }
        return return_vals;
    }

    @Override
    public String toString() {
        // Warning if the visited is not empty
        if(!visited.isEmpty()) {
            logger.warn("{} proper reads with missing pairs in the file at {}", visited.size(), IntervalUtils.locatableToString(interval));
        }
        StringBuilder builder = new StringBuilder();
        builder.append(getContig()); builder.append("\t");
        builder.append(getStart()); builder.append("\t");
        builder.append(getEnd()); builder.append("\t");
        builder.append(total);				builder.append("\t");
        builder.append(proper);
        for(int val: values) {
            builder.append("\t");			builder.append(val);
        }
        if(sc)  { builder.append("\t");		builder.append(softclip); }
        if(ind) { builder.append("\t");		builder.append(indel);    }

        return builder.toString();
    }
}

