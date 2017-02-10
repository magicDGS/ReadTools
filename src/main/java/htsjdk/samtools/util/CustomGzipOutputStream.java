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

package htsjdk.samtools.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Hacky little class used to allow us to set the compression level on a GZIP output stream which, for some
 * bizarre reason, is not exposed in the standard API.
 *
 * @author Tim Fennell
 */
// TODO: this should be removed after https://github.com/samtools/htsjdk/pull/798
public class CustomGzipOutputStream extends GZIPOutputStream {
    public CustomGzipOutputStream(final OutputStream outputStream, final int bufferSize, final int compressionLevel) throws
            IOException {
        super(outputStream, bufferSize);
        this.def.setLevel(compressionLevel);
    }

    public CustomGzipOutputStream(final OutputStream outputStream, final int compressionLevel) throws IOException {
        super(outputStream);
        this.def.setLevel(compressionLevel);
    }
}