package org.magicdgs.readtools.tools.mappability.gem;

import org.magicdgs.readtools.RTCommandLineProgramTest;
import org.magicdgs.readtools.TestResourcesUtils;
import org.magicdgs.readtools.exceptions.RTUserExceptions;
import org.magicdgs.readtools.utils.mappability.gem.GemMappabilityReaderUnitTest;

import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.IOUtil;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GemMappabilityToBedIntegrationTest extends RTCommandLineProgramTest {

    private final File EXAMPLE_FILE = TestResourcesUtils.getReadToolsTestResource(
            GemMappabilityReaderUnitTest.class.getPackage().getName().replace(".", "/") +
                    "/GemMappabilityReader/example.gem.mappability");

    private final File REAL_SUBSET =
            new File(getClassTestDirectory().getParent(), "real_subset.gem.mappability");

    @DataProvider
    public Object[][] arguments() {
        return new Object[][] {
                // example file (easier to check manually)
                {EXAMPLE_FILE, GemScoreMethod.MAX, getTestFile("example.gem.mappability.max.bed")},
                {EXAMPLE_FILE, GemScoreMethod.MIN, getTestFile("example.gem.mappability.min.bed")},
                {EXAMPLE_FILE, GemScoreMethod.MID, getTestFile("example.gem.mappability.mid.bed")},
                // subset for a real sample (only one contig) and compressed output
                {REAL_SUBSET, GemScoreMethod.MAX, getTestFile("real_subset.gem.mappability.max.bed.gz")},
                {REAL_SUBSET, GemScoreMethod.MIN, getTestFile("real_subset.gem.mappability.min.bed.gz")},
                {REAL_SUBSET, GemScoreMethod.MID, getTestFile("real_subset.gem.mappability.mid.bed.gz")}
        };
    }

    @Test(dataProvider = "arguments")
    public void testSimpleMappabilityFile(final File input, final GemScoreMethod method,
            final File expected) throws Exception {
        final File output = getSafeNonExistentFile(expected.getName());

        final ArgumentsBuilder args = new ArgumentsBuilder()
                .addFileArgument("input", input)
                .addArgument("score-method", method.name())
                .addFileArgument("output", output);

        runCommandLine(args);

        IntegrationTestSpec.assertEqualTextFiles(output, expected);

        // finally, test if compression works as expected
        try (final InputStream bis = new BufferedInputStream(new FileInputStream(output))) {
            Assert.assertEquals(
                    BlockCompressedInputStream.isValidFile(bis),
                    IOUtil.hasBlockCompressedExtension(output));
        }
    }

    @Test(expectedExceptions = UserException.CouldNotReadInputFile.class)
    public void testCouldNotReadInputFile() throws Exception {
        runCommandLine(new String[]{
            "--input", getSafeNonExistentFile("nonExistent.gem.mappability").getAbsolutePath(),
            "--output", getSafeNonExistentFile("nonExistent.gem.bed").getAbsolutePath()
        });
    }

    @Test(expectedExceptions = RTUserExceptions.OutputFileExists.class)
    public void testOutputFileExists() throws Exception {
        runCommandLine(new ArgumentsBuilder()
                .addFileArgument("input", EXAMPLE_FILE)
                .addFileArgument("output", createTempFile("exists", ".bed.gz")));
    }

    @Test
    public void testForceOverwrite() throws Exception {
        Assert.assertNull(
            runCommandLine(new ArgumentsBuilder()
                    .addFileArgument("input", EXAMPLE_FILE)
                    .addFileArgument("output", createTempFile("exists", ".bed.gz"))
                    .addBooleanArgument("forceOverwrite", true))
        );
    }
}
