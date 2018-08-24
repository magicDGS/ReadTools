package org.magicdgs.readtools.utils.barcodes;

import htsjdk.samtools.SAMReadGroupRecord;

import java.util.List;
import java.util.Set;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: make an implementation
public interface BarcodeSet {

    public int size();

    public SAMReadGroupRecord get(final int i);

    public SAMReadGroupRecord getUnknown();

    public List<String> getSampleBarcodesForIndex(final int i);

    // TODO: change signature to use SAMReadGroupRecord?
    public List<String> getAllBarcodesForSample(final int i);

    public List<SAMReadGroupRecord> asReadGroupList();

    // TODO: deprecated method (should be removed)

    // TODO: remove this method - not required if we support different number of indexes
    public int getMaxNumberOfIndexes();

    // TODO: remove this method - we should not work with joined barcodes at all
    public String getJoinedBarcodesForSample(final int i);

    // TODO: remove this method - we should not work with joined barcodes at all
    public SAMReadGroupRecord getReadGroupForJoinedBarcode(final String combinedBarcode);

    // TODO: this should disappear - it is specific for decoding algorithm
    public Set<String> getSetBarcodesForIndex(final int index);

    // TODO: this should disappear - it is specific for decoding algorithm
    public boolean isBarcodeUniqueInAt(final String barcode, final int index);
}
