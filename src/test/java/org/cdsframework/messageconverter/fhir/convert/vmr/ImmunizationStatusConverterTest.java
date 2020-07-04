package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.BL;

/**
 * @author Brian Lamb
 */
public class ImmunizationStatusConverterTest {
    protected ImmunizationStatusConverter immunizationStatusConverter = new ImmunizationStatusConverter();

    @Test
    public void convertToFhirReturnsNullForNull() {
        ImmunizationStatus status = this.immunizationStatusConverter.convertToFhir(null);

        assertEquals(ImmunizationStatus.NULL, status);
    }

    @Test
    public void convertToFhirReturnsCompletedForValid() {
        BL valid = new BL();
        valid.setValue(true);

        ImmunizationStatus status = this.immunizationStatusConverter.convertToFhir(valid);

        assertEquals(ImmunizationStatus.COMPLETED, status);
    }

    @Test
    public void convertToFhirReturnsNotDoneForFalse() {
        BL valid = new BL();
        valid.setValue(false);

        ImmunizationStatus status = this.immunizationStatusConverter.convertToFhir(valid);

        assertEquals(ImmunizationStatus.NOTDONE, status);
    }

    @Test
    public void convertToCdsReturnsNullForNull() {
        BL valid = this.immunizationStatusConverter.convertToCds(ImmunizationStatus.NULL);

        assertNull(valid);
    }

    @Test
    public void convertToCdsReturnsTrueIfCompleted() {
        BL valid = this.immunizationStatusConverter.convertToCds(ImmunizationStatus.COMPLETED);

        assertTrue(valid.isValue());
    }

    @Test
    public void convertToCdsReturnsFalseIfNotNullOrCompleted() {
        BL valid = this.immunizationStatusConverter.convertToCds(ImmunizationStatus.NOTDONE);

        assertFalse(valid.isValue());
    }
}