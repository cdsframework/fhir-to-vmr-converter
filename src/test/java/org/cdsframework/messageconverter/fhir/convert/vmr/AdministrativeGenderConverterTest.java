package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.cdsframework.messageconverter.fhir.convert.utils.CDComparison;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public class AdministrativeGenderConverterTest {
    protected AdministrativeGenderConverter administrativeGenderConverter = new AdministrativeGenderConverter();
    protected CDComparison cdComparison = new CDComparison();
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

    @Test
    public void convertToCdsCreatesValidObject() {
        AdministrativeGender gender = AdministrativeGender.fromCode("male");
        CD code = this.administrativeGenderConverter.convertToCds(gender);

        assertNotNull(code);
        assertTrue(code instanceof CD);
        assertEquals("male", code.getCode());
    }

    @Test
    public void canConvertFromFhirToCdsBackToFhir() {
        AdministrativeGender gender = AdministrativeGender.fromCode("male");

        CD code = this.administrativeGenderConverter.convertToCds(gender);

        AdministrativeGender duplicate = this.administrativeGenderConverter.convertToFhir(code);

        assertEquals(gender, duplicate);
    }

    @Test
    public void canConvertFromCdsToFhirBackToCds() {
        AdministrativeGender gender = this.administrativeGenderConverter.convertToFhir(this.code);
        CD duplicate = this.administrativeGenderConverter.convertToCds(gender);

        assertNotNull(duplicate);
        assertTrue(this.cdComparison.isEqual(this.code, duplicate));
    }

    @Test
    public void convertGenderCodeGetsCode() {
        AdministrativeGender gender = AdministrativeGender.fromCode("male");
        String code = this.administrativeGenderConverter.convertGenderCode(gender);
        
        assertNotEquals(code, "");
    }

    @Test
    public void convertGenderCodeGetsEmptyStringIfNoGender() {
        AdministrativeGender gender = AdministrativeGender.NULL;
        String code = this.administrativeGenderConverter.convertGenderCode(gender);
        
        assertEquals(code, "");
    }

    @Test
    public void convertGenderCodeGetsEmptyStringIfUnknownCase() {
        AdministrativeGender gender = AdministrativeGender.OTHER;
        String code = this.administrativeGenderConverter.convertGenderCode(gender);
        
        assertEquals(code, "");        
    }
}