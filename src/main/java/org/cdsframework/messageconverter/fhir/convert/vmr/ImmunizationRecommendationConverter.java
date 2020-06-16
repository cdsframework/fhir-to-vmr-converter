package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.cdsframework.util.LogUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.RelatedClinicalStatement;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationProposal;

/**
 * @author Brian Lamb
 */
public class ImmunizationRecommendationConverter {
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected PatientConverter patientConverter = new PatientConverter();

    private static final LogUtils logger = LogUtils.getLogger(ImmunizationRecommendationConverter.class);
    
    /**
     * Extract the data from a CDSOutput object and put it into a FHIR compatible ImmunizationRecommendation
     * object. 
     * 
     * @param CDSOutput data : object containing data for an immunization recommendation
     * @return ImmunizationRecommendation
     */
    public ImmunizationRecommendation convertToFhir(CDSOutput data) {
        ImmunizationRecommendation recommendation = new ImmunizationRecommendation();

        try {
            // this is a simple conversion for now and simply extracts the id and creates the Patient object
            Patient patient = this.patientConverter.convertToFhir(data.getVmrOutput().getPatient());
            recommendation.setPatientTarget(patient);
        } catch (NullPointerException exception) {
            logger.debug("convertToFhir", "Null pointer exception found when accessing patient record");
            return recommendation;
        } catch (IllegalArgumentException exception) {
            logger.debug("convertToFhir", "Unknown gender code");
        }   

        // pass each proposal into method to create the ImmunizationRecommendationRecommendationComponent object
        for (SubstanceAdministrationProposal proposal : data.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationProposals().getSubstanceAdministrationProposal()) {
            recommendation.addRecommendation(this.convertToFhir(proposal));
        }
        
        return recommendation;
    }

    /**
     * Each substance propsal represents a recommendation. Extract the necessary data and build it into 
     * an ImmunizationRecommendationRecommendationComponent object which is added to the ImmunizationRecommendation
     * object.
     * 
     * @param SubstanceAdministrationProposal proposal : the proposal containing recommendations for immunizations
     * @return ImmunizationRecommendationRecommendationComponent
     */
    public ImmunizationRecommendationRecommendationComponent convertToFhir(SubstanceAdministrationProposal proposal) {
        ImmunizationRecommendationRecommendationComponent recommendation = new ImmunizationRecommendationRecommendationComponent();

        try {
            // if we can't extract the vaccine code, log it but continue
            CD proposalVaccineCode = proposal.getSubstance().getSubstanceCode();
            CodeableConcept vaccineCode = this.codeableConceptConverter.convertToFhir(proposalVaccineCode);
            recommendation.addVaccineCode(vaccineCode);
        } catch (NullPointerException exception) {
            logger.debug("convertToFhir", "No vaccine code found in packet.");
        }

        try {
            // if we can't extract the target disease, log it but continue
            CD proposalTargetDisease = proposal.getRelatedClinicalStatement().get(0).getObservationResult().getObservationFocus();
            CodeableConcept disease = this.codeableConceptConverter.convertToFhir(proposalTargetDisease);
            recommendation.setTargetDisease(disease);
        } catch (NullPointerException exception) {
            logger.debug("convertToFhir", "No proposed disease found.");
        } catch (IndexOutOfBoundsException exception) {
            logger.debug("convertToFhir", "No related clinical statements found.");
        }

        try {
            // add each forecast reason but if we can't extract it, log it and continue
            for (RelatedClinicalStatement statement : proposal.getRelatedClinicalStatement()) {
                CD proposalForecast = statement.getObservationResult().getObservationValue().getConcept();
                CodeableConcept forecast = this.codeableConceptConverter.convertToFhir(proposalForecast);
                recommendation.addForecastReason(forecast);    
            }
        } catch (NullPointerException exception) {
            logger.debug("convertToFhir", "No forecast data found.");
        }

        return recommendation;
    }
}