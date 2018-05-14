package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Date;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.FhirConstants;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.LogUtils;

/**
 *
 * @author sdn
 */
public class FhirPatient2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirPatient2Vmr.class);

    public static void setPatientData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setPatientData ";
        JsonObject patientObject = null;
        if (prefetchObject != null) {
            JsonObject patientNodeObject = VmrUtils.getJsonObject(prefetchObject, "patient");
            if (patientNodeObject != null) {
                JsonObject patientResourceObject = VmrUtils.getJsonObject(patientNodeObject, "resource");
                if (patientResourceObject != null) {
                    patientObject = VmrUtils.getJsonObject(patientResourceObject, "patient");
                } else {
                    patientObject = VmrUtils.getJsonObject(patientNodeObject, "patient");
                }
                if (patientObject == null) {
                    logger.warn(METHODNAME, "patientObject is null!");
                }
            } else {
                logger.warn(METHODNAME, "patientNodeObject is null!");
            }
        }

        if (patientObject == null) {
            patientObject = VmrUtils.retrieveResource(gson, fhirServer + "Patient/" + patientId, accessToken);
            logger.info(METHODNAME, "patientObject=", gson.toJson(patientObject));
        }
        if (patientObject != null) {

            FhirContext ctx = FhirContext.forDstu3();
            try {
                org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) ctx.newJsonParser().parseResource(gson.toJson(patientObject));
                if (patient != null) {
                    Date birthDate = patient.getBirthDate();
                    org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender gender = patient.getGender();
                    logger.debug(METHODNAME, "birthDate=", birthDate);
                    logger.debug(METHODNAME, "gender=", gender);
                    if (patient.getGender() != null) {
                        input.setPatientGender(patient.getGender().toCode(), FhirConstants.GENDER_CODE_SYSTEM);
                    }
                    if (patient.getBirthDate() != null) {
                        input.setPatientBirthTime(patient.getBirthDate());
                    }
                }
                logger.debug(METHODNAME, "patient=", patient);
            } catch (Exception e) {
                logger.error(e);
                ctx = FhirContext.forDstu2();
                ca.uhn.fhir.model.dstu2.resource.Patient patient = (ca.uhn.fhir.model.dstu2.resource.Patient) ctx.newJsonParser().parseResource(gson.toJson(patientObject));
                if (patient != null) {
                    Date birthDate = patient.getBirthDate();
                    String gender = patient.getGender();
                    logger.debug(METHODNAME, "birthDate=", birthDate);
                    logger.debug(METHODNAME, "gender=", gender);
                    if (patient.getGender() != null) {
                        input.setPatientGender(patient.getGender(), FhirConstants.GENDER_CODE_SYSTEM);
                    }
                    if (patient.getBirthDate() != null) {
                        input.setPatientBirthTime(patient.getBirthDate());
                    }
                }
                logger.debug(METHODNAME, "patient=", patient);
            }
        } else {
            logger.error(METHODNAME, "patientObject is null!!!");
        }
    }
}
