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
import org.hl7.fhir.r4.model.Identifier;
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

        assertFalse(evaluation.getPatientTarget().isEmpty());
        assertFalse(evaluation.getImmunizationEventTarget().isEmpty());
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
    public void convertToFhirAddsNoExtensionsIfNoIdentifiers() {
        Immunization immunization = new Immunization();
        Patient patient = new Patient();
        ObservationResult observationResult = new ObservationResult();
        boolean parentId = false;

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(
            patient,
            immunization,
            immunization,
            observationResult
        );

        for (Identifier identifier : evaluation.getIdentifier()) {
            if (identifier.getType().getText().equals("parentId")) {
                parentId = true;
                assertEquals(0, identifier.getExtension().size());
            }
        }

        assertTrue(parentId);
    }

    @Test
    public void convertToFhirAddsNoExtensionsIfNoIdExtensionIdentifier() {
        Immunization immunization = new Immunization();
        Patient patient = new Patient();
        ObservationResult observationResult = new ObservationResult();
        boolean parentId = false;

        Identifier identifier = this.identifierFactory.create("notUseful", "3");

        immunization.addIdentifier(identifier);
        immunization.addIdentifier(identifier);
        immunization.addIdentifier(identifier);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(
            patient,
            immunization,
            immunization,
            observationResult
        );

        for (Identifier evaluationIdentifier : evaluation.getIdentifier()) {
            if (evaluationIdentifier.getType().getText().equals("parentId")) {
                parentId = true;
                assertEquals(0, evaluationIdentifier.getExtension().size());
            }
        }

        assertTrue(parentId);
    }

    @Test
    public void convertToFhirAddsExtensionForEachIdExtensionIdentifier() {
        Immunization immunization = new Immunization();
        Patient patient = new Patient();
        ObservationResult observationResult = new ObservationResult();
        boolean parentId = false;

        Identifier templateId = this.identifierFactory.create("templateId", "3");
        Identifier idExtension = this.identifierFactory.create("idExtension", "4");

        immunization.addIdentifier(templateId);
        immunization.addIdentifier(idExtension);
        immunization.addIdentifier(idExtension);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(
            patient,
            immunization,
            immunization,
            observationResult
        );

        for (Identifier identifier : evaluation.getIdentifier()) {
            if (identifier.getType().getText().equals("parentId")) {
                parentId = true;
                assertEquals(2, identifier.getExtension().size());
            }
        }

        assertTrue(parentId);
    }

    @Test
    public void convertToFhirAddsNoTemplateIdIdentifiersIfNoTemplateIds() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();
        boolean templateId = false;

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        for (Identifier identifier : evaluation.getIdentifier()) {
            if (identifier.getType().getText().equals("templateId")) {
                templateId = true;
            }
        }

        assertFalse(templateId);
    }

    @Test
    public void convertToFhirAddsTemplateIdIdentifierForEachTemplateId() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();
        int templateIds = 0;

        II templateId = new II();

        observationResult.getTemplateId().add(templateId);
        observationResult.getTemplateId().add(templateId);
        observationResult.getTemplateId().add(templateId);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        for (Identifier identifier : evaluation.getIdentifier()) {
            if (identifier.getType().getText().equals("templateId")) {
                templateIds++;
            }
        }

        assertEquals(3, templateIds);
    }

    @Test
    public void convertToFhirDoesntSetIdIfNoIdFound() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertNull(evaluation.getId());
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
    public void convertToFhirAddsNoExtensionIdIfNoExtension() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();
        boolean extensionId = false;

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        for (Identifier identifier : evaluation.getIdentifier()) {
            if (identifier.getType().getText().equals("extensionId")) {
                extensionId = true;
            }
        }

        assertFalse(extensionId);
    }

    @Test
    public void convertToFhirAddsIdentifierForExtensionId() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();
        boolean extensionId = false;

        II id = new II();
        id.setRoot("my id");
        id.setExtension("my extension");

        observationResult.setId(id);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        for (Identifier identifier : evaluation.getIdentifier()) {
            if (identifier.getType().getText().equals("extensionId")) {
                extensionId = true;
                assertEquals("my extension", identifier.getValue());
            }
        }

        assertTrue(extensionId);
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
    public void convertToFhirAddsDoseStatusReasonForEachInterpretation() {
        Patient patient = new Patient();
        Immunization immunization = new Immunization();
        ObservationResult observationResult = new ObservationResult();

        CD interpretation = new CD();
        interpretation.setCode("code");
        interpretation.setDisplayName("display");
        interpretation.setCodeSystem("system");

        observationResult.getInterpretation().add(interpretation);
        observationResult.getInterpretation().add(interpretation);
        observationResult.getInterpretation().add(interpretation);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, observationResult);

        assertEquals(3, evaluation.getDoseStatusReason().size());
    }

    @Test
    public void convertToCdsSetsIdFromId() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        evaluation.setId("my id");

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals("my id", observationResult.getId().getRoot());
    }

    @Test
    public void convertToCdsSetsTemplateForEachTemplateIdentifier() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        Identifier templateId = this.identifierFactory.create("templateId", "template");

        evaluation.addIdentifier(templateId);
        evaluation.addIdentifier(templateId);
        evaluation.addIdentifier(templateId);

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals(3, observationResult.getTemplateId().size());
    }

    @Test
    public void convertToCdsSetsExtensionFromIdentifier() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        Identifier extensionId = this.identifierFactory.create("extensionId", "extension");

        evaluation.addIdentifier(extensionId);

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals("extension", observationResult.getId().getExtension());
    }

    @Test
    public void convertToCdsAddsNoTemplateIdIfNoIdentifiers() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals(0, observationResult.getTemplateId().size());
    }

    @Test
    public void convertToCdsDoesNotSetExtensionIfNoIdentifiers() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals("", observationResult.getId().getExtension());
    }

    @Test
    public void convertToCdsDoesNotAddTemplateIdIfNoMatchingIdentifiers() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        Identifier identifier = this.identifierFactory.create("regular", "id");

        evaluation.addIdentifier(identifier);

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals(0, observationResult.getTemplateId().size());
    }

    @Test
    public void convertToCdsDoesNotSetExtensionIfNoMatchingIdentifiers() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        Identifier identifier = this.identifierFactory.create("regular", "id");

        evaluation.addIdentifier(identifier);

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals("", observationResult.getId().getExtension());
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

    @Test
    public void convertToCdsAddsInterpretationForEachDoseStatusReason() {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        CodeableConcept doseStatusReason = new CodeableConcept();

        evaluation.addDoseStatusReason(doseStatusReason);
        evaluation.addDoseStatusReason(doseStatusReason);
        evaluation.addDoseStatusReason(doseStatusReason);

        ObservationResult observationResult = this.immunizationEvaluationConverter.convertToCds(evaluation);

        assertEquals(3, observationResult.getInterpretation().size());
    }
}