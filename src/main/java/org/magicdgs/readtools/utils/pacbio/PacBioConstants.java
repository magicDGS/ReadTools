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

import htsjdk.samtools.SAMFileHeader;

/**
 * Constants and documentation from
 * <a href="http://pacbiofileformats.readthedocs.io/en/3.0/BAM.html">PacBio BAM format 3.0</a>.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class PacBioConstants {

    /**
     * File extension for ZMW reads from movie.
     *
     * <p>Note: prefix should be the movie name.
     */
    public static final String ZMW_READS_FILE_EXTENSION = ".zmws.bam";

    /**
     * File extension for subreads from movie. Data in this file should be analysis ready, meaning
     * that all of the data present is expected to be useful for down-stream analyses.
     *
     * Any subreads for which we have strong evidence will not be useful (e.g. double-adapter
     * inserts, single-molecule artifacts) should be excluded from this file and placed in scraps
     * file (with {@link #SCRAPS_FILE_EXTENSION} as a Filtered with an SC tag of F.
     *
     * <p>Note: prefix should be the movie name.
     */
    public static final String SUBREADS_FILE_EXTENSION = ".subreads.bam";

    /**
     * File extension for excised adapters, barcodes, and rejected subreads.
     *
     * <p>Note: prefix should be the movie name.
     */
    public static final String SCRAPS_FILE_EXTENSION = ".scraps.bam";

    /** File extension for CCS reads computed from movie.
     *
     * <p>Note: prefix should be the movie name.
     */
    public static final String CSS_READS_FILE_EXTENSION = ".css.bam";


    /**
     * Unaligned PacBio BAM files shall be sorted by QNAME, so that all subreads from a ZMW hole
     * are stored contiguously in a file, with groups by ZMW hole number in numerical order, and
     * within a ZMW, numerically by qStart.
     *
     * <p>In case subreads and CCS reads are combined in a BAM, the CCS reads will sort after the
     * subreads (ccs follows {qStart}_{qEnd}). Note that this sorting is not strictly alphabetical,
     * so we shall set the BAM @HD::SO tag to unknown.
     */
    // TODO: I don't know if this will be necessary
    public static final SAMFileHeader.SortOrder SUBREADS_AND_CSS_SUBORDER = SAMFileHeader.SortOrder.unknown;



}
