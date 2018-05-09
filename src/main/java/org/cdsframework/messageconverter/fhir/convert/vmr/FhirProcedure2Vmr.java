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
public class FhirProcedure2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirProcedure2Vmr.class);

    public static void setProcedureData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setProcedureData ";
        JsonObject procedureElement = prefetchObject.getAsJsonObject("procedure");
        JsonElement procedureResourceElement = procedureElement.get("resource");
        logger.warn(METHODNAME, "procedureResourceElement=", gson.toJson(procedureResourceElement));
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle procedures = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(procedureResourceElement));
            logger.warn(METHODNAME, "procedures=", procedures);
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : procedures.getEntry()) {
                org.hl7.fhir.dstu3.model.Procedure procedure = (org.hl7.fhir.dstu3.model.Procedure) item.getResource();
                logger.warn(METHODNAME, "procedure=", procedure);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle procedures = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(procedureResourceElement));
                logger.warn(METHODNAME, "procedures=", procedures);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : procedures.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.Procedure procedure = (ca.uhn.fhir.model.dstu2.resource.Procedure) item.getResource();
                    logger.warn(METHODNAME, "procedure=", procedure);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }
}
