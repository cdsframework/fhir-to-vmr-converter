package org.cdsframework.messageconverter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirCondition2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirDiagnosticReport2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirEncounter2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirImmunization2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirObservation2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirPatient2Vmr;
import org.cdsframework.messageconverter.fhir.convert.vmr.FhirProcedure2Vmr;
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
        Gson gson = new Gson();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // get the patient id
        JsonPrimitive patientIdObject = jsonObject.getAsJsonPrimitive("patient");
        String patientId = patientIdObject.getAsString();
        logger.warn(METHODNAME, "patientId=", patientId);

        // get the fhir server url
        JsonPrimitive fhirServerObject = jsonObject.getAsJsonPrimitive("fhirServer");
        String fhirServer = fhirServerObject.getAsString();
        if (!fhirServer.endsWith("/")) {
            fhirServer = fhirServer + "/";
        }
        logger.warn(METHODNAME, "fhirServer=", fhirServer);

        // get the fhir server access token
        JsonObject fhirAuthorizationObject = jsonObject.getAsJsonObject("fhirAuthorization");
        String accessToken = null;
        if (fhirAuthorizationObject != null) {
            JsonPrimitive accessTokenOnject = fhirAuthorizationObject.getAsJsonPrimitive("access_token");
            accessToken = accessTokenOnject.getAsString();
        }
        logger.warn(METHODNAME, "accessToken=", accessToken);

        // get the prefetch object
        JsonObject prefetchObject = jsonObject.getAsJsonObject("prefetch");
        logger.debug(METHODNAME, "prefetchObject=", prefetchObject);

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
