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
import org.opencds.vmr.v1_0.schema.EvaluatedPerson;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.SubstanceAdministrationEvents;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.SubstanceAdministrationProposals;
import org.opencds.vmr.v1_0.schema.RelatedClinicalStatement;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationProposal;
import org.opencds.vmr.v1_0.schema.VMR;

/**
 * @author Brian Lamb
 */
public class Vmr2FhirTest {
    protected Vmr2Fhir vmr2Fhir = new Vmr2Fhir();
    protected CDSOutput output;
    protected CDSInput input;
    protected CDSOutput customOutput = new CDSOutput();

    @Before
    public void setUp() throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/recommendation.xml"));
        this.output = CdsObjectAssist.cdsObjectFromByteArray(data, CDSOutput.class);

        data = Files.readAllBytes(Paths.get("src/test/resources/vmrInput.xml"));
        this.input = CdsObjectAssist.cdsObjectFromByteArray(data, CDSInput.class);

        ClinicalStatements clinicalStatements = new ClinicalStatements();

        EvaluatedPerson patient = new EvaluatedPerson();
        patient.setClinicalStatements(clinicalStatements);

        VMR vmr = new VMR();
        vmr.setPatient(patient);

        this.customOutput.setVmrOutput(vmr);
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

    @Test
    public void getEvaluationsReturnsEmptyListIfNoSubstanceAdministrationEvents() throws ParseException {
        List<ImmunizationEvaluation> evaluations = this.vmr2Fhir.getEvaluations(this.customOutput);

        assertEquals(0, evaluations.size());
    }

    @Test
    public void getEvaluationsReturnsEmptyListIfSubSubstanceAdministrationEventsHaveNoRelatedClinicalStatements() throws ParseException {
        SubstanceAdministrationEvents substanceAdministrationEvents = new SubstanceAdministrationEvents();
        this.customOutput.getVmrOutput().getPatient().getClinicalStatements().setSubstanceAdministrationEvents(substanceAdministrationEvents);

        List<ImmunizationEvaluation> evaluations = this.vmr2Fhir.getEvaluations(this.customOutput);

        assertEquals(0, evaluations.size());
    }

    @Test
    public void getEvaluationsAddsEvaluationForEachRelatedClinicalStatement() throws ParseException {
        SubstanceAdministrationEvents substanceAdministrationEvents = new SubstanceAdministrationEvents();
        this.customOutput.getVmrOutput().getPatient().getClinicalStatements().setSubstanceAdministrationEvents(substanceAdministrationEvents);

        SubstanceAdministrationEvent outerEvent = new SubstanceAdministrationEvent();
        RelatedClinicalStatement outerStatement = new RelatedClinicalStatement();
        SubstanceAdministrationEvent innerEvent = new SubstanceAdministrationEvent();
        RelatedClinicalStatement innerStatement = new RelatedClinicalStatement();

        outerEvent.getRelatedClinicalStatement().add(outerStatement);
        outerEvent.getRelatedClinicalStatement().add(outerStatement);
        outerStatement.setSubstanceAdministrationEvent(innerEvent);
        innerEvent.getRelatedClinicalStatement().add(innerStatement);
        innerEvent.getRelatedClinicalStatement().add(innerStatement);
        innerEvent.getRelatedClinicalStatement().add(innerStatement);
        substanceAdministrationEvents.getSubstanceAdministrationEvent().add(outerEvent);

        List<ImmunizationEvaluation> evaluations = this.vmr2Fhir.getEvaluations(this.customOutput);

        assertEquals(6, evaluations.size());
    }

    @Test
    public void getRecommendationReturnsEmptyListIfNoProposals() throws IllegalArgumentException, ParseException {
        SubstanceAdministrationProposals substanceAdministrationProposals = new SubstanceAdministrationProposals();
        this.customOutput.getVmrOutput().getPatient().getClinicalStatements().setSubstanceAdministrationProposals(substanceAdministrationProposals);

        ImmunizationRecommendation recommendation = this.vmr2Fhir.getRecommendation(this.customOutput);

        assertEquals(0, recommendation.getRecommendation().size());
    }

    @Test
    public void getRecommendationAddsRecommendationPerProposal() throws IllegalArgumentException, ParseException {
        SubstanceAdministrationProposals substanceAdministrationProposals = new SubstanceAdministrationProposals();
        this.customOutput.getVmrOutput().getPatient().getClinicalStatements().setSubstanceAdministrationProposals(substanceAdministrationProposals);

        SubstanceAdministrationProposal proposal = new SubstanceAdministrationProposal();
        substanceAdministrationProposals.getSubstanceAdministrationProposal().add(proposal);
        substanceAdministrationProposals.getSubstanceAdministrationProposal().add(proposal);
        substanceAdministrationProposals.getSubstanceAdministrationProposal().add(proposal);

        ImmunizationRecommendation recommendation = this.vmr2Fhir.getRecommendation(this.customOutput);

        assertEquals(3, recommendation.getRecommendation().size());
    }

    @Test
    public void getImmunizationsReturnsEmptyListIfSubstanceAdministrationEventsHasNoSubstanceAdministrationEvents() throws ParseException {
        List<Immunization> immunizations = this.vmr2Fhir.getImmunizations(this.customOutput);

        assertEquals(0, immunizations.size());
    }

    @Test
    public void getImmunizationsReturnsImmunizationForEachSubstanceAdministrationEvent() throws ParseException {
        SubstanceAdministrationEvents substanceAdministrationEvents = new SubstanceAdministrationEvents();
        this.customOutput.getVmrOutput().getPatient().getClinicalStatements().setSubstanceAdministrationEvents(substanceAdministrationEvents);

        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        substanceAdministrationEvents.getSubstanceAdministrationEvent().add(event);
        substanceAdministrationEvents.getSubstanceAdministrationEvent().add(event);
        substanceAdministrationEvents.getSubstanceAdministrationEvent().add(event);

        List<Immunization> immunizations = this.vmr2Fhir.getImmunizations(this.customOutput);

        assertEquals(3, immunizations.size());
    }
}