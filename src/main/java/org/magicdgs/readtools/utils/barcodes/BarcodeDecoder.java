package org.magicdgs.readtools.utils.barcodes;

import org.magicdgs.readtools.metrics.barcodes.BarcodeStat;
import org.magicdgs.readtools.metrics.barcodes.MatcherStat;

import htsjdk.samtools.metrics.MetricsFile;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Common interface for assigning a read group to a read based on barcodes.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface BarcodeDecoder {

    public void assignReadGroup(final GATKRead read);

    public BarcodeSet getBarcodeSet();

    public MetricsFile<MatcherStat,Integer> getMatcherStatMetrics();

    public MetricsFile<BarcodeStat, Integer> getBarcodeStatMetrics();
}
