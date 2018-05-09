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
public class FhirImmunization2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirImmunization2Vmr.class);

    public static void setImmunizationData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setImmunizationData ";
        JsonElement immunizationResourceElement;
        if (prefetchObject != null) {
            JsonObject immunizationElement = prefetchObject.getAsJsonObject("condition");
            immunizationResourceElement = immunizationElement.get("resource");
        } else {
            immunizationResourceElement = VmrUtils.retrieveResource(gson, fhirServer + "/Immunization?patient=" + patientId, accessToken);
        }
        logger.warn(METHODNAME, "immunizationResourceElement=", gson.toJson(immunizationResourceElement));
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle immunizations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationResourceElement));
            logger.warn(METHODNAME, "immunizations=", immunizations);
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : immunizations.getEntry()) {
                org.hl7.fhir.dstu3.model.Immunization immunization = (org.hl7.fhir.dstu3.model.Immunization) item.getResource();
                logger.warn(METHODNAME, "immunization=", immunization);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle immunizations = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationResourceElement));
                logger.warn(METHODNAME, "immunizations=", immunizations);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : immunizations.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.Immunization immunization = (ca.uhn.fhir.model.dstu2.resource.Immunization) item.getResource();
                    logger.warn(METHODNAME, "immunization=", immunization);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }
}
