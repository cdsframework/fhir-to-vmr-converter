package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.Demographics;
import org.opencds.vmr.v1_0.schema.TS;

import ca.uhn.fhir.parser.DataFormatException;

/**
 * @author Brian Lamb
 */
public class PatientConverterTest {
    protected CdsInputWrapper wrapper;
    protected JSONObject patient;
    protected PatientConverter patientConverter = new PatientConverter();
    protected EvaluatedPerson person;
    protected IdentifierFactory identifierFactory = new IdentifierFactory();

    @Before
    public void setUp() throws IOException {
        this.wrapper = CdsInputWrapper.getCdsInputWrapper();

        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/patient.json"));
        String fileContents = new String(data);

        this.patient = new JSONObject(fileContents);
        this.patient = this.patient.getJSONObject("resource");

        this.person = new EvaluatedPerson();
        this.person.setDemographics(this.createDemographics("male", "20000120"));
    }

    protected Demographics createDemographics(String gender, String birthdate) {
        Demographics demographics = new Demographics();

        CD genderCode = new CD();
        TS birthtime = new TS();

        genderCode.setCode(gender);
        birthtime.setValue(birthdate);

        demographics.setGender(genderCode);
        demographics.setBirthTime(birthtime);

        return demographics;
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
    public void convertToFhirFailsIfBadGender() throws ParseException {
        Demographics demographics = this.createDemographics("incorrect", "20000120");
        this.person.setDemographics(demographics);
        Patient patient = this.patientConverter.convertToFhir(this.person);
        assertNull(patient.getGender());
    }

    @Test
    public void convertToCdsSetsDataCorrectly() {
        this.wrapper = this.patientConverter.convertToCds(this.wrapper, this.patient);

        assertEquals(this.wrapper.getPatientGender(), "female");
        assertEquals(this.wrapper.getPatientBirthTime(), "20070320");
        assertEquals(this.wrapper.getPatientFamilyName(), "Shaw");
        assertEquals(this.wrapper.getPatientGivenName(), "Amy");
    }

    @Test
    public void iceConvertToCdsSetsDataCorrectly() {
        IceCdsInputWrapper wrapper = new IceCdsInputWrapper(this.wrapper);
        wrapper = this.patientConverter.convertToCds(wrapper, this.patient);

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
    public void convertToFhirWorksForEvaluatedPerson() throws ParseException {
        Patient patient = this.patientConverter.convertToFhir(this.person);

        LocalDate birthdate = patient.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        assertEquals(birthdate.getYear(), 2000);
        assertEquals(birthdate.getMonthValue(), 1);
        assertEquals(birthdate.getDayOfMonth(), 20);
        assertEquals(patient.getGender(), Enumerations.AdministrativeGender.MALE);
    }

    @Test
    public void convertToCdsSetsEvaluatedPersonCorrectly() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmdd");
        Date birthDate = dateFormat.parse("20200608");

        AdministrativeGender gender = AdministrativeGender.fromCode("male");

        Patient patient = new Patient();
        patient.setBirthDate(birthDate);
        patient.setGender(gender);

        EvaluatedPerson person = this.patientConverter.convertToCds(patient);
        Date personDate = dateFormat.parse(
            person.getDemographics().getBirthTime().getValue()
        );

        assertTrue(person instanceof EvaluatedPerson);
        assertEquals("M", person.getDemographics().getGender().getCode());
        assertEquals(personDate, birthDate);
    }

    @Test
    public void convertToCdsSetsBirthTime() throws ParseException {
        Patient patient = new Patient();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmdd");
        Date birthDate = dateFormat.parse("20200615");
        patient.setBirthDate(birthDate);

        EvaluatedPerson person = this.patientConverter.convertToCds(patient);

        assertNotNull(person.getDemographics().getBirthTime());
    }

    @Test
    public void convertToCdsDoesntSetBirthTimeIfNoBirthdate() {
        Patient patient = new Patient();

        EvaluatedPerson person = this.patientConverter.convertToCds(patient);

        assertNull(person.getDemographics().getBirthTime());
    }

    @Test
    public void convertToFhirGenderNotAddedIfNoDemographics() throws ParseException {
        EvaluatedPerson person = new EvaluatedPerson();
        Patient patient = this.patientConverter.convertToFhir(person);

        assertNull(patient.getGender());
    }

    @Test
    public void convertToFhirSetsBirthDateFromBirthTime() throws ParseException {
        EvaluatedPerson person = new EvaluatedPerson();
        Demographics demographics = new Demographics();
        TS birthTime = new TS();
        birthTime.setValue("20200615");

        demographics.setBirthTime(birthTime);

        person.setDemographics(demographics);

        Patient patient = this.patientConverter.convertToFhir(person);
        assertNotNull(patient.getBirthDate());
    }

    @Test
    public void convertToFhirDoesntSetBirthDateIfNoBirthTime() throws ParseException {
        EvaluatedPerson person = new EvaluatedPerson();

        Patient patient = this.patientConverter.convertToFhir(person);
        assertNull(patient.getBirthDate());
    }
}