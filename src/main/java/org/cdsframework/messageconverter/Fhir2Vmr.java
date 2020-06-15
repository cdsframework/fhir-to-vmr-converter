package org.cdsframework.messageconverter;

import java.util.ArrayList;
import java.util.List;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.vmr.ImmunizationConverter;
import org.cdsframework.messageconverter.fhir.convert.vmr.PatientConverter;
import org.cdsframework.util.LogUtils;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONObject;
import org.json.XML;
import org.opencds.vmr.v1_0.schema.CDSInput;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.ObservationResults;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.SubstanceAdministrationEvents;
import org.opencds.vmr.v1_0.schema.ObservationResult;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;
import org.opencds.vmr.v1_0.schema.VMR;
/**
 * @author sdn
 */
public class Fhir2Vmr {
    private static final LogUtils logger = LogUtils.getLogger(Fhir2Vmr.class);
    private final List<String> errorList = new ArrayList<>();

    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected PatientConverter patientConverter = new PatientConverter();

    /**
     * Convert string into a JSONObject. This is used to validate fhir elements
     * later and ensure that the data has the appropriate properties. The string
     * object can be either json or xml formatted data.
     * 
     * @param String data the data to convert to a JSONObject
     * @return a json object containing the data in String data
     */
    protected JSONObject createFhirElement(String data) {
        if (logger.isDebugEnabled()) {
            final String METHODNAME = "Fhir2Vmr ";
            logger.debug(METHODNAME, "payload=", data);
        }

        // the data may be in xml, if so, convert to json
        if (data.startsWith("<")) {
            return XML.toJSONObject(data);
        }

        JSONObject json = new JSONObject(data);
        return json;
    }

    /**
     * @see createFhirElement(String)
     */
    protected JSONObject createFhirElement(byte[] data) {
        String payload = new String(data);
        return this.createFhirElement(payload);
    }

    /**
     * Get the value of errorList
     *
     * @return the value of errorList
     */
    public List<String> getErrorList() {
        return this.errorList;
    }

    /**
     * Convert fhir data as json object into cds formatted data. This uses several
     * converter objects to convert each respective structure definition.
     * 
     * @param CdsInputWrapper wrapper : the wrapper object that will be returned
     *                        containing the json data
     * @param JSONObject      fhirElement : the fhir data converted to a json object
     * @return CDSInput element containing the data in the fhir json object
     */
    public CDSInput getCdsInputFromFhir(CdsInputWrapper wrapper, JSONObject fhirElement) {
        // currently, this is a parameters resource
        // @TODO when the spec is adopted, use the hapi fhir library
        // the interesting part is in the parameters array
        if (!fhirElement.has("parameter")) {
            throw new IllegalArgumentException();
        }

        for (Object element : fhirElement.getJSONArray("parameter")) {
            JSONObject object = new JSONObject(element.toString());

            if (object.has("name") && object.has("resource")) {
                // this should be a primitive
                switch (object.getString("name")) {
                    case "immunization":
                        // convert immunization data
                        wrapper = this.immunizationConverter.convertToCds(wrapper, object.getJSONObject("resource"));
                        break;

                    case "patient":
                        // convert patient data
                        wrapper = this.patientConverter.convertToCds(wrapper, object.getJSONObject("resource"));
                        break;
                }
            }
        }

        return wrapper.getCdsObject();
    }

    /**
     * @see getCdsInputFromFhir(Patient, List<Immunization>, List<Immunization>)
     */
    public CDSInput getCdsInputFromFhir(Patient patient) {
        CDSInput input = new CDSInput();
        VMR vmr = new VMR();

        EvaluatedPerson evaluatedPerson = this.patientConverter.convertToCds(patient);

        vmr.setPatient(evaluatedPerson);

        input.setVmrInput(vmr);

        return input;
    }
    /**
     * @see getCdsInputFromFhir(Patient, List<Immunization>, List<Immunization>)
     */
    public CDSInput getCdsInputFromFhir(Patient patient, List<Immunization> observations) {
        CDSInput input = this.getCdsInputFromFhir(patient);
        ObservationResults observationResults = new ObservationResults();
        ClinicalStatements clinicalStatements = new ClinicalStatements();

        for (Immunization immunization : observations) {
            ObservationResult observation = this.immunizationConverter.convertToCdsObservation(immunization);
            observationResults.getObservationResult().add(observation);
        }
        
        input.getVmrInput().getPatient().setClinicalStatements(clinicalStatements);
        clinicalStatements.setObservationResults(observationResults);
        
        return input;
    }

