package org.magicdgs.readtools.utils.tests;

import org.magicdgs.readtools.Main;

import htsjdk.samtools.util.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.utils.LoggingUtils;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.test.CommandLineProgramTester;
import org.testng.Reporter;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.util.List;

/**
 * Base class for command line program testing.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class CommandLineProgramTest implements CommandLineProgramTester {

    // current directory for the tests
    private static final String CURRENT_DIRECTORY = System.getProperty("user.dir");

    /** Logger for the PAToK package for the tests. */
    public static final Logger logger = LogManager.getLogger("org.magicdgs.readtools");

    /** Root file directory for test resources. */
    private static final String TEST_ROOT_FILE_DIRECTORY =
            new File(CURRENT_DIRECTORY, "src/test/resources/").getAbsolutePath() + "/";

    /** Root file directory for common files. */
    public static final String COMMON_TEST_FILE_DIRECTORY =
            new File(TEST_ROOT_FILE_DIRECTORY, "org/magicdgs/readtools").getAbsolutePath() + "/";

    /** Root file directory for large test resources. */
    public static final String LARGE_TEST_FILE_DIRECTORY =
            new File(COMMON_TEST_FILE_DIRECTORY, "large").getAbsolutePath() + "/";

    // TODO: this two files should have barcodes in their names
    /** Test FASTQ file (pair 1). */
    public static final File SMALL_FASTQ_1 = getInputDataFile("SRR1931701_1.fq");
    /** Test FASTQ file (pair 2). */
    public static final File SMALL_FASTQ_2 = getInputDataFile("SRR1931701_2.fq");
    /** Test BAM file (paired). */
    public static final File PAIRED_BAM_FILE = getInputDataFile("SRR1931701.tagged.sam");
    /** Test BAM file (single). */
    public static final File SINGLE_BAM_FILE = getInputDataFile("SRR1931701.single.tagged.sam");

    /** Log this message so that it shows up inline during output as well as in html reports. */
    public static void log(final String message) {
        Reporter.log(message, true);
    }

    /**
     * Gets a common test file in the resources directory.
     */
    public static File getCommonTestFile(final String fileName) {
        return new File(COMMON_TEST_FILE_DIRECTORY, fileName);
    }

    /** Gets input data in the test directory. */
    public static File getInputDataFile(final String fileName) {
        return getCommonTestFile("data/" + fileName);
    }

    /**
     * Gets a large test file in the large directory.
     */
    public static File getLargeTestFile(final String fileName) {
        return new File(LARGE_TEST_FILE_DIRECTORY, fileName);
    }

    /**
     * Creates a temp directory for tests, deleting recursively on exit.
     *
     * @param prefix the prefix for the test directory.
     *
     * @return temp directory file.
     */
    public static File createTestTempDir(final String prefix) {
        final File dir = IOUtils.tempDir(prefix, "");
        IOUtils.deleteRecursivelyOnExit(dir);
        return dir;
    }

    /** All the tests will have only the debug verbosity. */
    @BeforeSuite
    public void setTestVerbosity() {
        LoggingUtils.setLoggingLevel(Log.LogLevel.DEBUG);
    }

    /**
     * The tested tool name should is included in the class name by default, using the format
     * "NameIntegrationTest"
     */
    @Override
    public String getTestedToolName() {
        return getClass().getSimpleName().replaceAll("IntegrationTest$", "");
    }

    /**
     * Gets the test file sited in the default testing directory for tools ({@link
     * #COMMON_TEST_FILE_DIRECTORY}/ToolName/fileName).
     */
    public File getToolTestFile(final String fileName) {
        return new File(COMMON_TEST_FILE_DIRECTORY, getTestedToolName() + "/" + fileName);
    }

    /** Use our main class. */
    @Override
    public Object runCommandLine(final List<String> args) {
        return new Main().instanceMain(makeCommandLineArgs(args));
    }
}
