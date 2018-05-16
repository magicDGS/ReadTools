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

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.engine.ReadToolsProgram;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.CloserUtil;
import org.broadinstitute.barclay.argparser.Advanced;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.BetaFeature;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.programgroups.ReadDataProgramGroup;
import org.broadinstitute.hellbender.engine.ReadsDataSource;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.runtime.ProgressLogger;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Copied as it was (very bad implementation) with small modifications to be able to integrate with
 * barclay and GATKRead.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: improve docs
@CommandLineProgramProperties(oneLineSummary = "Count the number of proper read-pairs for an integer tag and a cutoff.",
        summary = "Count the number of read-pairs that falls under/over a cutoff for any integer tag (only proper-pairs -> mate map in the same chromosome).",
        programGroup = ReadDataProgramGroup.class) // TODO: add program group for mapped files
@BetaFeature // TODO: this should be experimental, which isn't integrated into the GATK version used yet
// TODO: this should probably be a converted to a SlidingWindowReadWalker
public final class TagByWindow extends ReadToolsProgram {

    // TODO: maybe a different kind of argument would be necessary
    @Argument(fullName = RTStandardArguments.INPUT_LONG_NAME, shortName = RTStandardArguments.INPUT_SHORT_NAME, doc = "BAM file to have the statistic (indexed)")
    public String input;

    @Argument(fullName = RTStandardArguments.OUTPUT_LONG_NAME, shortName = RTStandardArguments.OUTPUT_SHORT_NAME, doc = "Output tab-delimited file")
    public String outputArg;

    @Argument(fullName = "window-size", doc = "Window size to perform the analysis")
    public Integer window;

    // TODO: tagged areguments would be great here!
    @Argument(fullName = "tag-threshold-definition", doc = "Integer tag and the threshold required. The format is TAG>INT or TAG<threshold. E.g.: NM>2")
    public List<String> tagThresholdDefinition;

    @Argument(fullName = "soft-clip", doc = "Count the number of soft clipped reads in the window")
    public boolean softclip = false;

    @Argument(fullName = "count-indel", doc = "Count the number of reads with insertion/deletions in the window")
    public boolean indel = false;

    @Advanced
    @Argument(fullName = "no-last-empty", doc = "Speed-up the results and avoid last windows in a reference when no more reads are in that contig")
    public boolean EMPTY = true; // TODO: change capital and default value to false (or true, but change flag name)?



    // TODO: change by a ReadsDataSource - and change name
    // public SamReader bam_reader;
    public ReadsDataSource reads;
    // TODO: change PrintWriter for better printer
    public PrintWriter OUT_TAB;
    // TODO: threshold class is not needed! - better a Pair<String, FunctionalInterface> to test
    public static Threshold[] thresholds;
    // TODO: the OutputWindow implementation is shit! use a different stuff for it (e.g., Shard<read> and TableFeature for results)
    // TODO: in addition, all the logic in OutputWindow should be move outside here
    public static Queue<OutputWindow> windowQueue= new LinkedList<>(); // TODO: LinkedList is inefficient!


    @Override
    public String[] customCommandLineValidation() {
        // TODO: super bad implementation!
        reads = new ReadsDataSource(IOUtils.getPath(input));

        try {
            OUT_TAB = new PrintWriter(new FileWriter(outputArg), true);
        } catch (final Exception e) {
            throw new UserException.CouldNotCreateOutputFile(outputArg, e);
        }

        thresholds = tagThresholdDefinition.stream().map(Threshold::new).toArray(Threshold[]::new);

        return super.customCommandLineValidation();
    }

