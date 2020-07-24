package org.cdsframework.messageconverter.fhir.convert.vmr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.cdsframework.util.LogUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationDateCriterionComponent;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.vmr.v1_0.schema.AdministrableSubstance;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.II;
import org.opencds.vmr.v1_0.schema.IVLTS;
import org.opencds.vmr.v1_0.schema.ObservationResult;
import org.opencds.vmr.v1_0.schema.ObservationResult.ObservationValue;
import org.opencds.vmr.v1_0.schema.RelatedClinicalStatement;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationProposal;

/**
 * @author Brian Lamb
 */
public class ImmunizationRecommendationConverter {
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected PatientConverter patientConverter = new PatientConverter();
    protected IdentifierFactory identifierFactory = new IdentifierFactory();

    protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmdd");

    private final LogUtils logger = LogUtils.getLogger(ImmunizationRecommendationConverter.class);

    /**
     * Extract the data from a CDSOutput object and put it into a FHIR compatible ImmunizationRecommendation
     * object.
     *
     * @param CDSOutput data : object containing data for an immunization recommendation
     * @return ImmunizationRecommendation
     */
    public ImmunizationRecommendation convertToFhir(Patient patient, SubstanceAdministrationProposal proposal) {
        ImmunizationRecommendation recommendation = new ImmunizationRecommendation();
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        recommendation.setPatientTarget(patient);

        try {
            recommendation.setId(proposal.getId().getRoot());
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "Cannot set id");
        }

