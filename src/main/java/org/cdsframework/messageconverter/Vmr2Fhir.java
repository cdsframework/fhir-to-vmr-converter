package org.cdsframework.messageconverter;

import java.util.List;

import org.cdsframework.messageconverter.fhir.convert.vmr.ImmunizationEvaluationConverter;
import org.cdsframework.messageconverter.fhir.convert.vmr.ImmunizationRecommendationConverter;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.opencds.vmr.v1_0.schema.CDSOutput;

/**
 * @author Brian Lamb
 */
public class Vmr2Fhir {
    protected ImmunizationRecommendationConverter immunizationRecommendationConverter = new ImmunizationRecommendationConverter();
    protected ImmunizationEvaluationConverter immunizationEvaluationConverter = new ImmunizationEvaluationConverter();

    /**
     * This method converts a CDSOutput object into a list of ImmunizationEvaluation objects. This data is contained
     * inside of SubstanceAdministrationEvents.
     * 
     * @param CDSOutput output : object containing recommendations and evaluations
     * @return List<ImmunizationEvaluation>
     */
    public List<ImmunizationEvaluation> getEvaluations(CDSOutput output) {
        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(output);
        return evaluations;
    }

    /**
     * This method extracts the data from a CDSOutput object to form an ImmunizationRecommendation fhir 
     * object. This data is contained inside SubstanceAdministrationProposal objects.
     * 
     * @param ImmunizationRecommendation output : object containing an immunization recommendation request
     * @return ImmunizationRecommendation
     */
    public ImmunizationRecommendation getRecommendation(CDSOutput output) {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(output);
        return recommendation;
    }
}