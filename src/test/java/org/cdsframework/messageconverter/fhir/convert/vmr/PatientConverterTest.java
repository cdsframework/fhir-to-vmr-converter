package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.parser.DataFormatException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.cdsframework.cds.vmr.CdsInputWrapper;

import org.hl7.fhir.r4.model.Patient;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Brian Lamb
 */
public class PatientConverterTest {
    protected CdsInputWrapper wrapper;
    protected JsonObject patient;
    protected PatientConverter patientConverter = new PatientConverter();

    @Before
    public void setUp() throws FileNotFoundException {
        this.wrapper = CdsInputWrapper.getCdsInputWrapper();

        JsonParser parser = new JsonParser();
        Object obj;

        try {
            obj = parser.parse(new FileReader("src/test/resources/patient.json"));
        } catch (FileNotFoundException exception) {
            obj = "";
        }

        this.patient = (JsonObject) obj;
        this.patient = this.patient.getAsJsonObject("resource");
    }

    @Test
    public void convertToFhirReturnsPatientObjectForValidData() {
        Patient patient = this.patientConverter.convertToFhir(this.patient);
        assertTrue(patient instanceof Patient);
    }

    @Test(expected = DataFormatException.class)
    public void convertToFhirFailsIfInvalidData() {
        JsonObject json = new JsonObject();
        this.patientConverter.convertToFhir(json);
    }

    @Test
    public void convertToCdsSetsDataCorrectly() {
        this.wrapper = this.patientConverter.convertToCds(this.wrapper, this.patient);

        assertEquals(this.wrapper.getPatientId(), "Patient/smart-1032702/_history/1");
        assertEquals(this.wrapper.getPatientGender(), "female");
        assertEquals(this.wrapper.getPatientBirthTime(), "20070320");
        assertEquals(this.wrapper.getPatientFamilyName(), "Shaw");
        assertEquals(this.wrapper.getPatientGivenName(), "Amy");
    }

    @Test
    public void convertToCdsIgnoresGenderIfNotPresent() {
        this.patient.remove("gender");
        this.wrapper = this.patientConverter.convertToCds(this.wrapper, this.patient);

        assertNull(this.wrapper.getPatientGender());
    }

    @Test
    public void convertToCdsIgnoresBirthDateIfNotPresent() {
        this.patient.remove("birthDate");
        this.wrapper = this.patientConverter.convertToCds(this.wrapper, this.patient);

        assertNull(this.wrapper.getPatientBirthTime());
    }
}
