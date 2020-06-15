package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
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
        this.code.setCodeSystem("code system");
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
        assertEquals(code.getCoding().get(0).getSystem(), "code system");
    }

    @Test
    public void convertToCdsHasCorrectData() {
        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();

        coding.setCode("mc");
        coding.setDisplay("my code");
        coding.setSystem("my system");
        
        code.addCoding(coding);

        CD cd = this.codeableConceptConverter.convertToCds(code);

        assertNotNull(cd);
        assertTrue(cd instanceof CD);
        assertEquals("mc", cd.getCode());
        assertEquals("my code", cd.getDisplayName());
        assertEquals("my system", cd.getCodeSystem());
    }
}