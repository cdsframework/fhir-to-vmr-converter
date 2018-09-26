package org.cdsframework.messageconverter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
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
public class Fhir2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(Fhir2Vmr.class);
    private final List<String> errorList = new ArrayList<>();
    private final JsonElement fhirElement;
    private final Gson gson = new Gson();

    /**
     * Get the value of fhirElement
     *
     * @return the value of fhirElement
     */
    public JsonElement getFhirElement() {
        return fhirElement;
    }

    public Fhir2Vmr(byte[] payload) {
        final String METHODNAME = "Fhir2Vmr ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "payload=", new String(payload));
        }
        fhirElement = gson.fromJson(new String(payload), JsonElement.class);
    }

    /**
     * Get the value of errorList
     *
     * @return the value of errorList
     */
    public List<String> getErrorList() {
        return errorList;
    }

    public CDSInput getCdsInputFromFhir() {
        final String METHODNAME = "getCdsInputFromFhir ";
        JsonObject jsonObject = getFhirElement().getAsJsonObject();

        // get the patient id
        String patientId = null;

        // get the patient id out of the context (DSTU3)
        JsonObject contextObject = VmrUtils.getJsonObject(jsonObject, "context");
        if (contextObject != null) {
            patientId = VmrUtils.getJsonObjectAsString(contextObject, "patientId");
        } else {
            logger.error(METHODNAME, "contextObject is null!!!");
        }

        // fall back and check for it in the patient node (DSTU2)
        if (patientId == null) {
            patientId = VmrUtils.getJsonObjectAsString(jsonObject, "patient");
            logger.warn(METHODNAME, "got patient id from patient node - not context!");
        }
        if (patientId == null) {
            getErrorList().add(logger.error(METHODNAME, "patientId is null!!!"));
        }
        logger.warn(METHODNAME, "patientId=", patientId);

        // get the fhir server url
        String fhirServer = VmrUtils.getJsonObjectAsString(jsonObject, "fhirServer");
        if (fhirServer != null) {
            if (!fhirServer.endsWith("/")) {
                fhirServer = fhirServer + "/";
            }
        } else {
            getErrorList().add(logger.warn(METHODNAME, "fhirServer is null!"));
        }
        logger.debug(METHODNAME, "fhirServer=", fhirServer);

        // get the fhir server access token
        JsonObject fhirAuthorizationObject = VmrUtils.getJsonObject(jsonObject, "fhirAuthorization");

        String accessToken = null;
        if (fhirAuthorizationObject != null) {
            accessToken = VmrUtils.getJsonObjectAsString(fhirAuthorizationObject, "access_token");
        } else {
            logger.warn(METHODNAME, "fhirAuthorizationObject is null!");
        }
        logger.debug(METHODNAME, "accessToken=", accessToken);

        // get the prefetch object
        JsonObject prefetchObject = VmrUtils.getJsonObject(jsonObject, "prefetch");
        logger.debug(METHODNAME, "prefetchObject=", prefetchObject);

        CdsInputWrapper input = CdsInputWrapper.getCdsInputWrapper();

        CDSInput result = input.getCdsObject();

        VmrUtils.setSystemUserType(result, getFhirElement(), getErrorList());
        FhirPatient2Vmr.setPatientData(input, prefetchObject, gson, patientId, fhirServer, accessToken, getErrorList());
        FhirCondition2Vmr.setConditionData(input, prefetchObject, gson, patientId, fhirServer, accessToken, getErrorList());
        FhirImmunization2Vmr.setImmunizationData(input, prefetchObject, gson, patientId, fhirServer, accessToken, getErrorList());
        FhirObservation2Vmr.setObservationData(input, prefetchObject, gson, patientId, fhirServer, accessToken, getErrorList());

        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "result=", CdsObjectAssist.cdsObjectToString(result, CDSInput.class));
        }
        return result;
    }
}
