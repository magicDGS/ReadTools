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

import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.Closeable;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TagByWindowEngine implements Closeable {

    private Logger logger = LogManager.getLogger(this.getClass());

    // initialize some variables and flags
    private int start = 1;
    private int end;
    private int unmapped = 0;
    private boolean first_read_flag = true;
    private boolean sc = false;
    private boolean ind = false;
    // TODO: changed to null, not necessary until construction in the first read
    private OutputWindow current_window = null;

    // parameters
    private final Integer window;
    private final SAMFileHeader header_context;
    private final List<IntTagFunction> operations;
    private final boolean EMPTY;
    private final boolean softclip;
    private final boolean indel;
    private final PrintWriter OUT_TAB;
    // TODO: the OutputWindow implementation is shit! use a different stuff for it (e.g., Shard<read> and TableFeature for results)
    // TODO: in addition, all the logic in OutputWindow should be move outside here
    private final Queue<OutputWindow> windowQueue= new LinkedList<>(); // TODO: LinkedList is inefficient!

    private final boolean compatible;

    public TagByWindowEngine(final Integer window, final SAMFileHeader header_context,
            final List<IntTagFunction> operations, final boolean empty, final boolean softclip,
            final boolean indel,
            final PrintWriter out_tab,
            final boolean compatible) {
        this.window = window;
        this.header_context = header_context;
        this.operations = operations;
        EMPTY = empty;
        this.softclip = softclip;
        this.indel = indel;
        OUT_TAB = out_tab;

        this.end = this.window;
        this.compatible = compatible;
    }

    public static String getContigCopatibility(final GATKRead read, final SAMFileHeader header_context, final boolean compatible) {
        return (compatible) ? read.convertToSAMRecord(header_context).getReferenceName() : read.getContig();
    }

    public static int getStartCompatibility(final GATKRead read, final SAMFileHeader header_context, final boolean compatible) {
        return (compatible) ? read.convertToSAMRecord(header_context).getAlignmentStart() : read.getStart();
    }

    public static String getMateContigCopatibility(final GATKRead read, final SAMFileHeader header_context, final boolean compatible) {
        return (compatible) ? read.convertToSAMRecord(header_context).getMateReferenceName() : read.getMateContig();
    }

    public static int getMateStartCompatibility(final GATKRead read, final SAMFileHeader header_context, final boolean compatible) {
        return (compatible) ? read.convertToSAMRecord(header_context).getMateAlignmentStart() : read.getMateStart();
    }

    public static boolean isUnmappedCompatibility(final GATKRead read, final SAMFileHeader header_context, final boolean compatible) {
        return  (compatible) ? read.convertToSAMRecord(header_context).getAlignmentStart() == 0 : read.isUnmapped();
    }

    /**
     * Check if a record is proper (the mate is mapped in the same reference)
     *
     * @param read	read to check
     * @return true if is proper; false otherwise
     */
    public static boolean isProper(final GATKRead read, final SAMFileHeader header_context, final boolean compatible) {
        if (compatible) {
            final SAMRecord record = read.convertToSAMRecord(header_context);
            return record.getReferenceIndex() == record.getMateReferenceIndex() && (record.getAlignmentStart() != record.getMateAlignmentStart());
        } else {
            return !read.isUnmapped() && !read.mateIsUnmapped() && read.getContig().equals(read.getMateContig());
        }
    }

    public static boolean isMateDownstream(final GATKRead read, final SAMFileHeader header_context, final boolean compatible) {
        return isProper(read, header_context, compatible) && (getMateStartCompatibility(read, header_context, compatible) > getStartCompatibility(read, header_context, compatible));
    }

    public void addRead(final GATKRead read) {
        // if is unmapped count it and continue to the next
        if (isUnmappedCompatibility(read, header_context, compatible)) {
            unmapped++;
            return;
        }
        // if there are unmmaped, output a warning
        if(unmapped != 0) {
            logger.debug("Skipped {} unmapped reads.", unmapped);
            unmapped = 0;
        }
        // get the reference name
        final String reference = getContigCopatibility(read, header_context, compatible);

        // if is the first read
        if(first_read_flag) {
            // Print the header for the output
            PrintTabDelimHeader();
            // Initialize the current window
            current_window = new OutputWindow(reference, start, end, header_context, operations.size(), softclip, indel, compatible);
            // Add the current window to the queue
            windowQueue.add(current_window);
            // remove the flag
            first_read_flag = false;
            // print the log to initialize
            logger.info("Analysing {}", reference);
        }

        // if the record is not in this window
        while(!current_window.isInWin(getContigCopatibility(read, header_context, compatible), getStartCompatibility(read, header_context, compatible))) {
            // if the reference is different form the current window
            if(!current_window.getContig().equals(reference)) {
                // output the complete queue
                flushQueue();
                // output last empty window
                outputLastEmptyWindows(current_window, header_context);
                // change the start and the end
                start=1;
                end=window;
                logger.info("Analysing {}", reference);
            } else {
                // output the queue as much as it could be output
                updateQueue(0);
                // change the start and end
                start+=window;
                end+=window;
            }
            try {
                // create the new current window and add to the queue
                current_window = new OutputWindow(reference, start, end, header_context, operations.size(), softclip, indel, compatible);
                windowQueue.add(current_window);
            } catch(IllegalArgumentException e) {
                break;
            }
            // if the record is in the window
        }

        // Now the record is in the window and we could compute all the stuff
        // First if is proper
        boolean prop = isProper(read, header_context, compatible);
        // initialize empty values
        Boolean[] values;
        if(prop) {
            // if softclip, compute the clipping
            if(softclip) sc = read.getCigarElements().stream().anyMatch(s -> s.getOperator() == CigarOperator.S);
            // if indel, compute the indels
            if(indel) ind = read.getCigarElements().stream().anyMatch(s -> s.getOperator().isIndel());

            // compute the values for the tag
            values = operations.stream().map(s -> s.apply(read)).toArray(Boolean[]::new);
        } else {
            sc = false;
            ind = false;
            values = new Boolean[operations.size()];
            Arrays.fill(values, false);
        }

        // add the record to the window
        current_window.addRecord(read, prop, values, sc, ind);
        // if the mate is before
        if(!isMateDownstream(read, header_context, compatible)) {
            // update the queue, storing the values for this window to add
            int[] cur_vals = updateQueue(read, values);
            // if the mate is not in this window, add the values (if not, is already updated)
            if(!current_window.isInWin(getMateContigCopatibility(read, header_context, compatible), getMateStartCompatibility(read, header_context, compatible))) current_window.addValues(cur_vals);
        }

    }


    public void outputLastEmptyWindows(OutputWindow lastWindow, SAMFileHeader header_context) {
        String reference = lastWindow.getContig();
        if(!lastWindow.isLast()) {
            if(EMPTY) {
                int start = lastWindow.getStart();
                int end = lastWindow.getEnd();
                while(!lastWindow.isLast()) {
                    start+=window;
                    end+=window;
                    try {
                        lastWindow = new OutputWindow(reference, start, end, header_context, operations.size(), softclip, indel, compatible);
                        OUT_TAB.println(lastWindow);
                    } catch(IllegalArgumentException e) {
                        break;
                    }
                }
            } else {
                logger.warn("Skipped empty windows at the end of {}", reference);
            }
        }

    }

    /**
     * Update the queue until it reach some length or it founds one window with mates stored
     *
     * @param length	The minimum length of the queue
     */
    public void updateQueue(int length) {
        while(windowQueue.size() > length && !windowQueue.isEmpty()) {
            if(windowQueue.peek().visitedEmpty()) {
                OUT_TAB.println(windowQueue.poll());
            } else {
                break;
            }
        }
    }

    /**
     * Output to the file all the queue
     */
    public void flushQueue() {
        while(!windowQueue.isEmpty()) OUT_TAB.println(windowQueue.poll());
    }

    /**
     * Update the Queue with a record
     *
     * @param record	The record to update the previous window
     * @param values	The values of this record
     * @return values for the mate upstream
     */
    public int[] updateQueue(GATKRead record, Boolean[] values) {
        int iterations = 0;
        int[] return_vals = new int[values.length];
        for(OutputWindow win: windowQueue) {
            iterations++;
            if(iterations < windowQueue.size()) {
                if(win.isInWin(record.getMateContig(), record.getMateStart())) {
                    return_vals = win.mateUpdate(record.getName(), values);
                    break;
                }
            } else {
                Arrays.fill(return_vals, 0);
                break;
            }
        }
        updateQueue(1);
        return return_vals;
    }

    /**
     * Print the header for the output of this program
     */
    public void PrintTabDelimHeader() {
        StringBuilder newString = new StringBuilder();
        newString.append("Ref\tStart\tEnd\tTotal\tProper");
        operations.forEach(s -> newString.append("\t").append(s.toString()));
        if(softclip) newString.append("\tSoftClip");
        if(indel) newString.append("\tIndels");
        OUT_TAB.println(newString);
    }

    /**
     * Finalize process and close.
     */
    @Override
    public void close() {
        flushQueue();
        outputLastEmptyWindows(current_window, header_context);
        OUT_TAB.close();
    }
}