        try {
            IVLTS proposedTimeInterval = proposal.getProposedAdministrationTimeInterval();

            String low = proposedTimeInterval.getLow();
            String high = proposedTimeInterval.getHigh();

            if (low != null && !low.isEmpty()) {
                Date proposedDate = this.dateFormat.parse(proposedTimeInterval.getLow());
                ImmunizationRecommendationRecommendationDateCriterionComponent recommendedTime = new ImmunizationRecommendationRecommendationDateCriterionComponent();

                CodeableConcept lowConcept = new CodeableConcept();
                lowConcept.setText("low");

                recommendedTime.setValue(proposedDate);
                recommendedTime.setCode(lowConcept);

                component.addDateCriterion(recommendedTime);
            }

            if (high != null && !high.isEmpty()) {
                Date proposedDate = this.dateFormat.parse(proposedTimeInterval.getHigh());

                ImmunizationRecommendationRecommendationDateCriterionComponent recommendedTime = new ImmunizationRecommendationRecommendationDateCriterionComponent();

                CodeableConcept lowConcept = new CodeableConcept();
                lowConcept.setText("high");

                recommendedTime.setValue(proposedDate);
                recommendedTime.setCode(lowConcept);

                component.addDateCriterion(recommendedTime);
            }
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "Cannot set proposed date");
        } catch (ParseException exception) {
            this.logger.debug("convertToFhir", "Improperly formatted date");
        }

        // add in the template ids
        for (II templateId : proposal.getTemplateId()) {
            Identifier identifier = this.identifierFactory.create("templateId", templateId.getRoot());
            recommendation.addIdentifier(identifier);
        }

        try {
            CD generalPurpose = proposal.getSubstanceAdministrationGeneralPurpose();
            CodeableConcept concept = this.codeableConceptConverter.convertToFhir(generalPurpose);

            component.addContraindicatedVaccineCode(concept);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "Cannot set substance administration general purpose");
        }

        // add in the vaccine
        try {
            // if we can't extract the vaccine code, log it but continue
            CD proposalVaccineCode = proposal.getSubstance().getSubstanceCode();
            CodeableConcept vaccineCode = this.codeableConceptConverter.convertToFhir(proposalVaccineCode);

            try {
                vaccineCode.setId(proposal.getSubstance().getId().getRoot());
            } catch (NullPointerException exception) {
                this.logger.debug("convertToFhir", "Cannot set vaccine code id");
            }

            component.addVaccineCode(vaccineCode);
        } catch (NullPointerException exception) {
            logger.debug("convertToFhir", "No vaccine code found in packet.");
        }

        // if no observation results, we are done
        if (proposal.getRelatedClinicalStatement().isEmpty()) {
            recommendation.addRecommendation(component);
            return recommendation;
        }

        for (RelatedClinicalStatement relatedClinicalStatement : proposal.getRelatedClinicalStatement()) {
            ImmunizationRecommendationRecommendationComponent newRecommendation = component.copy();
            ObservationResult observationResult = relatedClinicalStatement.getObservationResult();

            if (observationResult == null) {
                continue;
            }

            try {
                newRecommendation.setId(observationResult.getId().getRoot());
            } catch (NullPointerException exception) {
                this.logger.debug("convertToFhir", "Cannot set recommendation id");
            }

            try {
                CD proposalTargetDisease = observationResult.getObservationFocus();
                CodeableConcept disease = this.codeableConceptConverter.convertToFhir(proposalTargetDisease);
                newRecommendation.setTargetDisease(disease);
            } catch (NullPointerException exception) {
                this.logger.debug("convertToFhir", "Cannot set target disease");
            }

            for (II templateId : observationResult.getTemplateId()) {
                Identifier identifier = this.identifierFactory.create("observationTemplateId", templateId.getRoot());
                identifier.setSystem(observationResult.getId().getRoot());

                recommendation.addIdentifier(identifier);
            }

            try {
                CD proposalForecast = observationResult.getObservationValue().getConcept();
                CodeableConcept forecast = this.codeableConceptConverter.convertToFhir(proposalForecast);
                newRecommendation.setForecastStatus(forecast);
            } catch (NullPointerException exception) {
                this.logger.debug("convertToFhir", "Cannot add forecast reason");
            }

            for (CD interpretation : observationResult.getInterpretation()) {
                CodeableConcept forecastReason = this.codeableConceptConverter.convertToFhir(interpretation);
                newRecommendation.addForecastReason(forecastReason);
            }

            recommendation.addRecommendation(newRecommendation);
        }

        if (recommendation.getRecommendation().size() == 0) {
            recommendation.addRecommendation(component);
        }

        return recommendation;
    }

    /**
     * This method converts a FHIR ImmunizationRecommendation object into a CDS SubstanceAdministrationProposal
     * object. The data is extracted from the ImmunizationRecommnedation object and placed in the appropriate
     * places in the SubstanceAdministrationProposal object.
     *
     * @param ImmunizationRecommendation recommendation : the FHIR ImmunizationRecommendation object
     * @return SubstanceAdministrationProposal
     */
    public SubstanceAdministrationProposal convertToCds(ImmunizationRecommendation recommendation) {
        SubstanceAdministrationProposal proposal = new SubstanceAdministrationProposal();
        boolean hasSetDate = false;

        // set the id
        II id = new II();
        id.setRoot(recommendation.getId());
        proposal.setId(id);

        // add in the template ids
        for (Identifier identifier : recommendation.getIdentifier()) {
            if (identifier.getType().getText().equals("templateId")) {
                II templateId = new II();
                templateId.setRoot(identifier.getValue());

                proposal.getTemplateId().add(templateId);
            }
        }

        for (ImmunizationRecommendationRecommendationComponent component : recommendation.getRecommendation()) {
            if (proposal.getProposedAdministrationTimeInterval() == null) {
                IVLTS proposedTimeInterval = new IVLTS();
                hasSetDate = false;

                for (ImmunizationRecommendationRecommendationDateCriterionComponent dateCriterion : component.getDateCriterion()) {
                    String type = dateCriterion.getCode().getText();

                    if (type.equals("low")) {
                        String proposed = this.dateFormat.format(dateCriterion.getValue());
                        proposedTimeInterval.setLow(proposed);
                        hasSetDate = true;
                    } else if (type.equals("high")) {
                        String proposed = this.dateFormat.format(dateCriterion.getValue());
                        proposedTimeInterval.setHigh(proposed);
                        hasSetDate = true;
                    }
                }

                if (hasSetDate) {
                    proposal.setProposedAdministrationTimeInterval(proposedTimeInterval);
                }
            }

            // while it's possible for there to be multiples of these, we are only storing one
            CodeableConcept contraindicatedVaccineCode = component.getContraindicatedVaccineCodeFirstRep();

            if (!contraindicatedVaccineCode.isEmpty()) {
                CD administrationGeneralPurpose = this.codeableConceptConverter.convertToCds(contraindicatedVaccineCode);
                proposal.setSubstanceAdministrationGeneralPurpose(administrationGeneralPurpose);
            }

            CodeableConcept vaccineCode = component.getVaccineCodeFirstRep();

            if (!vaccineCode.isEmpty()) {
                CD substanceCode = this.codeableConceptConverter.convertToCds(vaccineCode);
                AdministrableSubstance substance = new AdministrableSubstance();

                II substanceCodeId = new II();
                substanceCodeId.setRoot(vaccineCode.getId());
                substance.setId(substanceCodeId);

                substance.setSubstanceCode(substanceCode);
                proposal.setSubstance(substance);
            }

            // these go with observation results
            RelatedClinicalStatement relatedClinicalStatement = new RelatedClinicalStatement();
            ObservationResult observationResult = new ObservationResult();
            relatedClinicalStatement.setObservationResult(observationResult);

            // get the id for the observation result
            II observationResultId = new II();
            observationResultId.setRoot(component.getId());
            observationResult.setId(observationResultId);

            // get the observation focus
            CodeableConcept targetDisease = component.getTargetDisease();

            if (!targetDisease.isEmpty()) {
                CD observationFocus = this.codeableConceptConverter.convertToCds(targetDisease);
                observationResult.setObservationFocus(observationFocus);
            }

            // get the template id
            for (Identifier identifier : recommendation.getIdentifier()) {
                try {
                    if (identifier.getType().getText().equals("observationTemplateId")
                        && identifier.getSystem().equals(observationResultId.getRoot())
                    ) {
                        II templateId = new II();
                        templateId.setRoot(identifier.getValue());
                        observationResult.getTemplateId().add(templateId);
                    }
                } catch (NullPointerException exception) {
                    this.logger.debug("convertToCds", "Cannot set template id");
                }
            }

            // get the observation value
            CodeableConcept forecastStatus = component.getForecastStatus();

            if (!forecastStatus.isEmpty()) {
                CD observationValueConcept = this.codeableConceptConverter.convertToCds(forecastStatus);
                ObservationValue observationValue = new ObservationValue();

                observationValue.setConcept(observationValueConcept);
                observationResult.setObservationValue(observationValue);
            }

            for (CodeableConcept forecastReason : component.getForecastReason()) {
                CD interpretation = this.codeableConceptConverter.convertToCds(forecastReason);
                observationResult.getInterpretation().add(interpretation);
            }

            proposal.getRelatedClinicalStatement().add(relatedClinicalStatement);
        }

        return proposal;
    }
}