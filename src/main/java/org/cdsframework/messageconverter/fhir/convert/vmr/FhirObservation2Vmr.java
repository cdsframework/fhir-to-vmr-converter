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
public class FhirObservation2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirObservation2Vmr.class);

    public static void setObservationData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setObservationData ";
        JsonElement observationResourceElement;
        if (prefetchObject != null) {
            JsonObject observationElement = prefetchObject.getAsJsonObject("condition");
            observationResourceElement = observationElement.get("resource");
        } else {
            observationResourceElement = VmrUtils.retrieveResource(gson, fhirServer + "/Observation?patient=" + patientId, accessToken);
        }
        logger.warn(METHODNAME, "observationResourceElement=", gson.toJson(observationResourceElement));
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle observations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(observationResourceElement));
            logger.warn(METHODNAME, "observations=", observations);
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : observations.getEntry()) {
                org.hl7.fhir.dstu3.model.Observation observation = (org.hl7.fhir.dstu3.model.Observation) item.getResource();
                logger.warn(METHODNAME, "observation=", observation);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle observations = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(observationResourceElement));
                logger.warn(METHODNAME, "observations=", observations);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : observations.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.Observation observation = (ca.uhn.fhir.model.dstu2.resource.Observation) item.getResource();
                    logger.warn(METHODNAME, "observation=", observation);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }
}
