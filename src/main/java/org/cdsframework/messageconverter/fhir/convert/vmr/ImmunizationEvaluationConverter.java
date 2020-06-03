package org.cdsframework.messageconverter.fhir.convert.vmr;

import java.util.ArrayList;
import java.util.List;

import org.cdsframework.util.LogUtils;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;

/**
 * @author Brian Lamb
 */
public class ImmunizationEvaluationConverter implements CdsOutputToFhirListConverter {
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected PatientConverter patientConverter = new PatientConverter();

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
        Patient patient;

        try {
            // this is a simple conversion for now and simply extracts the id and creates the Patient object
            patient = this.patientConverter.convertToFhir(data.getVmrOutput().getPatient());
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "Null pointer exception found when accessing patient record");
            return evaluations;
        }

        for (SubstanceAdministrationEvent event : data.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent()) {
            ImmunizationEvaluation evaluation = this.convertToFhir(patient, event);
            evaluations.add(evaluation);
        }

        return evaluations;
    }

    /**
     * This method converts a SubstanceAdministrationEvent object into an ImmunizationEvaluation object.
     * It requires a Patient object to update the ImmunizationEvaluation object.
     * 
     * @param Patient patient : the patient object receiving the immunization
     * @param SubstanceAdministrationEvent event : the substance administration event object containing the evaluation data
     * @return ImmunizationEvaluation
     */
    public ImmunizationEvaluation convertToFhir(Patient patient, SubstanceAdministrationEvent event) {
        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();

        evaluation.setPatientTarget(patient);
        evaluation.setImmunizationEventTarget(this.immunizationConverter.convertToFhir(event));
   
        return evaluation;
    }
}