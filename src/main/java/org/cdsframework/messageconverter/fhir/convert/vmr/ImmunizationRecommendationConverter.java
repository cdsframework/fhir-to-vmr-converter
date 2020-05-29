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
public class ImmunizationRecommendationConverter implements CdsOutputToFhirConverter {
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected PatientConverter patientConverter = new PatientConverter();

    private static final LogUtils logger = LogUtils.getLogger(ImmunizationRecommendationConverter.class);
    
    public ImmunizationRecommendation convertToFhir(CDSOutput data) {
        ImmunizationRecommendation recommendation = new ImmunizationRecommendation();

        try {
            Patient patient = this.patientConverter.convertToFhir(data.getVmrOutput().getPatient());
            recommendation.setPatientTarget(patient);
        } catch (NullPointerException exception) {
            logger.debug("convertToFhir", "Null pointer exception found when accessing patient record");
            return recommendation;
        }            

        for (SubstanceAdministrationProposal proposal : data.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationProposals().getSubstanceAdministrationProposal()) {
            recommendation.addRecommendation(this.convertToFhir(proposal));
        }
        
        return recommendation;
    }

    public ImmunizationRecommendationRecommendationComponent convertToFhir(SubstanceAdministrationProposal proposal) {
        ImmunizationRecommendationRecommendationComponent recommendation = new ImmunizationRecommendationRecommendationComponent();

        try {
            CD proposalVaccineCode = proposal.getSubstance().getSubstanceCode();
            CodeableConcept vaccineCode = this.codeableConceptConverter.convertToFhir(proposalVaccineCode);
            recommendation.addVaccineCode(vaccineCode);
        } catch (NullPointerException exception) {
            logger.debug("convertToFhir", "No vaccine code found in packet.");
        }

        try {
            CD proposalTargetDisease = proposal.getRelatedClinicalStatement().get(0).getObservationResult().getObservationFocus();
            CodeableConcept disease = this.codeableConceptConverter.convertToFhir(proposalTargetDisease);
            recommendation.setTargetDisease(disease);
        } catch (NullPointerException exception) {
            logger.debug("convertToFhir", "No proposed disease found.");
        } catch (IndexOutOfBoundsException exception) {
            logger.debug("convertToFhir", "No related clinical statements found.");
        }

        try {
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