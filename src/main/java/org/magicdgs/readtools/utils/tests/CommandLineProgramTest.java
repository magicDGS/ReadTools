package org.magicdgs.readtools.utils.tests;

import org.magicdgs.readtools.Main;

import htsjdk.samtools.util.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.utils.LoggingUtils;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.test.CommandLineProgramTester;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.util.List;

/**
 * Base class for command line program testing.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class CommandLineProgramTest extends BaseTest implements CommandLineProgramTester {

    /** Logger for the PAToK package for the tests. */
    public static final Logger logger = LogManager.getLogger("org.magicdgs.readtools");

    /** Test FASTQ file (pair 1). */
    public static final File SMALL_FASTQ_1 = getInputDataFile("SRR1931701_1.fq");
    /** Test FASTQ file (pair 2). */
    public static final File SMALL_FASTQ_2 = getInputDataFile("SRR1931701_2.fq");
    /** Test BAM file (paired). */
    public static final File PAIRED_BAM_FILE = getInputDataFile("SRR1931701.tagged.sam");
    /** Test BAM file (single). */
    public static final File SINGLE_BAM_FILE = getInputDataFile("SRR1931701.single.tagged.sam");

    /**
     * Gets input data in the test directory.
     *
     * @deprecated use directly {@link TestResourcesUtils#getReadToolsTestResource(String)}.
     */
    @Deprecated
    public static File getInputDataFile(final String fileName) {
        return TestResourcesUtils.getReadToolsTestResource("org/magicdgs/readtools/data/" + fileName);
    }

    /** All the tests will have only the debug verbosity. */
    @BeforeSuite
    public void setTestVerbosity() {
        LoggingUtils.setLoggingLevel(Log.LogLevel.DEBUG);
    }

    /** @return {@link #getTestedClassName()} */
    @Override
    public String getTestedToolName() {
        return getTestedClassName();
    }

    /** Returns a file in the class test directory with the provided file name. */
    public File getTestFile(final String fileName) {
        return new File(getClassTestDirectory(), fileName);
    }

    /** Use our main class. */
    @Override
    public Object runCommandLine(final List<String> args) {
        return new Main().instanceMain(makeCommandLineArgs(args));
    }
}
