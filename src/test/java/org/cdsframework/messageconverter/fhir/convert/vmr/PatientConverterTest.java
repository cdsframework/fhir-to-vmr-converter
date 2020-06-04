package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson;
import org.opencds.vmr.v1_0.schema.II;

import ca.uhn.fhir.parser.DataFormatException;

/**
 * @author Brian Lamb
 */
public class PatientConverterTest {
    protected CdsInputWrapper wrapper;
    protected JSONObject patient;
    protected PatientConverter patientConverter = new PatientConverter();

    @Before
    public void setUp() throws IOException {
        this.wrapper = CdsInputWrapper.getCdsInputWrapper();

        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/patient.json"));
        String fileContents = new String(data);

        this.patient = new JSONObject(fileContents);
        this.patient = this.patient.getJSONObject("resource");
    }

    @Test
    public void convertToFhirReturnsPatientObjectForValidData() {
        Patient patient = this.patientConverter.convertToFhir(this.patient);
        assertTrue(patient instanceof Patient);
    }

    @Test(expected = DataFormatException.class)
    public void convertToFhirFailsIfInvalidData() {
        JSONObject json = new JSONObject();
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
    public void iceConvertToCdsSetsDataCorrectly() {
        IceCdsInputWrapper wrapper = new IceCdsInputWrapper(this.wrapper);
        wrapper = this.patientConverter.convertToCds(wrapper, this.patient);

        assertEquals(wrapper.getCdsInputWrapper().getPatientId(), "Patient/smart-1032702/_history/1");
        assertEquals(wrapper.getCdsInputWrapper().getPatientGender(), "female");
        assertEquals(wrapper.getCdsInputWrapper().getPatientBirthTime(), "20070320");
        assertEquals(wrapper.getCdsInputWrapper().getPatientFamilyName(), "Shaw");
        assertEquals(wrapper.getCdsInputWrapper().getPatientGivenName(), "Amy");        
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

    @Test
    public void convertToFhirWorksForEvaluatedPerson() {
        EvaluatedPerson person = new EvaluatedPerson();

        II id = new II();
        id.setRoot("tester");

        person.setId(id);

        Patient patient = this.patientConverter.convertToFhir(person);

        assertEquals(patient.getId(), "tester");
    }
}
