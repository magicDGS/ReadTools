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

package org.magicdgs.readtools.engine;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadToolsProgramUnitTest {

    private static class TestProgram extends ReadToolsProgram {
        @Override
        protected Object doWork() {
            return null;
        }
    }

    @Test
    public void testGetProgramRecord() {
        final ReadToolsProgram program = new TestProgram();
        final String programName = "ReadTools TestProgram";

        // test the program record for an empty header
        final SAMFileHeader header = new SAMFileHeader();
        SAMProgramRecord pg0 = program.getProgramRecord(header);
        Assert.assertEquals(pg0.getId(), programName);
        Assert.assertEquals(pg0.getProgramName(), programName);

        // test adding more program records
        for (int i = 1; i < 5; i++) {
            header.addProgramRecord(pg0);
            pg0 = program.getProgramRecord(header);
            Assert.assertEquals(pg0.getId(), programName + "." + i);
            Assert.assertEquals(pg0.getProgramName(), programName);
        }
    }

}