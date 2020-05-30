package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public class CodeableConceptConverterTest {
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    protected CD code;

    @Before
    public void setUp() {
        this.code = new CD();
        this.code.setCode("cft");
        this.code.setDisplayName("Code for test");
    }

    @Test
    public void convertToFhirReturnsCodeableConceptObjectForValidData() {
        CodeableConcept code = this.codeableConceptConverter.convertToFhir(this.code);
        assertTrue(code instanceof CodeableConcept);
    }

    @Test
    public void convertToFhirHasCorrectData() {
        CodeableConcept code = this.codeableConceptConverter.convertToFhir(this.code);
        assertEquals(code.getCoding().get(0).getCode(), "cft");
        assertEquals(code.getCoding().get(0).getDisplay(), "Code for test");
    }
}