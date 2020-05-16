package org.cdsframework.messageconverter.fhir.convert.vmr;

import java.util.List;

import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.FhirConstants;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;

/**
 * @author sdn
 */
public class PatientConverter implements FhirConverter {
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
}