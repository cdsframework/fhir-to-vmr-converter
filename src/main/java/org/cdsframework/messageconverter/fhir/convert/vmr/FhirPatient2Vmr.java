package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Date;
import java.util.List;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.FhirConstants;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.LogUtils;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.StringType;

/**
 *
 * @author sdn
 */
public class FhirPatient2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirPatient2Vmr.class);

    public static void setPatientData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken, List<String> errorList) {
        final String METHODNAME = "setPatientData ";
        input.setPatientId(patientId);
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
            if (logger.isDebugEnabled()) {
                logger.debug(METHODNAME, "patientObject=", gson.toJson(patientObject));
            }
        }
        if (patientObject != null) {

            FhirContext ctx = FhirContext.forDstu3();
            try {
                org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) ctx.newJsonParser().parseResource(gson.toJson(patientObject));
                if (patient != null) {
                    Date birthDate = patient.getBirthDate();
                    org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender gender = patient.getGender();
                    HumanName humanName = patient.getNameFirstRep();
                    List<StringType> givenNames = humanName.getGiven();
                    StringType givenName = givenNames.get(0);
                    String familyName = humanName.getFamily();
                    logger.info(METHODNAME, "givenName=", givenName);
                    logger.info(METHODNAME, "familyName=", familyName);
                    logger.debug(METHODNAME, "birthDate=", birthDate);
                    logger.debug(METHODNAME, "gender=", gender);
                    if (patient.getGender() != null) {
                        input.setPatientGender(patient.getGender().toCode(), FhirConstants.GENDER_CODE_SYSTEM);
                    }
                    if (patient.getBirthDate() != null) {
                        input.setPatientBirthTime(patient.getBirthDate());
                    }
                    input.setPatientName(givenName.asStringValue(), familyName);
                }
                logger.debug(METHODNAME, "patient=", patient);
            } catch (ConfigurationException | DataFormatException e) {
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
            errorList.add(logger.error(METHODNAME, "patientObject is null!!!"));
        }
    }
}
