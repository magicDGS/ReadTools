/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
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
 */
package org.vetmeduni.io;

import htsjdk.samtools.fastq.FastqRecord;

/**
 * Class for store two pairs of records, for pair-end read
 *
 * @author Daniel Gómez-Sánchez
 */
public class FastqPairedRecord {

	private final FastqRecord record1;

	private final FastqRecord record2;

	/**
	 * Constructor for two records
	 *
	 * @param record1 the first record
	 * @param record2 the second record
	 */
	public FastqPairedRecord(FastqRecord record1, FastqRecord record2) {
		this.record1 = record1;
		this.record2 = record2;
	}

	/**
	 * Get the first record
	 *
	 * @return the first record
	 */
	public FastqRecord getRecord1() {
		return record1;
	}

	/**
	 * Get the second record
	 *
	 * @return the second record
	 */
	public FastqRecord getRecord2() {
		return record2;
	}

	/**
	 * Check if at least one of the records contain information
	 *
	 * @return <code>true</code> if at least one the records is not <code>null</code>; <code>false</code> otherwise
	 */
	public boolean containRecords() {
		return !(record1 == null && record2 == null);
	}

	/**
	 * Chek if the record contain both record1 and record2
	 *
	 * @return <code>true</code> if both records are not <code>null</code>; <code>false</code> otherwise
	 */
	public boolean isComplete() {
		return record1 != null && record2 != null;
	}

	@Override
	public String toString() {
		return String.format("%s\n%s", record1.toString(), record2.toString());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FastqPairedRecord other = (FastqPairedRecord) obj;
		return record1.equals(other.record1) && record2.equals(other.record2);
	}

	@Override
	public int hashCode() {
		int result = record1 != null ? record1.hashCode() : 0;
		result = 31 * result + (record2 != null ? record2.hashCode() : 0);
		return result;
	}
}
