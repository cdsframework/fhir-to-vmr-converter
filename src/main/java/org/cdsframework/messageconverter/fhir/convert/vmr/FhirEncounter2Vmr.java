package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.LogUtils;

/**
 *
 * @author sdn
 */
public class FhirEncounter2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirEncounter2Vmr.class);

    public static void setEncounterData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setEncounterData ";
        JsonElement encounterResourceElement;
        if (prefetchObject != null) {
            JsonObject encounterElement = prefetchObject.getAsJsonObject("condition");
            encounterResourceElement = encounterElement.get("resource");
        } else {
            encounterResourceElement = VmrUtils.retrieveResource(gson, fhirServer + "/Encounter?patient=" + patientId, accessToken);
        }
        logger.warn(METHODNAME, "encounterResourceElement=", gson.toJson(encounterResourceElement));
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle encounters = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(encounterResourceElement));
            logger.warn(METHODNAME, "encounters=", encounters);
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : encounters.getEntry()) {
                org.hl7.fhir.dstu3.model.Encounter encounter = (org.hl7.fhir.dstu3.model.Encounter) item.getResource();
                logger.warn(METHODNAME, "encounter=", encounter);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle encounters = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(encounterResourceElement));
                logger.warn(METHODNAME, "encounters=", encounters);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : encounters.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.Encounter encounter = (ca.uhn.fhir.model.dstu2.resource.Encounter) item.getResource();
                    logger.warn(METHODNAME, "ecounter=", encounter);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }
}
