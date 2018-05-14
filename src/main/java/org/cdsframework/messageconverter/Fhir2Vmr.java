package org.cdsframework.messageconverter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirCondition2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirImmunization2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirObservation2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirPatient2Vmr;
import org.cdsframework.util.LogUtils;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 *
 * @author sdn
 */
class Fhir2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(Fhir2Vmr.class);

    static CDSInput getCdsInputFromFhir(String json) {
        final String METHODNAME = "getCdsInputFromFhir ";
        logger.warn(METHODNAME, "json=", json);
        Gson gson = new Gson();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // get the patient id
        String patientId = null;

        // get the patient id out of the context
        JsonObject contextObject = VmrUtils.getJsonObject(jsonObject, "context");
        if (contextObject != null) {
            patientId = VmrUtils.getJsonObjectAsString(contextObject, "patientId");
        } else {
            logger.error(METHODNAME, "contextObject is null!!!");
        }

        // fall back and check for it in the patient node
        if (patientId == null) {
            patientId = VmrUtils.getJsonObjectAsString(jsonObject, "patient");
            logger.warn(METHODNAME, "got patient id from patient node - not context!");
        }

        if (patientId == null) {
            logger.error(METHODNAME, "patientId is null!!!");
        }

        logger.warn(METHODNAME, "patientId=", patientId);

        // get the fhir server url
        String fhirServer = VmrUtils.getJsonObjectAsString(jsonObject, "fhirServer");
        if (fhirServer != null) {
            if (!fhirServer.endsWith("/")) {
                fhirServer = fhirServer + "/";
            }
        } else {
            logger.warn(METHODNAME, "fhirServer is null!");
        }
        logger.warn(METHODNAME, "fhirServer=", fhirServer);

        // get the fhir server access token
        JsonObject fhirAuthorizationObject = VmrUtils.getJsonObject(jsonObject, "fhirAuthorization");

        String accessToken = null;
        if (fhirAuthorizationObject != null) {
            accessToken = VmrUtils.getJsonObjectAsString(fhirAuthorizationObject, "access_token");
        } else {
            logger.warn(METHODNAME, "fhirAuthorizationObject is null!");
        }
        logger.warn(METHODNAME, "accessToken=", accessToken);

        // get the prefetch object
        JsonObject prefetchObject = VmrUtils.getJsonObject(jsonObject, "prefetch");
        logger.warn(METHODNAME, "prefetchObject=", prefetchObject);

        CdsInputWrapper input = CdsInputWrapper.getCdsInputWrapper();

        CDSInput result = input.getCdsObject();

        VmrUtils.setSystemUserType(result);
        FhirPatient2Vmr.setPatientData(input, prefetchObject, gson, patientId, fhirServer, accessToken);
        FhirCondition2Vmr.setConditionData(input, prefetchObject, gson, patientId, fhirServer, accessToken);
        //FhirDiagnosticReport2Vmr.setDiagnosticReportData(input, prefetchObject, gson, patientId, fhirServer, accessToken);
        //FhirEncounter2Vmr.setEncounterData(input, prefetchObject, gson, patientId, fhirServer, accessToken);
        FhirImmunization2Vmr.setImmunizationData(input, prefetchObject, gson, patientId, fhirServer, accessToken);
        FhirObservation2Vmr.setObservationData(input, prefetchObject, gson, patientId, fhirServer, accessToken);
        //FhirProcedure2Vmr.setProcedureData(input, prefetchObject, gson, patientId, fhirServer, accessToken);

        String cdsObjectToString = CdsObjectAssist.cdsObjectToString(result, CDSInput.class);
        logger.warn(METHODNAME, "result=", cdsObjectToString);
        return result;
    }
}
