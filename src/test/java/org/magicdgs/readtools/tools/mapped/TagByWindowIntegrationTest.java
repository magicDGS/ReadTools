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

import org.magicdgs.readtools.RTCommandLineProgramTest;

import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TagByWindowIntegrationTest extends RTCommandLineProgramTest {

    // TODO: do not enable because it won't be longer true that the data is the same
    @Test(enabled = false)
    public void testLargeData() throws Exception {
        final File input = new File("/Volumes/backup2/BISCUTELLA_2014/mapped/raw/A-cntrl_L1-12_n001_l200.bam");
        final File expectedOutput = new File("tmp/A-cntrl_L1-12_n001_l200.1000.old.tab");

        // TODO: test large data can take ages (maybe it will break the test)
        final IntegrationTestSpec specs = new IntegrationTestSpec(
                String.format("--input %s --window-size %d --soft-clip %s --count-indel %s --no-last-empty %s ",
                        input, 1000, true, true, false) +
                        makeTagArgument(Arrays.asList("NM:2", "NM:3", "NM:4"), "--tag-count-greater-than") +
                        " --verbosity INFO " +
                        " -O %s", // indication of one output
                Collections.singletonList(expectedOutput.getAbsolutePath()));

        specs.executeTest("largeTest", this);
    }

    @DataProvider
    public Object[][] arguments() {
        return new Object[][] {
                // TODO: create small data instead of this tmp
                // TODO: this is only for checking that the concordance is not broken in the local computer
                {
                    new File("tmp/small_tag.2contigs.bam"),
                    Arrays.asList("NM:2", "NM:3", "NM:4"),
                    Collections.emptyList(),
                    1000, true, true, false,
                    new File("tmp/small_tag.2contigs.old.tab")
                },
                // TODO: create small data instead of this tmp
                // TODO: this is only for checking that the concordance is not broken in the local computer
                {
                        new File("tmp/small_tag.2contigs.bam"),
                        Collections.emptyList(),
                        Arrays.asList("NM:2", "NM:3", "NM:4"),
                        1000, true, true, false,
                        new File("tmp/small_tag.2contigs.old.lt.tab")
                },
                // tmp/small_tag.2contigs.old.all_no_indel_sc.tab
                {
                        new File("tmp/small_tag.2contigs.bam"),
                        Arrays.asList("NM:2", "NM:3", "NM:4"),
                        Collections.emptyList(),
                        1000, false, false, true,
                        new File("tmp/small_tag.2contigs.old.all_no_indel_sc.tab")
                },
        };
    }

    @Test(dataProvider = "arguments")
    public void testTagByWindow(final File input, final List<String> largerThan,
            final List<String> lowerThan,
            final int windowSize, final boolean softclip, final boolean indel,
            final boolean noLastEmpty, final File expectedOutput) throws Exception {
        final IntegrationTestSpec specs = new IntegrationTestSpec(
                String.format("--input %s --window-size %d --soft-clip %s --count-indel %s --no-last-empty %s ",
                        input, windowSize, softclip, indel, noLastEmpty) +
                makeTagArgument(largerThan, "--tag-count-greater-than") + makeTagArgument(lowerThan, "--tag-count-lower-than") +
                " -verbosity INFO" +
                " -O %s", // indication of one output
                Collections.singletonList(expectedOutput.getAbsolutePath()));

        specs.executeTest("concordantTest", this);
    }


    private static final String makeTagArgument(final List<String> strings, final String arg) {
        if (strings.isEmpty()) {
            return "";
        }
        return strings.stream().collect(Collectors.joining(" " + arg + " ", " " + arg + " ", ""));
    }
}