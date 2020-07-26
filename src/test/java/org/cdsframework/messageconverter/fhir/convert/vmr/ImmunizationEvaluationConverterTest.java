package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.SubstanceAdministrationEvents;
import org.opencds.vmr.v1_0.schema.II;
import org.opencds.vmr.v1_0.schema.ObservationResult;
import org.opencds.vmr.v1_0.schema.ObservationResult.ObservationValue;
import org.opencds.vmr.v1_0.schema.RelatedClinicalStatement;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;
import org.opencds.vmr.v1_0.schema.VMR;

/**
 * @author Brian Lamb
 */
public class ImmunizationEvaluationConverterTest {
    protected ImmunizationEvaluationConverter immunizationEvaluationConverter = new ImmunizationEvaluationConverter();
    protected CDSOutput output;
    protected CD code;
    protected CDSOutput rawOutput = new CDSOutput();
    protected IdentifierFactory identifierFactory = new IdentifierFactory();

    @Before
    public void setUp() throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/recommendation.xml"));
        this.output = CdsObjectAssist.cdsObjectFromByteArray(data, CDSOutput.class);

        this.code = new CD();
        this.code.setCode("jut");
        this.code.setDisplayName("Junit Test");

        this.rawOutput = new CDSOutput();

        VMR vmr = new VMR();
        this.rawOutput.setVmrOutput(vmr);

        EvaluatedPerson person = new EvaluatedPerson();
        vmr.setPatient(person);

        ClinicalStatements clinicalStatements = new ClinicalStatements();
        person.setClinicalStatements(clinicalStatements);

        SubstanceAdministrationEvents substanceAdministrationEvents = new SubstanceAdministrationEvents();
        clinicalStatements.setSubstanceAdministrationEvents(substanceAdministrationEvents);
    }

    @Test
    public void convertToFhirReturnsNoEvaluationsIfNoPatient() {
        CDSOutput blankOutput = new CDSOutput();
        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(blankOutput);

        assertEquals(evaluations.size(), 0);
    }

    @Test
    public void convertToFhirReturnsNoEvaluationsIfNoSubstanceAdministrationEvents() {
        CDSOutput blankOutput = new CDSOutput();
        EvaluatedPerson patient = new EvaluatedPerson();
        VMR vmr = new VMR();

        vmr.setPatient(patient);

        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(blankOutput);

        assertEquals(evaluations.size(), 0);

    }

    @Test
    public void convertToFhirAddsEvaluations() {
        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(this.output);
        assertFalse(evaluations.isEmpty());
    }

    @Test
    public void convertToFhirSetsPatientAndImmunization() {
        Patient patient = new Patient();
        patient.setId("id");

        Immunization immunization = new Immunization();
        immunization.setId("Id");

        ObservationResult result = new ObservationResult();

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, result);

        assertFalse(evaluation.getPatient().isEmpty());
        assertFalse(evaluation.getImmunizationEvent().isEmpty());
    }

    @Test
    public void convertToFhirReturnsEmptyListIfNoEvents() {
        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(this.rawOutput);

        assertEquals(0, evaluations.size());
    }

    @Test
    public void convertToFhirReturnsEmptyListIfNoClinicalStatements() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        this.rawOutput.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().add(event);
        this.rawOutput.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().add(event);
        this.rawOutput.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().add(event);

        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(this.rawOutput);

        assertEquals(0, evaluations.size());
    }

    @Test
    public void convertToFhirAddsEvaluationForEachClinicalStatement() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();

        event.getRelatedClinicalStatement().add(clinicalStatement);
        event.getRelatedClinicalStatement().add(clinicalStatement);

        this.rawOutput.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().add(event);
        this.rawOutput.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().add(event);
        this.rawOutput.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().add(event);

        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(this.rawOutput);

        assertEquals(6, evaluations.size());
    }

    @Test
    public void convertToCdsAddsNoEventsIfNoEvaluations() {
        List<ImmunizationEvaluation> evaluations = new ArrayList<ImmunizationEvaluation>();

        SubstanceAdministrationEvents events = this.immunizationEvaluationConverter.convertToCds(evaluations);

        assertEquals(0, events.getSubstanceAdministrationEvent().size());
    }

    @Test
    public void convertToCdsAddsEventForEachEvaluation() {
        List<ImmunizationEvaluation> evaluations = new ArrayList<ImmunizationEvaluation>();
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();

        evaluations.add(evaluation);
        evaluations.add(evaluation);
        evaluations.add(evaluation);

        SubstanceAdministrationEvents events = this.immunizationEvaluationConverter.convertToCds(evaluations);

        assertEquals(3, events.getSubstanceAdministrationEvent().size());
    }

    @Test
    public void convertToFhirDoesntSetIdRandomlyEvenIfNoId() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertNotNull(evaluation.getId());
    }

    @Test
    public void convertToFhirSetsIdFromId() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        II id = new II();
        id.setRoot("my id");

        observationResult.setId(id);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertEquals("my id", evaluation.getId());
    }

    @Test
    public void convertToFhirDoesNotSetTargetDiseaseIfNoObservationFocus() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertTrue(evaluation.getTargetDisease().isEmpty());
    }

    @Test
    public void convertToFhirSetsTargetDiseaseFromObservationFocus() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        CD observationFocus = new CD();
        observationFocus.setCode("code");
        observationFocus.setDisplayName("display");
        observationFocus.setCodeSystem("system");
        observationResult.setObservationFocus(observationFocus);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertFalse(evaluation.getTargetDisease().isEmpty());
    }

    @Test
    public void convertToFhirDoesNotSetDoseStatusIfNoObservationValue() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertTrue(evaluation.getDoseStatus().isEmpty());
    }

    @Test
    public void convertToFhirSetsDoseStatusFromObservationValue() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        CD observationValueConcept = new CD();
        observationValueConcept.setCode("code");
        observationValueConcept.setDisplayName("display");
        observationValueConcept.setCodeSystem("system");

        ObservationValue observationValue = new ObservationValue();
        observationValue.setConcept(observationValueConcept);

        observationResult.setObservationValue(observationValue);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertFalse(evaluation.getDoseStatus().isEmpty());
    }

    @Test
    public void convertToFhirAddsNoDoseStatusReasonIfNoInterpretations() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertEquals(0, evaluation.getDoseStatusReason().size());
    }

    @Test
    public void convertToCdsSetsIdFromId() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        evaluation.setId("my id");

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals("my id", observationResult.getId().getRoot());
    }

    @Test
    public void convertToCdsDoesNotSetObservationFocusIfNoTargetDisease() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertNull(observationResult.getObservationFocus());
    }

    @Test
    public void convertToCdsSetsObservationFocusFromTargetDisease() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        CodeableConcept targetDisease = new CodeableConcept();
        targetDisease.setId("my id");

        evaluation.setTargetDisease(targetDisease);

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertNotNull(observationResult.getObservationFocus());
    }

    @Test
    public void convertToCdsDoesNotSetObservationValueIfNoDoseStatus() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertNull(observationResult.getObservationValue());
    }

    @Test
    public void convertToCdsSetsObservationValueFromDoseStatus() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        CodeableConcept doseStatus = new CodeableConcept();
        doseStatus.setId("my id");

        evaluation.setDoseStatus(doseStatus);

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertNotNull(observationResult.getObservationValue());
    }

    @Test
    public void convertToCdsAddsNoInterpretationIfNoDoseStatusReason() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals(0, observationResult.getInterpretation().size());
    }
}