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
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * TODO: remove!
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class Window {
    private String reference;
    private int winStart;
    private int winEnd;
    private boolean isLast;

    /**
     * Constructor
     * @param ref	Reference sequence
     * @param start	Start of the window
     * @param end	End of the window
     * @param headerContext	Context for the reference (header of the BAM file)
     */
    public Window(String ref, int start, int end, SAMFileHeader headerContext) throws IllegalArgumentException{
        this(ref, start, end);
        int hard_end = headerContext.getSequence(ref).getSequenceLength();
        if(hard_end < end) {
            winEnd = hard_end;
            isLast = true;
        } else {
            isLast = false;
        }
        checkLength();
    }

    /**
     * Private constructor
     *
     * @param ref	Reference sequence
     * @param start	Start of the window
     * @param end	End of the window
     */
    private Window(String ref, int start, int end) {
        reference = ref;
        winStart = start;
        winEnd = end;
    }

    private void checkLength() {
        if(winStart > winEnd) {
            throw new IllegalArgumentException("Window cannot have a length less than 0 (end > start)");
        }
    }

    public String getRef() { return reference; }

    public int getStart() { return winStart; }

    public int getEnd() { return winEnd; }

    public boolean isLast() { return isLast; }

    /**
     * Check if the record is in the window
     *
     * @param record	Record to know if is in the windows
     * @return	True if in window; false otherwise
     */
    public boolean isInWin(SAMRecord record) {
        return record.getAlignmentStart() >= winStart && record.getAlignmentStart() <= winEnd && record.getReferenceName().equals(reference);
    }

    /**
     * Check if the record mate is in the window
     *
     * @param record	Record to know if mate is in the windows
     * @return	True if in window; false otherwise
     */
    public boolean isMateInWin(SAMRecord record) {
        // TODO: I believe that this is wrong in the original implementation
        // TODO: it should be record.getMateReferenceName instead of the reference name
        // TODO: it might be the same, because only proper pairs are passing through (I guess)
        return record.getMateAlignmentStart() >= winStart && record.getMateAlignmentStart() <= winEnd && record.getReferenceName().equals(reference);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(reference); builder.append("\t");
        builder.append(winStart); builder.append("\t");
        builder.append(winEnd);
        return builder.toString();
    }

    public String toIntervalString() {
        StringBuilder builder = new StringBuilder();
        builder.append(reference); builder.append(":");
        builder.append(winStart); builder.append("-");
        builder.append(winEnd);
        return builder.toString();
    }

    public boolean equals(Window that) {
        return (this.winStart == that.winStart) && (this.winEnd == that.winEnd) && (this.reference == that.reference);
    }
}
