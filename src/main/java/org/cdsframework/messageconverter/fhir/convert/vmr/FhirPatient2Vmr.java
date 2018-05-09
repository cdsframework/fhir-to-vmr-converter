package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Date;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.FhirConstants;
import org.cdsframework.util.LogUtils;

/**
 *
 * @author sdn
 */
public class FhirPatient2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirPatient2Vmr.class);

    public static void setPatientData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setPatientData ";
        JsonObject patientObject = prefetchObject.getAsJsonObject("patient");
        JsonElement patientResourceElement = patientObject.get("resource");
        logger.debug(METHODNAME, "patientResourceElement=", gson.toJson(patientResourceElement));
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) ctx.newJsonParser().parseResource(gson.toJson(patientResourceElement));
            if (patient != null) {
                Date birthDate = patient.getBirthDate();
                org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender gender = patient.getGender();
                logger.warn(METHODNAME, "birthDate=", birthDate);
                logger.warn(METHODNAME, "gender=", gender);
                if (patient.getGender() != null) {
                    input.setPatientGender(patient.getGender().toCode(), FhirConstants.GENDER_CODE_SYSTEM);
                }
                if (patient.getBirthDate() != null) {
                    input.setPatientBirthTime(patient.getBirthDate());
                }
            }
            logger.warn(METHODNAME, "patient=", patient);
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            ca.uhn.fhir.model.dstu2.resource.Patient patient = (ca.uhn.fhir.model.dstu2.resource.Patient) ctx.newJsonParser().parseResource(gson.toJson(patientResourceElement));
            if (patient != null) {
                Date birthDate = patient.getBirthDate();
                String gender = patient.getGender();
                logger.warn(METHODNAME, "birthDate=", birthDate);
                logger.warn(METHODNAME, "gender=", gender);
                if (patient.getGender() != null) {
                    input.setPatientGender(patient.getGender(), FhirConstants.GENDER_CODE_SYSTEM);
                }
                if (patient.getBirthDate() != null) {
                    input.setPatientBirthTime(patient.getBirthDate());
                }
            }
            logger.warn(METHODNAME, "patient=", patient);

        }
    }
}
