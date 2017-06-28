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

package org.magicdgs.readtools.utils.hdf5;

import org.broadinstitute.hdf5.HDF5File;

/**
 * Interface for retrieve an Object from an HDF5 file.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface HDF5Accessor<T> {

    /** Gets the PacBio object from the file. */
    public T getObjectFromFile(final HDF5File file);

    /** Gets the full path for this object. */
    public String getFullPath();


    /** Interface representing an String Object. */
    public static interface StringHDF5Accessor extends HDF5Accessor<String> {

        @Override
        public default String getObjectFromFile(final HDF5File file) {
            // TODO: should have only one element, or should it be concatenated?
            return file.readStringArray(getFullPath())[0];
        }
    }

    /** Interface representing a Byte Object. */
    public static interface ByteHDF5Accessor extends HDF5Accessor<Byte> {

        @Override
        public default Byte getObjectFromFile(final HDF5File file) {
            return (byte) file.readDouble(getFullPath());
        }
    }

    /** Interface representing any kind of Integer Object (UInt32, Int32, UInt16, Int16...). */
    // TODO: create an accessor for each of the types?
    public static interface IntHDF5Accessor extends HDF5Accessor<Integer> {

        @Override
        public default Integer getObjectFromFile(final HDF5File file) {
            return (int) file.readDouble(getFullPath());
        }
    }

}
