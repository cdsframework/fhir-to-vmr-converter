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
public class FhirDiagnosticReport2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirDiagnosticReport2Vmr.class);

    public static void setDiagnosticReportData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setDiagnosticReportData ";
        JsonElement diagnosticReportResourceElement;
        if (prefetchObject != null && prefetchObject.getAsJsonObject("diagnosticreport") != null) {
            JsonObject diagnosticReportElement = prefetchObject.getAsJsonObject("diagnosticreport");
            diagnosticReportResourceElement = diagnosticReportElement.get("resource");
        } else {
            diagnosticReportResourceElement = VmrUtils.retrieveResource(gson, fhirServer + "DiagnosticReport?patient=" + patientId, accessToken);
        }
        logger.debug(METHODNAME, "diagnosticReportResourceElement=", gson.toJson(diagnosticReportResourceElement));
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle diagnosticReports = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(diagnosticReportResourceElement));
            logger.debug(METHODNAME, "diagnosticReports=", diagnosticReports);
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : diagnosticReports.getEntry()) {
                org.hl7.fhir.dstu3.model.DiagnosticReport diagnosticReport = (org.hl7.fhir.dstu3.model.DiagnosticReport) item.getResource();
                logger.debug(METHODNAME, "diagnosticReport=", diagnosticReport);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle diagnosticReports = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(diagnosticReportResourceElement));
                logger.debug(METHODNAME, "diagnosticReports=", diagnosticReports);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : diagnosticReports.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.DiagnosticReport diagnosticReport = (ca.uhn.fhir.model.dstu2.resource.DiagnosticReport) item.getResource();
                    logger.debug(METHODNAME, "diagnosticReport=", diagnosticReport);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }
}
