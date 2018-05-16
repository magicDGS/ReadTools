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

import htsjdk.samtools.SAMRecord;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * TODO: remove class
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class Threshold {

    private String tag;
    private Operations operation;
    private int threshold;

    private static enum Operations {
        lower, bigger;

        @Override
        public String toString() {
            switch(this) {
                case lower: return "<";
                case bigger: return ">";
                default: throw new IllegalArgumentException("Invalid operation");
            }
        }

        public static Operations toOperation(String op) {
            if(op.equals(">")) return bigger;
            else if(op.equals("<")) return lower;
            else throw new IllegalArgumentException("Invalid operation");
        }
    }

    public Threshold() { }

    /**
     *
     * @param args String array in the form tag, operation, threshold
     * @throws Exception
     */
    public Threshold(String tag, String operation, String threshold) {
        ThresholdConstructor(tag, operation, threshold);
    }

    public Threshold(String threshold_string) {
        String split = "";
        if(threshold_string.contains(">")) {
            split = ">";
        } else if(threshold_string.contains("<")) {
            split = "<";
        } else {
            throw new IllegalArgumentException("Malformed threshold:"+threshold_string);
        }
        String[] arg = threshold_string.split(split);
        ThresholdConstructor(arg[0], split, arg[1]);
    }

    private void ThresholdConstructor(String tag, String operation, String threshold) {
        try {
            this.tag = tag;
            this.operation = Operations.toOperation(operation);
            this.threshold = Integer.parseInt(threshold);
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operation: "+operation);
        }
    }

    /**
     *
     * @param record	First pair
     * @param mate	Second pair
     * @return	1 if the pair fullfill the threshold; 0 otherwise
     */
    public boolean operate(SAMRecord record, SAMRecord mate) {
        if(isInThreshold(record) && isInThreshold(mate)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @param record	One SAM record
     * @return	true if falls under/over the threshold; false otherwise
     */
    public boolean isInThreshold(SAMRecord record) {
        if(operation.equals(Operations.bigger)) return record.getIntegerAttribute(tag) > threshold;
        if(operation.equals(Operations.lower)) return record.getIntegerAttribute(tag) < threshold;
        return false;
    }


    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(tag);
        builder.append(operation);
        builder.append(threshold);
        return builder.toString();
    }
}

