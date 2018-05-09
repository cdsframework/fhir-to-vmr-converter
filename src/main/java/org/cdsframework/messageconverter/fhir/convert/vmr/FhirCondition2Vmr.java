package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.util.LogUtils;

/**
 *
 * @author sdn
 */
public class FhirCondition2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirCondition2Vmr.class);

    public static void setConditionData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setConditionData ";
        JsonObject conditionElement = prefetchObject.getAsJsonObject("condition");
        JsonElement conditionResourceElement = conditionElement.get("resource");
        logger.warn(METHODNAME, "conditionResourceElement=", gson.toJson(conditionResourceElement));
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle conditions = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(conditionResourceElement));
            logger.warn(METHODNAME, "conditions=", conditions);
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : conditions.getEntry()) {
                org.hl7.fhir.dstu3.model.Condition condition = (org.hl7.fhir.dstu3.model.Condition) item.getResource();
                logger.warn(METHODNAME, "condition=", condition);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle conditions = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(conditionResourceElement));
                logger.warn(METHODNAME, "conditions=", conditions);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : conditions.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.Condition condition = (ca.uhn.fhir.model.dstu2.resource.Condition) item.getResource();
                    logger.warn(METHODNAME, "condition=", condition);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }
}
