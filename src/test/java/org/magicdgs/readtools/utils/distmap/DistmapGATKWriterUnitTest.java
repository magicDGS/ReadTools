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

package org.magicdgs.readtools.utils.distmap;

import org.magicdgs.readtools.utils.read.ReadWriterFactory;
import org.magicdgs.readtools.utils.tests.BaseTest;

import org.apache.commons.io.output.NullWriter;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.GATKReadWriter;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.function.Supplier;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class DistmapGATKWriterUnitTest extends BaseTest {

    private static final File TEST_TMP_DIR = createTestTempDir("DistmapGATKWriterUnitTest");

    private static final GATKRead READ_1 = ArtificialReadUtils.createArtificialUnmappedRead(
            null, new byte[] {'A', 'T', 'C', 'G'}, new byte[] {33, 0, 0, 33});

    private static final GATKRead READ_2 = ArtificialReadUtils.createArtificialUnmappedRead(
            null, new byte[] {'G', 'C', 'T', 'A'}, new byte[] {0, 33, 33, 0});

    static {
        READ_1.setName("readName");
        READ_1.setAttribute("BC", "TTTT");
        READ_2.setName("readName");
    }

    @DataProvider
    public Object[][] distmapFiles() {
        return new Object[][] {
                {getTestFile("single_end.distmap"), false},
                {getTestFile("pair_end.distmap"), true},
                {getTestFile("pair_end.distmap.bz2"), true}
        };
    }

    @Test(dataProvider = "distmapFiles")
    public void testWriter(final File expected, final boolean paired) throws Exception {
        final File actual = new File(TEST_TMP_DIR, expected.getName());
        final GATKReadWriter writer = new ReadWriterFactory()
                .createDistmapWriter(actual.getAbsolutePath(), paired);
        writer.addRead(READ_1);
        writer.addRead(READ_2);
        writer.close();
        IntegrationTestSpec.assertEqualTextFiles(actual, expected);
    }

    @Test(expectedExceptions = DistmapException.class)
    public void testCloseBroken() throws Exception {
        final File broken = new File(TEST_TMP_DIR, "broken.distmap");
        final GATKReadWriter writer = new ReadWriterFactory()
                .createDistmapWriter(broken.getAbsolutePath(), true);
        writer.addRead(READ_1);
        writer.close();
    }

    @DataProvider(name = "userExceptions")
    public Object[][] userExceptionCachedWhilePrinting() {
        return new Object[][] {
                {new IOException()},
                {new DistmapException("test")}
        };
    }

    @Test(dataProvider = "userExceptions", expectedExceptions = UserException.CouldNotCreateOutputFile.class)
    public void testUserExceptionWhilePrinting(final Exception exception) throws Exception {
        // creates a mocked writer throwing IOExceptions
        final Writer ioExceptionWriter = Mockito.mock(Writer.class,
                (Answer) (invocation -> {throw exception;}));
        // check if it throws
        new DistmapGATKWriter(ioExceptionWriter, "test", true).printAndCheckError(() -> null);
    }

}