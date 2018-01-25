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

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.cmd.argumentcollections.RTInputArgumentCollection;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.Locatable;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * Base class for all ReadTools command line programs to  raw read traversal. It mimics the
 * behaviour and design of {@link org.broadinstitute.hellbender.engine.GATKTool} to allow easier
 * development similar to the Walkers in the GATK framework.
 *
 * There are several differences with respect to the Walkers from GATK:
 *
 * - The source of reads is a {@link RTDataSource}.
 * - Traversal over read-pairs could be different by overriding {@link #apply(Tuple2)}
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class ReadToolsWalker extends ReadToolsProgram {

    // For the progress meter in the GATKTool
    @Argument(fullName = RTStandardArguments.SECONDS_BETWEEN_PROGRESS_UPDATES_NAME, shortName = RTStandardArguments.SECONDS_BETWEEN_PROGRESS_UPDATES_NAME, doc = "Output traversal statistics every time this many seconds elapse.", optional = true, common = true)
    private double secondsBetweenProgressUpdates = ProgressMeter.DEFAULT_SECONDS_BETWEEN_UPDATES;

    @Argument(fullName = RTStandardArguments.REFERENCE_LONG_NAME, shortName = RTStandardArguments.REFERENCE_SHORT_NAME, doc = "Reference sequence file. Required for CRAM input.", optional = true, common = true)
    private String referencePath = null;

    @ArgumentCollection
    private RTInputArgumentCollection inputArgumentCollection = new RTInputArgumentCollection();

    /**
     * Progress meter to print out traversal statistics. Subclasses must invoke
     * {@link ProgressMeter#update(Locatable)} after each record processed from
     * the primary input in their {@link #traverse} method.
     */
    // TODO: probably our own progress meter could be better
    // TODO: because this only takes into account the locus positions
    private ProgressMeter progressMeter;

    /** Source of reads for traversal, either pair-end or single-end data. */
    private RTDataSource dataSource;

    /**
     * Start the progress meter, and initialize the data source.
     *
     * Authors should override {@link #onTraversalStart()} for pre-traversal operations.
     */
    @Override
    protected final void onStartup() {
        super.onStartup();
        progressMeter = new ProgressMeter(secondsBetweenProgressUpdates);
        dataSource = inputArgumentCollection.getDataSource(getReferencePath());
        logger.info("Input source quality encoding: {}", dataSource.getOriginalQualityEncoding());
    }

    /**
     * Close all data sources on shutdown.
     *
     * Authors should override {@link #onTraversalSuccess()} for post-traversal operations.
     */
    @Override
    protected final void onShutdown() {
        super.onShutdown();
        CloserUtil.close(dataSource);
    }

    /**
     * Operations performed just prior to the start of traversal. Should be overridden by tool
     * authors who need to process arguments local to their tool or perform other kinds of local
     * initialization.
     *
     * Default implementation does nothing.
     */
    public void onTraversalStart() {}

    /**
     * A complete traversal from start to finish of {@link RTDataSource}. Default implementation
     * iterates over pair-end data if {@link #isPaired()} returns {@code true}, or
     * over single-end data if it returns {@code false}.
     *
     * Authors should implement {@link #apply(GATKRead)} and/or {@link #apply(Tuple2)} for perform
     * operations over the reads.
     *
     * Tool authors who wish to "roll their own" traversal from scratch can override this method,
     * but it should be suitable for most of the cases.
     */
    protected void traverse() {
        if (isPaired()) {
            logger.info("Processing reads as pairs.");
            StreamSupport.stream(dataSource.pairedIterator().spliterator(), false)
                    .forEach(pairEndConsumer);
        } else {
            logger.info("Processing reads as singles.");
            StreamSupport.stream(dataSource.spliterator(), false)
                    .forEach(singleEndConsumer);
        }
    }

    // the processing of single-end data
    private final Consumer<GATKRead> singleEndConsumer = r -> {
        apply(r);
        progressMeter.update(r);
    };

    // the processing of pair-end data
    private final Consumer<Tuple2<GATKRead, GATKRead>> pairEndConsumer = r -> {
        apply(r);
        // update with the second because we are traversing read pairs
        // and reporting pairs
        progressMeter.update(r._2);
    };


    /**
     * Process a single read.
     *
     * @param read the read to process.
     */
    protected abstract void apply(final GATKRead read);

    /**
     * Process a pair of reads.
     *
     * Default implementation use {@link #apply(GATKRead)} for each read independently, but tool
     * authors may override as necessary.
     *
     * @param pair pair-end reads to process.
     */
    protected void apply(final Tuple2<GATKRead, GATKRead> pair) {
        apply(pair._1);
        apply(pair._2);
    }

    /**
     * Operations performed immediately after a successful traversal (ie when no uncaught
     * exceptions were thrown during the traversal).
     *
     * Should be overridden by tool authors who need to close local resources, etc., after
     * traversal. Also allows tools to return a value representing the traversal result, which is
     * printed by the engine.
     *
     * Default implementation does nothing and returns null.
     *
     * @return Object representing the traversal result, or null if a tool does not return a value
     */
    public Object onTraversalSuccess() {
        return null;
    }

    @Override
    protected final Object doWork() {
        try {
            onTraversalStart();
            progressMeter.setRecordLabel(isPaired() ? "read pairs" : "reads");
            progressMeter.start();
            traverse();
            progressMeter.stop();
            return onTraversalSuccess();
        } finally {
            closeTool();
        }
    }

    /**
     * This method is called by the framework at the end of the {@link #doWork} template
     * method.
     *
     * It is called regardless of whether the {@link #traverse} has succeeded or not.
     * It is called <em>after</em> the {@link #onTraversalSuccess} has completed (successfully or
     * not) but before the {@link #doWork} method returns.
     *
     * In other words, on successful runs both {@link #onTraversalSuccess} and {@link #closeTool}
     * will be called (in this order) while on failed runs (when {@link #traverse} causes an
     * exception), only {@link #closeTool} will be called.
     *
     * The default implementation does nothing. Subclasses should override this method to close any
     * resources that must be closed regardless of the success of traversal.
     */
    public void closeTool() { }

    /** Gets the reference file if provided; {@code null} otherwise. */
    public final Path getReferencePath() {
        return referencePath == null ? null : IOUtils.getPath(referencePath);
    }

    /** Rerturns {@code true} if the input is paired. */
    public final boolean isPaired() {
        return dataSource.isPaired();
    }

    /**
     * Returns the SAM header for the data source. Will be a merged header if there are multiple
     * inputs for the reads. If there is only a single input, returns its header directly.
     *
     * Note: modifications to this header may affect the original header.
     *
     * @return non-null SAM header.
     */
    public final SAMFileHeader getHeaderForReads() {
        return dataSource.getHeader();
    }

}
