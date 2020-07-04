package org.cdsframework.messageconverter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.cdsframework.messageconverter.fhir.convert.vmr.ImmunizationConverter;
import org.cdsframework.messageconverter.fhir.convert.vmr.ImmunizationEvaluationConverter;
import org.cdsframework.messageconverter.fhir.convert.vmr.ImmunizationRecommendationConverter;
import org.cdsframework.messageconverter.fhir.convert.vmr.PatientConverter;
import org.cdsframework.util.LogUtils;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.vmr.v1_0.schema.CDSInput;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.SubstanceAdministrationEvents;
import org.opencds.vmr.v1_0.schema.ObservationResult;
import org.opencds.vmr.v1_0.schema.RelatedClinicalStatement;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationProposal;

/**
 * @author Brian Lamb
 */
public class Vmr2Fhir {
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected ImmunizationRecommendationConverter immunizationRecommendationConverter = new ImmunizationRecommendationConverter();
    protected ImmunizationEvaluationConverter immunizationEvaluationConverter = new ImmunizationEvaluationConverter();
    protected PatientConverter patientConverter = new PatientConverter();
    private final LogUtils logger = LogUtils.getLogger(Vmr2Fhir.class);

    /**
     * This method converts a CDSOutput object into a list of ImmunizationEvaluation objects. This data is contained
     * inside of SubstanceAdministrationEvents.
     *
     * @param CDSOutput output : object containing recommendations and evaluations
     * @return List<ImmunizationEvaluation>
     */
    public List<ImmunizationEvaluation> getEvaluations(CDSOutput output) throws ParseException {
        List<ImmunizationEvaluation> evaluations = new ArrayList<ImmunizationEvaluation>();

        Patient patient = this.patientConverter.convertToFhir(output);
        SubstanceAdministrationEvents substanceAdministrationEvents = output.getVmrOutput()
            .getPatient()
            .getClinicalStatements()
            .getSubstanceAdministrationEvents();

        if (substanceAdministrationEvents == null) {
            return evaluations;
        }

        // this next line is null so we need to grab it and do it
        for (SubstanceAdministrationEvent outerSubstanceAdministrationEvent : substanceAdministrationEvents.getSubstanceAdministrationEvent()) {
            for (RelatedClinicalStatement outerRelatedClinicalStatement : outerSubstanceAdministrationEvent.getRelatedClinicalStatement()) {
                SubstanceAdministrationEvent substanceAdministrationEvent = outerRelatedClinicalStatement.getSubstanceAdministrationEvent();

                Immunization outerImmunization = this.immunizationConverter.convertToFhir(outerSubstanceAdministrationEvent);
                Immunization immunization = this.immunizationConverter.convertToFhir(substanceAdministrationEvent);

                for (RelatedClinicalStatement relatedClinicalStatement : substanceAdministrationEvent.getRelatedClinicalStatement()) {
                    ObservationResult observationResult = relatedClinicalStatement.getObservationResult();
                    ImmunizationEvaluation immunizationEvaluation = this.immunizationEvaluationConverter.convertToFhir(patient, immunization, outerImmunization, observationResult);
                    evaluations.add(immunizationEvaluation);
                }
            }
        }

        return evaluations;
    }

    /**
     * This method extracts a list of observations from a CDSInput object. It converts the CDSInput object
     * into a list of FHIR Immunization objects.
     *
     * @param CDSInput input : the input object containing observation data
     * @return List<Immunization>
     */
    public List<Immunization> getObservations(CDSInput input) {
        List<Immunization> observations = new ArrayList<Immunization>();

        for (ObservationResult result : input.getVmrInput().getPatient().getClinicalStatements().getObservationResults().getObservationResult()) {
            observations.add(this.immunizationConverter.convertToFhir(result));
        }

        return observations;
    }

    /**
     * This method extracts a list of observations from a CDSOutput object. It converts the CDSOutput object
     * into a list of FHIR Immunization objects.
     *
     * @param CDSOutput output : the output object containing observation data
     * @return List<Immunization>
     */
    public List<Immunization> getObservations(CDSOutput output) {
        List<Immunization> observations = new ArrayList<Immunization>();

        for (ObservationResult result : output.getVmrOutput().getPatient().getClinicalStatements().getObservationResults().getObservationResult()) {
            observations.add(this.immunizationConverter.convertToFhir(result));
        }

        return observations;
    }

    /**
     * This method extracts the data from a CDSOutput object to form an ImmunizationRecommendation fhir
     * object. This data is contained inside SubstanceAdministrationProposal objects.
     *
     * @param ImmunizationRecommendation output : object containing an immunization recommendation request
     * @return ImmunizationRecommendation
     */
    public List<ImmunizationRecommendation> getRecommendations(CDSOutput output) throws IllegalArgumentException, ParseException {
        List<ImmunizationRecommendation> recommendations = new ArrayList<ImmunizationRecommendation>();
        Patient patient = this.patientConverter.convertToFhir(output);

        for (SubstanceAdministrationProposal proposal : output.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationProposals().getSubstanceAdministrationProposal()) {
            ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(patient, proposal);
            recommendations.add(recommendation);
        }

        return recommendations;
    }

    /**
     * This method converts the data found in a CDSOutput object into a patient object. The patient data
     * is located in an EvaluatedPerson object.
     *
     * @param CDSOutput output : the object containing the Patient data
     * @return Patient
     */
    public Patient getPatient(CDSOutput output) throws IllegalArgumentException, ParseException {
        return this.patientConverter.convertToFhir(output);
    }

    /**
     * This method extracts a list of immunizations from a CDSInput object. This data is contained
     * inside SubstanceAdministrationEvent objects.
     *
     * @param CDSInput input : object containing immunizations received
     * @return List<Immunization>
     */
    public List<Immunization> getImmunizations(CDSInput input) {
        List<Immunization> immunizations = new ArrayList<Immunization>();

        try {
            for (SubstanceAdministrationEvent event : input.getVmrInput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent()) {
                immunizations.add(this.immunizationConverter.convertToFhir(event));
            }
        } catch (NullPointerException exception) {
            this.logger.debug("getImmunizations", "No substance administration events found");
        }

        return immunizations;
    }

    /**
     * This method converts substance administration events found in a CDSOutput object into a list of
     * FHIR Immunization objects.
     *
     * @param CDSOutput output : object containing immunization data
     * @return List<Immunization>
     */
    public List<Immunization> getImmunizations(CDSOutput output) {
        List<Immunization> immunizations = new ArrayList<Immunization>();

        try {
            for (SubstanceAdministrationEvent event : output.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent()) {
                immunizations.add(this.immunizationConverter.convertToFhir(event));
            }
        } catch (NullPointerException exception) {
            this.logger.debug("getImmunizations", "No substance administration events found");
        }

        return immunizations;
    }

    /**
     * This method extracts a fhir Patient object from a CDSInput object. This data is contained inside
     * the EvaluatedPatient object.
     *
     * @param CDSInput input : the cds input object from which to extract the patient object
     * @return Patient
     */
    public Patient getPatient(CDSInput input) throws ParseException {
        return this.patientConverter.convertToFhir(input.getVmrInput().getPatient());
    }
}