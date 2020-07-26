package org.cdsframework.messageconverter.fhir.convert.vmr;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.cdsframework.util.LogUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationEvaluation.ImmunizationEvaluationStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.SubstanceAdministrationEvents;
import org.opencds.vmr.v1_0.schema.II;
import org.opencds.vmr.v1_0.schema.ObservationResult;
import org.opencds.vmr.v1_0.schema.ObservationResult.ObservationValue;
import org.opencds.vmr.v1_0.schema.RelatedClinicalStatement;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;

/**
 * @author Brian Lamb
 */
public class ImmunizationEvaluationConverter {
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected PatientConverter patientConverter = new PatientConverter();
    protected IdentifierFactory identifierFactory = new IdentifierFactory();

    private final LogUtils logger = LogUtils.getLogger(ImmunizationRecommendationConverter.class);

    /**
     * This method extracts the data from a CDSOutput object into a List of ImmunizationEvaluation fhir
     * compliant objects. The data is contained in SubstanceAdministrationEvent objects.
     *
     * @param CDSOutput data : the object containing the evaluations
     * @return List<ImmunizationEvaluation>
     */
    public List<ImmunizationEvaluation> convertToFhir(CDSOutput data) {
        List<ImmunizationEvaluation> evaluations = new ArrayList<ImmunizationEvaluation>();
        Patient patient = new Patient();

        try {
            // this is a simple conversion for now and simply extracts the id and creates the Patient object
            patient = this.patientConverter.convertToFhir(data.getVmrOutput().getPatient());
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "Null pointer exception found when accessing patient record");
            return evaluations;
        } catch (IllegalArgumentException exception) {
            logger.debug("convertToFhir", "Unknown gender code");
        }

        for (SubstanceAdministrationEvent event : data.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent()) {
            Immunization immunization = this.immunizationConverter.convertToFhir(patient, event);

            for (RelatedClinicalStatement relatedClinicalStatment : event.getRelatedClinicalStatement()) {
                ObservationResult observationResult = relatedClinicalStatment.getObservationResult();
                ImmunizationEvaluation evaluation = this.convertToFhir(patient, immunization, observationResult);
                evaluations.add(evaluation);
            }
        }

        return evaluations;
    }

    /**
     * This method converts a list of ImmunizationEvaluations into a CDS compatible SubstanceAdministrationEvent
     * object. The evaluations themselves are stored in the child SubstanceAdministrationEvent list inside
     * SubstanceAdministrationEvents.
     *
     * @param List<ImmunizationEvalution> evaluations : a list of FHIR ImmunizationEvalution
     * @return SubstanceAdministrationEvents
     */
    public SubstanceAdministrationEvents convertToCds(List<ImmunizationEvaluation> evaluations) {
        SubstanceAdministrationEvents events = new SubstanceAdministrationEvents();

        for (ImmunizationEvaluation evaluation : evaluations) {
            SubstanceAdministrationEvent event = this.immunizationConverter.convertToCds(evaluation.getImmunizationEventTarget());
            events.getSubstanceAdministrationEvent().add(event);
        }

        return events;
    }

    /**
     * This method converts a SubstanceAdministrationEvent object into an ImmunizationEvaluation object.
     * It requires a Patient object to update the ImmunizationEvaluation object.
     *
     * @param Patient patient : the patient object receiving the immunization
     * @param SubstanceAdministrationEvent event : the substance administration event object containing the evaluation data
     * @return ImmunizationEvaluation
     */
    public ImmunizationEvaluation convertToFhir(Patient patient, Immunization immunization, ObservationResult observationResult) {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();

        Meta meta = new Meta();
        meta.addProfile("http://hl7.org/fhir/us/ImmunizationFHIRDS/StructureDefinition/immds-immunizationevaluation");
        evaluation.setMeta(meta);

        ImmunizationEvaluationStatus immunizationEvaluationStatus = ImmunizationEvaluationStatus.COMPLETED;
        evaluation.setStatus(immunizationEvaluationStatus);

        try {
            evaluation.setId(observationResult.getId().getRoot());
        } catch (NullPointerException exception) {
            evaluation.setId(UUID.randomUUID().toString());
            this.logger.debug("convertToFhir", "No id found");
        }

        try {
            CodeableConcept targetDisease = this.codeableConceptConverter.convertToFhir(
                observationResult.getObservationFocus()
            );
            evaluation.setTargetDisease(targetDisease);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No observation focus found");
        }

        try {
            CodeableConcept doseStatus = this.codeableConceptConverter.convertToFhir(
                observationResult.getObservationValue().getConcept()
            );
            evaluation.setDoseStatus(doseStatus);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No observation value found");
        }

        PositiveIntType doseNumber = new PositiveIntType();
        doseNumber.setValue(1);

        evaluation.setDoseNumber(doseNumber);

        Reference patientReference = new Reference();
        patientReference.setReference("Patient/" + patient.getId());

        Reference immunizationReference = new Reference();
        immunizationReference.setReference("Immunization/" + immunization.getId());

        evaluation.setPatient(patientReference);
        evaluation.setImmunizationEvent(immunizationReference);

        return evaluation;
    }

    /**
     * This method converts an ImmunizationEvaluation object into a CDS ObservationResult object. The data
     * is mapped from the FHIR ImmunizationEvaluation to the ObservationResult object.
     *
     * @param ImmunizationEvaluation evaluation : the FHIR object containing the immunization evaluation data
     * @return ObservationResult
     */
    public ObservationResult convertToCds(ImmunizationEvaluation evaluation) {
        ObservationResult observationResult = new ObservationResult();

        II id = new II();
        id.setRoot(evaluation.getId());

        observationResult.setId(id);

        CodeableConcept targetDisease = evaluation.getTargetDisease();

        if (!targetDisease.isEmpty()) {
            CD observationFocus = this.codeableConceptConverter.convertToCds(targetDisease);
            observationResult.setObservationFocus(observationFocus);
        }

        CodeableConcept doseStatus = evaluation.getDoseStatus();

        if (!doseStatus.isEmpty()) {
            CD observationValueConcept = this.codeableConceptConverter.convertToCds(doseStatus);
            ObservationValue observationValue = new ObservationValue();

            observationValue.setConcept(observationValueConcept);
            observationResult.setObservationValue(observationValue);
        }

        return observationResult;
    }
}