    /**
     * This method combines various FHIR objects into a CDSInput object. It uses a patient object, a list of immunizations
     * and observations and sets them in the appropriate context for the CDSInput object.
     * 
     * @param Patient patient : the patient object containing demographic information
     * @param List<Immunization> immunizations : a list of immunizations recommended
     * @param List<Immunization> observations : a list of immunizations received already
     * @return CDSInput
     */
    public CDSInput getCdsInputFromFhir(Patient patient, List<Immunization> immunizations, List<Immunization> observations) {
        CDSInput input = this.getCdsInputFromFhir(patient, observations);

        SubstanceAdministrationEvents substanceAdministrationEvents = new SubstanceAdministrationEvents();

        for (Immunization immunization : immunizations) {
            SubstanceAdministrationEvent event = this.immunizationConverter.convertToCds(immunization);
            substanceAdministrationEvents.getSubstanceAdministrationEvent().add(event);
        }

        if (!substanceAdministrationEvents.getSubstanceAdministrationEvent().isEmpty()) {
            input.getVmrInput().getPatient().getClinicalStatements().setSubstanceAdministrationEvents(substanceAdministrationEvents);
        }

        return input;
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JSONObject)
     */
    public CDSInput getCdsInputFromFhir(String data) {
        CdsInputWrapper wrapper = CdsInputWrapper.getCdsInputWrapper();
        JSONObject fhirElement = this.createFhirElement(data);

        return this.getCdsInputFromFhir(wrapper, fhirElement);
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JSONObject)
     */
    public CDSInput getCdsInputFromFhir(byte[] data) {
        CdsInputWrapper wrapper = CdsInputWrapper.getCdsInputWrapper();
        JSONObject fhirElement = this.createFhirElement(data);

        return this.getCdsInputFromFhir(wrapper, fhirElement);       
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JSONObject)
     */
    public CDSInput getCdsInputFromFhir(JSONObject data) {
        CdsInputWrapper wrapper = CdsInputWrapper.getCdsInputWrapper();

        return this.getCdsInputFromFhir(wrapper, data);
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JSONObject)
     */
    public CDSInput getCdsInputFromFhir(CdsInputWrapper wrapper, String data) {
        JSONObject fhirElement = this.createFhirElement(data);
        return this.getCdsInputFromFhir(wrapper, fhirElement);
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JSONObject)
     */
    public CDSInput getCdsInputFromFhir(CdsInputWrapper wrapper, byte[] data) {
        JSONObject fhirElement = this.createFhirElement(data);
        return this.getCdsInputFromFhir(wrapper, fhirElement);        
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JSONObject)
     */
    public CDSInput getCdsInputFromFhir(IceCdsInputWrapper wrapper, String data) {
        JSONObject fhirElement = this.createFhirElement(data);

        this.getCdsInputFromFhir(wrapper.getCdsInputWrapper(), fhirElement);
        return wrapper.getCdsInput();
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JSONObject)
     */
    public CDSInput getCdsInputFromFhir(IceCdsInputWrapper wrapper, byte[] data) {
        JSONObject fhirElement = this.createFhirElement(data);

        this.getCdsInputFromFhir(wrapper.getCdsInputWrapper(), fhirElement);
        return wrapper.getCdsInput();
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JSONObject)
     */
    public CDSInput getCdsInputFromFhir(IceCdsInputWrapper wrapper, JSONObject data) {
        this.getCdsInputFromFhir(wrapper.getCdsInputWrapper(), data);
        return wrapper.getCdsInput();
    }
}