    @Override
    protected Object doWork() {
        try {
            logger.info("Starting TagByWindow.");
            SAMFileHeader header_context = reads.getHeader();

            // initialize some variables and flags
            int start = 1;
            int end = window;
            int unmapped = 0;
            boolean first_read_flag = true;
            boolean sc = false;
            boolean ind = false;
            OutputWindow current_window = new OutputWindow();

            // For logging the progress
            ProgressLogger progress = new ProgressLogger(logger);
            // for each record
            // TODO: changed to use GATKRead to test possible problems due to impl details
            for(GATKRead read: reads) {
                // TODO: conversion to maintain compatibility
                final SAMRecord record = read.convertToSAMRecord(header_context);
                // System.out.println(record);
                // if is unmapped count it and continue to the next
                // TODO: use read.isUnmapped() instead of record.getAlignmentStart() == 0
                if(record.getAlignmentStart() == 0) {
                    unmapped++;
                    progress.record(record);
                    continue;
                }
                // if there are unmmaped, output a warning
                if(unmapped != 0) {
                    logger.warn("Skipped {} unmapped reads.", unmapped);
                    unmapped = 0;
                }
                // get the reference name
                String reference = record.getReferenceName();

                // if is the first read
                if(first_read_flag) {
                    // Print the header for the output
                    PrintTabDelimHeader();
                    // Initialize the current window
                    current_window = new OutputWindow(reference, start, end, header_context, thresholds.length, softclip, indel);
                    // Add the current window to the queue
                    windowQueue.add(current_window);
                    // remove the flag
                    first_read_flag = false;
                    // print the log to initialize
                    logger.info("Analysing {}", reference);
                }

                // if the record is not in this window
                while(!current_window.isInWin(record)) {
                    // if the reference is different form the current window
                    if(!current_window.getRef().equals(reference)) {
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
                        current_window = new OutputWindow(reference, start, end, header_context, thresholds.length, softclip, indel);
                        windowQueue.add(current_window);
                    } catch(IllegalArgumentException e) {
                        break;
                    }
                    // if the record is in the window
                }
                // Now the record is in the window and we could compute all the stuff
                // First if is proper
                boolean prop = RecordOperation.isProper(record);
                // initialize empty values
                boolean[] values;
                if(prop) {
                    // if softclip, compute the clipping
                    if(softclip) sc = RecordOperation.isClip(record);
                    // if indel, compute the indels
                    if(indel) ind = RecordOperation.isIndel(record);
                    // compute the values for the tag
                    values = RecordOperation.tagValueThreshold(record, thresholds);
                } else {
                    sc = false;
                    ind = false;
                    values = new boolean[thresholds.length];
                    Arrays.fill(values, false);
                }
                // add the record to the window
                current_window.addRecord(record, prop, values, sc, ind);
                // if the mate is before
                if(!RecordOperation.isMateDownstream(record)) {
                    // update the queue, storing the values for this window to add
                    int[] cur_vals = updateQueue(record, values);
                    // if the mate is not in this window, add the values (if not, is already updated)
                    if(!current_window.isMateInWin(record)) current_window.addValues(cur_vals);
                }
                progress.record(record);
            }
            flushQueue();
            outputLastEmptyWindows(current_window, header_context);
            // Print the final log and exit
            logger.info("Succesfully parsed {} reads.", progress.getCount());
            System.exit(0);
        } finally {
            CloserUtil.close(Arrays.asList(reads, OUT_TAB));
        }
        return null;
    }

    public void outputLastEmptyWindows(OutputWindow lastWindow, SAMFileHeader header_context) {
        String reference = lastWindow.getRef();
        if(!lastWindow.isLast()) {
            if(EMPTY) {
                int start = lastWindow.getStart();
                int end = lastWindow.getEnd();
                while(!lastWindow.isLast()) {
                    start+=window;
                    end+=window;
                    try {
                        lastWindow = new OutputWindow(reference, start, end, header_context, thresholds.length, softclip, indel);
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
    public int[] updateQueue(SAMRecord record, boolean[] values) {
        int iterations = 0;
        int[] return_vals = new int[values.length];
        for(OutputWindow win: windowQueue) {
            iterations++;
            if(iterations < windowQueue.size()) {
                if(win.isMateInWin(record)) {
                    return_vals = win.mateUpdate(record, values);
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
        for(Threshold thr: thresholds) {
            newString.append("\t");
            newString.append(thr.toString());
        }
        if(softclip) newString.append("\tSoftClip");
        if(indel) newString.append("\tIndels");
        OUT_TAB.println(newString);
    }

}
