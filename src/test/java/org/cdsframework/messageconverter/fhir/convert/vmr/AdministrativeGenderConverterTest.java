package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public class AdministrativeGenderConverterTest {
    protected AdministrativeGenderConverter administrativeGenderConverter = new AdministrativeGenderConverter();
    protected CD code;

    @Before
    public void setUp() {
        this.code = new CD();
        this.code.setCode("male");
    }

    @Test
    public void convertToFhirWorksForExpectedCodes() {
        AdministrativeGender gender = this.administrativeGenderConverter.convertToFhir(this.code);
        assertEquals(gender.getDisplay(), "Male");
    }
    
    @Test
    public void convertToFhirWorksForICECodes() {
        this.code.setCode("m");

        AdministrativeGender gender = this.administrativeGenderConverter.convertToFhir(this.code);
        assertEquals(gender.getDisplay(), "Male");        
    }
    
    @Test
    public void convertToFhirGivesEmptyGenderForInvalidCodes() {
        this.code.setCode("invalid");

        AdministrativeGender gender = this.administrativeGenderConverter.convertToFhir(this.code);
        assertNull(gender);
    }
    
    @Test
    public void convertGenderCodeWorksForValidCodes() {
        this.code.setCode("m");

        String code = this.administrativeGenderConverter.convertGenderCode(this.code);
        assertFalse(code.equals(""));
    }
    
    @Test
    public void convertGenderCodeGivesEmptyStringForInvalidCodes() {
        this.code.setCode("invalid");

        String code = this.administrativeGenderConverter.convertGenderCode(this.code);
        assertTrue(code.equals(""));
    }
}