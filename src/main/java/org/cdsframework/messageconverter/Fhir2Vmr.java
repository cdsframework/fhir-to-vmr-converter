/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cdsframework.messageconverter;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Date;
import org.cdsframework.cds.util.CdsObjectFactory;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.util.LogUtils;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 *
 * @author sdn
 */
class Fhir2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(Fhir2Vmr.class);
    private static final String CDSINPUT_TEMPLATE_ID = "2.16.840.1.113883.3.1829.11.1.1.8";
    private static final String SYSTEMUSERTYPE_CODESYSTEM = "2.16.840.1.113883.3.795.5.4.12.2.1";
    private static final String PROVIDER_FACILITY = "PROVIDER_FACILITY";
    private static final String GENDER_CODE_SYSTEM = "2.16.840.1.113883.1.11.1";

    static CDSInput getCdsInputFromFhir(String json) {
        final String METHODNAME = "getCdsInputFromFhir ";
        Gson gson = new Gson();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        logger.debug(METHODNAME, "jsonElement=", jsonElement);

        CdsInputWrapper input = CdsInputWrapper.getCdsInputWrapper();

        CDSInput result = input.getCdsObject();

        setSystemUserType(result);
        setPatientData(input, jsonElement, gson);

        return result;
    }

    private static void setPatientData(CdsInputWrapper input, JsonElement jsonElement, Gson gson) {
        final String METHODNAME = "setPatientData ";
        Patient patient = null;
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject patientElement = jsonObject.getAsJsonObject("patient");
        JsonElement patientResourceElement = patientElement.get("resource");
        logger.debug(METHODNAME, "patientResourceElement=", gson.toJson(patientResourceElement));
        FhirContext ctx = FhirContext.forDstu3();
        try {
            patient = (Patient) ctx.newJsonParser().parseResource(gson.toJson(patientResourceElement));
            logger.warn(METHODNAME, "patient=", patient);
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                patient = (Patient) ctx.newJsonParser().parseResource(gson.toJson(patientResourceElement));
                logger.warn(METHODNAME, "patient=", patient);
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
        if (patient != null) {
            Date birthDate = patient.getBirthDate();
            Enumerations.AdministrativeGender gender = patient.getGender();
            logger.warn(METHODNAME, "birthDate=", birthDate);
            logger.warn(METHODNAME, "gender=", gender);
            if (patient.getGender() != null) {
                input.setPatientGender(patient.getGender().toCode(), GENDER_CODE_SYSTEM);
            }
            if (patient.getBirthDate() != null) {
                input.setPatientBirthTime(patient.getBirthDate());
            }
        }
    }

    private static void setSystemUserType(CDSInput cdsInput) {

        cdsInput.getTemplateId().clear();
        cdsInput.getTemplateId().add(CdsObjectFactory.getII(CDSINPUT_TEMPLATE_ID));

        cdsInput.getCdsContext().setCdsInformationRecipientPreferredLanguage(null);

        CD systemUserType = new CD();
        systemUserType.setCodeSystem(SYSTEMUSERTYPE_CODESYSTEM);
        systemUserType.setCode(PROVIDER_FACILITY);
        cdsInput.getCdsContext().setCdsSystemUserType(systemUserType);
    }
}
