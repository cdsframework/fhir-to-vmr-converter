package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.opencds.vmr.v1_0.schema.BL;

/**
 * @author Brian Lamb
 */
public class ImmunizationStatusConverter {
    /**
     * This method converts a CDS valid status to an ImmunizationStatus value. ImmunizationStatus are full objects
     * but are needed in certain places.
     *
     * @param BL valid : the CDS version of a valid status
     * @return ImmunizationStatus
     */
    public ImmunizationStatus convertToFhir(BL valid) {
        if (valid == null) {
            return ImmunizationStatus.NULL;
        }

        if (valid.isValue()) {
            return ImmunizationStatus.COMPLETED;
        }

        return ImmunizationStatus.NOTDONE;
    }

    /**
     * This method converts an ImmunizationStatus object into a CDS BL status that is used to track
     * validity.
     *
     * @param ImmunizationStatus status : the FHIR ImmunizationStatus object
     * @return BL
     */
    public BL convertToCds(ImmunizationStatus status) {
        BL valid = new BL();

        if (status == ImmunizationStatus.NULL) {
            return null;
        }

        if (status == ImmunizationStatus.COMPLETED) {
            valid.setValue(true);
        } else {
            valid.setValue(false);
        }

        return valid;
    }
}