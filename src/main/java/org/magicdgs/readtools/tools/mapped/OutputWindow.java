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
    // TODO: this should be done with a simple interval?
    private String reference;
    private int winStart;
    private int winEnd;
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
        reference = ref;
        winStart = start;
        winEnd = end;
        int hard_end = headerContext.getSequence(ref).getSequenceLength();
        if(hard_end < end) {
            winEnd = hard_end;
            isLast = true;
        } else {
            isLast = false;
        }
        if(winStart > winEnd) {
            throw new IllegalArgumentException("Window cannot have a length less than 0 (end > start)");
        }

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
        return reference;
    }

    @Override
    public int getStart() { return winStart; }

    @Override
    public int getEnd() { return winEnd; }

    public boolean isLast() { return isLast; }

    /**
     * Check if the record is in the window
     *
     * @param record	Record to know if is in the windows
     * @return	True if in window; false otherwise
     */
    public boolean isInWin(SAMRecord record) {
        // TODO: maybe this can be modified with SimpleInterval methods - not covered by small real data
        return record.getAlignmentStart() >= getStart() && record.getAlignmentStart() <= getEnd() && record.getReferenceName().equals(getContig());
    }

    /**
     * Check if the record mate is in the window
     *
     * @param record	Record to know if mate is in the windows
     * @return	True if in window; false otherwise
     */
    public boolean isMateInWin(SAMRecord record) {
        // TODO: we should use the SimpleInterval method - not covered by small real data
        // TODO: I believe that this is wrong in the original implementation
        // TODO: it should be record.getMateReferenceName instead of the reference name
        // TODO: it might be the same, because only proper pairs are passing through (I guess)
        return record.getMateAlignmentStart() >= getStart() && record.getMateAlignmentStart() <= getEnd() && record.getReferenceName().equals(getContig());
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
     * Check if the record is visited
     * @param record
     * @return	true if is already visited; false otherwise
     */
    public boolean isVisited(SAMRecord record) {
        if(visited.containsKey(record.getReadName())) return true;
        return false;
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
            if(isVisited(record)) {
                // Add the values
                addValues(getVisitedValue(record, values), 2);
                // remove from the hash
                removeVisited(record);
                // if not add to the visited only if the mate is downstream
            } else {
                if(RecordOperation.isMateDownstream(record)) visited.put(record.getReadName(), values);
            }
        }
    }

    /**
     * Remove the record from visited
     *
     * @param record Record to remove
     */
    public void removeVisited(SAMRecord record) {
        visited.remove(record.getReadName());
    }

    /**
     * Update the window for a mate. Return the values for the updated record; array of 0s otherwise
     *
     * @param record	Record to update the mate
     * @param values	Values from this record
     * @return	values for this record-mate pair
     */
    public int[] mateUpdate(SAMRecord record, Boolean[] values) {
        int[] vals = getVisitedValue(record, values);
        addValues(vals, 1);
        removeVisited(record);
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
     * @param record	Record to recover the mate in the window
     * @param values	The values for the record
     * @return	The values for this window
     */
    private int[] getVisitedValue(SAMRecord record, Boolean[] values) {
        Boolean[] vals = visited.get(record.getReadName());
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
            logger.warn("{} proper reads with missing pairs in the file at {}:{}-{}", visited.size(), reference, winStart, winEnd);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(reference); builder.append("\t");
        builder.append(winStart); builder.append("\t");
        builder.append(winEnd); builder.append("\t");
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

