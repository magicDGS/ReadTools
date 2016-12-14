package org.magicdgs.readtools.tools.trimming;

import org.magicdgs.readtools.utils.tests.CommandLineProgramTest;
import org.magicdgs.readtools.utils.tests.TestResourcesUtils;

import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class TrimFastqIntegrationTest extends CommandLineProgramTest {

    // temp directory for all the tests
    private final static File TEST_TEMP_DIR =
            createTestTempDir(TrimFastqIntegrationTest.class.getSimpleName());

    /** Returns an argument builder with the required arguments for all the tests. */
    private static ArgumentsBuilder getRequiredArguments() {
        return new ArgumentsBuilder().addArgument("input1", SMALL_FASTQ_1.getAbsolutePath());
    }

    @DataProvider(name = "barArguments")
    public Object[][] getBadArguments() {
        return new Object[][] {
                {"badQual", getRequiredArguments().addArgument("quality-threshold", "-1")},
                {"batMinLength", getRequiredArguments().addArgument("minimum-length", "-1")},
                {"badMaxLength", getRequiredArguments().addArgument("maximum-length", "-1")},
                {"badLengthRange", getRequiredArguments().addArgument("minimum-length", "80")
                        .addArgument("maximum-length", "10")}

        };
    }

    @Test(dataProvider = "barArguments", expectedExceptions = CommandLineException.BadArgumentValue.class)
    public void testBadArguments(final String testName, final ArgumentsBuilder builder)
            throws Exception {
        builder.addArgument("output", new File(TEST_TEMP_DIR, testName).getAbsolutePath());
        runCommandLine(builder);
        log("Exception not thrown for " + testName);
    }

    @DataProvider(name = "TrimmingData")
    public Object[][] getTrimmingData() throws Exception {
        return new Object[][] {
                // test default arguments, with both pair-end and single-end data
                {"testTrimmingSingleEndDefaultParameters", getRequiredArguments(),
                        false, false},
                {"testTrimmingPairEndDefaultParameters", getRequiredArguments()
                        .addArgument("input2", SMALL_FASTQ_2.getAbsolutePath()),
                        true, false},
                // test keep discarded
                {"testTrimmingSingleEndDefaultParameters", getRequiredArguments(),
                        false, true},
                {"testTrimmingPairEndDefaultParameters", getRequiredArguments()
                        .addArgument("input2", SMALL_FASTQ_2.getAbsolutePath()),
                        true, true},
                // test lower mapping quality
                {"testTrimmingSingleEndLowQualityThreshold", getRequiredArguments()
                        .addArgument("quality-threshold", "18"),
                        false, true},
                // test with length range
                {"testTrimmingSingleEndLengthRange", getRequiredArguments()
                        .addArgument("minimum-length", "60")
                        .addArgument("maximum-length", "75"),
                        false, true},
                // test discard internal ns
                {"testTrimmingSingleEndDiscardInternalN", getRequiredArguments()
                        .addBooleanArgument("discard-internal-N", true),
                        false, true},
                // test trimming Ns, discarding internal Ns and no 5 primer
                {"testTrimmingSingleEndNo5p", getRequiredArguments()
                        .addBooleanArgument("no-5p-trim", true),
                        false, true},
                // test no trimming quality
                {"testTrimmingSingleEndNoQuality", getRequiredArguments()
                        .addBooleanArgument("no-trim-quality", true),
                        false, false}
        };
    }

    @Test(dataProvider = "TrimmingData")
    public void testTrimFastq(final String testName, final ArgumentsBuilder builder,
            final boolean pairEnd, final boolean keepDiscarded) throws Exception {
        final String testOutputName = testName + ((keepDiscarded) ? "KeepDiscarded" : "");
        log("Running " + testOutputName);
        // gets the output prefix and add to command line
        final File outputPrefix = new File(TEST_TEMP_DIR, testOutputName);
        final ArgumentsBuilder args = builder
                .addArgument("output", outputPrefix.getAbsolutePath())
                .addBooleanArgument("keep-discarded", keepDiscarded);
        // running the command line
        runCommandLine(args);
        // check the metrics file
        checkFiles(testName, outputPrefix.getAbsolutePath(), ".metrics");
        final String[] suffixes = (pairEnd)
                ? new String[] {"_1.fq.gz", "_2.fq.gz", "_SE.fq.gz"}
                : new String[] {".fq.gz"};
        for (final String suffix : suffixes) {
            checkFiles(testName, outputPrefix.getAbsolutePath(), suffix);
            if (keepDiscarded) {
                checkFiles(testName, outputPrefix.getAbsolutePath(), "_discarded" + suffix);
            } else {
                final File discardedFile =
                        new File(outputPrefix.getAbsolutePath() + "_discarded" + suffix);
                Assert.assertFalse(discardedFile.exists(),
                        discardedFile + " exists for no keep-discarded");
            }
        }
    }

    private void checkFiles(final String testName, final String outPath, final String suffix)
            throws Exception {
        logger.debug("Checking output: {}{}", testName, suffix);
        IntegrationTestSpec.assertEqualTextFiles(new File(outPath + suffix),
                new File(TestResourcesUtils.getReadToolsTestResource("org/magicdgs/readtools/"
                        + getTestedClassName()), testName + suffix));
    }

    @Test
    public void testTrimOnlyNdata() throws Exception {
        final File outputPrefix = new File(TEST_TEMP_DIR, "testTrimOnlyNdata");
        final File expectedOutput = new File(outputPrefix.getAbsolutePath() + ".fq");
        Assert.assertFalse(expectedOutput.exists());
        runCommandLine(new ArgumentsBuilder()
                .addFileArgument("output", outputPrefix)
                .addFileArgument("input1", getInputDataFile("onlyN.fq"))
                .addBooleanArgument("disable-zipped-output", true));
        Assert.assertTrue(expectedOutput.exists());
        assertFileIsEmpty(expectedOutput);
    }

}