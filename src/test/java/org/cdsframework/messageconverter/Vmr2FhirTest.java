package org.cdsframework.messageconverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CDSInput;
import org.opencds.vmr.v1_0.schema.CDSOutput;

/**
 * @author Brian Lamb
 */
public class Vmr2FhirTest {
    protected Vmr2Fhir vmr2Fhir = new Vmr2Fhir();
    protected CDSOutput output;
    protected CDSInput input;

    @Before
    public void setUp() throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/recommendation.xml"));
        this.output = CdsObjectAssist.cdsObjectFromByteArray(data, CDSOutput.class);

        data = Files.readAllBytes(Paths.get("src/test/resources/vmrInput.xml"));
        this.input = CdsObjectAssist.cdsObjectFromByteArray(data, CDSInput.class);
    }

    @Test
    public void getEvaluationsContainsListOfEvaluations() {
        List<ImmunizationEvaluation> evaluations = this.vmr2Fhir.getEvaluations(this.output);
        
        assertFalse(evaluations.isEmpty());
    }

    @Test
    public void getRecommendationContainsRecommendations() {
        ImmunizationRecommendation recommendation = this.vmr2Fhir.getRecommendation(this.output);

        assertFalse(recommendation.getRecommendation().isEmpty());
    }

    @Test
    public void getImmunizationsExtractsImmunizationsFromCdsInput() {
        List<Immunization> immunizations = this.vmr2Fhir.getImmunizations(this.input);

        assertFalse(immunizations.isEmpty());
    }

    @Test
    public void getPatientExtractsPatientDataFromCdsInput() throws ParseException {
        Patient expected = new Patient();
        Patient patient = this.vmr2Fhir.getPatient(this.input);

        Date birthDate = new SimpleDateFormat("yyyymmdd").parse("20091130");

        expected.setGender(AdministrativeGender.fromCode("male"));
        expected.setBirthDate(birthDate);

        assertNotNull(patient);
        assertEquals(expected.getGender(), patient.getGender());
        assertEquals(expected.getBirthDate(), patient.getBirthDate());
    }

    @Test
    public void getObservationsReturnsNoObservationsIfNoObservationResults() {
        this.input.getVmrInput().getPatient().getClinicalStatements().getObservationResults().getObservationResult().clear();

        List<Immunization> observations = this.vmr2Fhir.getObservations(this.input);
        assertTrue(observations.isEmpty());
    }
    
    @Test
    public void getObservationsReturnsImmunizationPerObservationResult() {
        List<Immunization> observations = this.vmr2Fhir.getObservations(this.input);
        assertEquals(
            observations.size(),
            this.input.getVmrInput().getPatient().getClinicalStatements().getObservationResults().getObservationResult().size()
        );
    }
    
    @Test
    public void getImmunizationsReturnsEmptyListIfNoSubstanceAdministrationEvents() {
        this.input.getVmrInput().getPatient().getClinicalStatements().setSubstanceAdministrationEvents(null);

        List<Immunization> events = this.vmr2Fhir.getImmunizations(this.input);
        assertTrue(events.isEmpty());
    }
    
    @Test
    public void getPatientReturnsPopulatedPatientObject() throws ParseException {
        Patient patient = new Patient();
        Patient populated = this.vmr2Fhir.getPatient(this.input);

        assertNotEquals(patient, populated);
    }    
}