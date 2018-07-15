package org.magicdgs.readtools.cmd.programgroups;

import org.magicdgs.readtools.RTHelpConstants;

import org.broadinstitute.barclay.argparser.CommandLineProgramGroup;

/**
 * Program group for mappability-related operations.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class MappabiltityProgramGroup implements CommandLineProgramGroup {

    @Override
    public String getName() {
        return RTHelpConstants.DOC_CAT_MAPPABILITY;
    }

    @Override
    public String getDescription() {
        return RTHelpConstants.DOC_CAT_MAPPABILITY_SUMMARY;
    }
}
