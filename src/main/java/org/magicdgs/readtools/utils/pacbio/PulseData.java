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

package org.magicdgs.readtools.utils.pacbio;

import org.magicdgs.readtools.utils.hdf5.HDF5Accessor;

/**
 * Hierarchical set of constants to retrieve objects from a PacBio bas.h5 / bax.h5 file.
 *
 * <p>At the root level, the content of the bas.h5 / bax.h5 file consists of two groups: PulseData
 * and ScanData. Because the ScanData is only for debugging purposes, this is not exposed in our
 * API.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @see HDF5Accessor
 * @see <a href="http://files.pacb.com/software/instrument/2.0.0/bas.h5%20Reference%20Guide.pdf">bash.h5
 * Reference Guide 2.0.0</a>
 */
public class PulseData {

    /** PulseData full path. */
    public static final String fullPath = "/PulseData";

    /**
     * BaseCalls are base metrics produced by the PulseToBase pipeline stage for all pulses
     * classified as base-incorporation events by the basecaller.
     */
    public static final class BaseCalls {

        /** BaseCalls full path. */
        public static final String fullPath = PulseData.fullPath + "/BaseCalls";

        /** Creation date and time of the data group, in ISO 8601 format. */
        // TODO: create a ISO date accessor?
        public static final HDF5Accessor.StringHDF5Accessor DateCreated =
                () -> fullPath + "/DateCreated";

        /**
         * Revision ID of the software that created this data, in format
         * <code><Major>.<Minor>.<Micro>.<Hot fix>.<PacBioChangeNumber></code>.
         */
        public static final HDF5Accessor.StringHDF5Accessor ChangeListID =
                () -> fullPath + "/ChangeListID";

        /** Version or revision number of the group schema. */
        public static final HDF5Accessor.StringHDF5Accessor SchemaRevision =
                () -> fullPath + "/SchemaRevision";

        /** Description of the quality-value encoding scheme. */
        public static final HDF5Accessor.StringHDF5Accessor QVDecoding =
                () -> fullPath + "/QVDecoding";

        /** Content description of the group, as a data set [name, type].*/
        public static final HDF5Accessor.StringHDF5Accessor Content = () -> fullPath + "/Content";

        /** The number of ZMW reads contained in each data set. */
        public static final HDF5Accessor.IntHDF5Accessor CountStored =
                () -> fullPath + "/CountStored";

        /** Called base. */
        public static final HDF5Accessor.ByteHDF5Accessor BaseCall = () -> fullPath + "/BaseCall";

        /** Probability of a deletion error prior to the current base. Phred QV. */
        public static final HDF5Accessor.ByteHDF5Accessor DeletionQV =
                () -> fullPath + "/DeletionQV";

        /** Likely identity of the deleted base, if it exists. */
        public static final HDF5Accessor.ByteHDF5Accessor DeletionTag =
                () -> fullPath + "/DeletionTag";

        /** Probability that the current base is an insertion. Phred QV. */
        public static final HDF5Accessor.ByteHDF5Accessor InsertionQV =
                () -> fullPath + "/InsertionQV";

        /** Probability of a merged-pulse error at the current base. Phred QV. */
        public static final HDF5Accessor.ByteHDF5Accessor MergeQV = () -> fullPath + "/MergeQV";

        /** Duration between the start of a base and the end of the previous base, in Frames. */
        public static final HDF5Accessor.IntHDF5Accessor PreBaseFrames =
                () -> fullPath + "/PreBaseFrames";

        /** Index into called pulses. */
        public static final HDF5Accessor.IntHDF5Accessor PulseIndex =
                () -> fullPath + "/PulseIndex";

        /** Probability of a basecalling error at the current base. Phred QV. */
        public static final HDF5Accessor.ByteHDF5Accessor QualityValue =
                () -> fullPath + "/QualityValue";

        /** Probability of a substitution error at the current base. Phred QV */
        public static final HDF5Accessor.ByteHDF5Accessor SubstitutionQV =
                () -> fullPath + "/SubstitutionQV";

        /** Most likely alternative base. */
        public static final HDF5Accessor.ByteHDF5Accessor SubstitutionTag =
                () -> fullPath + "/SubstitutionTag";

        /** Duration of the base-incorporation event, in Frames. */
        public static final HDF5Accessor.IntHDF5Accessor WidthInFrames =
                () -> fullPath + "/WidthInFrames";


        /** This group includes ZMW information. */
        public static final class ZMW {

            /** ZMW full path. */
            public static final String fullPath = BaseCalls.fullPath + "/ZMW";

            // TODO: implement the accessors for this path

        }

    }


}
