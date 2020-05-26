package org.cdsframework.messageconverter.fhir.convert.vmr;

import java.util.List;

import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.FhirConstants;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;

/**
 * @author Brian Lamb
 */
public class PatientConverter implements FhirConverter {
    /**
     * Convert a json object of fhir data to cds format. Save the results to the ice cds input wrapper.
     * 
     * @param IceCdsInputWrapper wrapper : wrapper object, used to store patient data
     * @param JsonObject data : a json object of fhir data
     * @return IceCdsInputWrapper object updated with fhir data
     */
    public IceCdsInputWrapper convertToCds(IceCdsInputWrapper wrapper, JsonObject data) {
        this.convertToCds(wrapper.getCdsInputWrapper(), data);
        return wrapper;
    }

    /**
     * Convert a json object of fhir data to cds format. Save the results to the cds input wrapper.
     * 
     * @param CdsInputWrapper wrapper : wrapper object, used to store patient data
     * @param JsonObject data : a json object of fhir data
     * @return CdsInputWrapper object updated with fhir data
     */
    public CdsInputWrapper convertToCds(CdsInputWrapper wrapper, JsonObject data) {
        Patient patient = this.convertToFhir(data);

        HumanName humanName = patient.getNameFirstRep();
        List<StringType> givenNames = humanName.getGiven();
        StringType givenName = givenNames.get(0);
        String familyName = humanName.getFamily();

        if (patient.getGender() != null) {
            wrapper.setPatientGender(patient.getGender().toCode(), FhirConstants.GENDER_CODE_SYSTEM);
        }

        if (patient.getBirthDate() != null) {
            wrapper.setPatientBirthTime(patient.getBirthDate());
        }

        wrapper.setPatientName(givenName.asStringValue(), familyName);
        wrapper.setPatientId(patient.getId());

        return wrapper;
    }

    /**
     * To make parsing the patient data easier, convert to a patient object to easily get
     * the data out.
     * 
     * @param JsonObject data : the patient fhir data
     * @return a patient object populated via the fhir data
     */
    public Patient convertToFhir(JsonObject data) {
        FhirContext ctx = FhirContext.forR4();

        // Create a parser and configure it to use the strict error handler
        IParser parser = ctx.newJsonParser();
        parser.setParserErrorHandler(new StrictErrorHandler());

        // get the string representation of the json object
        String str = data.toString();
                
        // The following will throw a DataFormatException because of the StrictErrorHandler
        Patient patient = parser.parseResource(Patient.class, str);
        return patient;       
    }

    /**
     * Convert an EvaluatedPerson from a VMR record to the FHIR version of the Patient. For now, it 
     * only saves the id to be used as a reference but this can be updated to include additional
     * metadata.
     * 
     * @param EvaluatedPerson person : the evaluated person from a VMR record
     * @return a patient object
     */
    public Patient convertToFhir(EvaluatedPerson person) {
        Patient patient = new Patient();

        patient.setId(person.getId().getRoot());

        return patient;
    }
}