package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import org.cdsframework.cds.util.CdsObjectFactory;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.LogUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.vmr.v1_0.schema.II;
import org.opencds.vmr.v1_0.schema.Problem;

/**
 * Convert Condition to Problem.
 *
 * @author sdn
 */
public class FhirCondition2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirCondition2Vmr.class);

    public static void setConditionData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setConditionData ";
        JsonElement conditionResourceElement = null;
        boolean dataMissing = true;
        if (prefetchObject != null && prefetchObject.getAsJsonObject("condition") != null) {
            dataMissing = false;
            JsonObject conditionElement = prefetchObject.getAsJsonObject("condition");
            conditionResourceElement = conditionElement.get("resource");
        }
        if (dataMissing) {
            conditionResourceElement = VmrUtils.getMissingData(gson, "Condition", patientId, fhirServer, accessToken);
        }
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle conditions = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(conditionResourceElement));
            List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent> entry = conditions.getEntry();
            if (entry.isEmpty()) {
                conditionResourceElement = VmrUtils.getMissingData(gson, "Condition", patientId, fhirServer, accessToken);
                conditions = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(conditionResourceElement));
                entry = conditions.getEntry();
            }
//            logger.warn(METHODNAME, "conditionResourceElement=", gson.toJson(conditionResourceElement));
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : entry) {
                org.hl7.fhir.dstu3.model.Condition condition = (org.hl7.fhir.dstu3.model.Condition) item.getResource();
                setDstu3ConditionOnCdsInput(condition, input);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle conditions = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(conditionResourceElement));
                logger.debug(METHODNAME, "conditions=", conditions);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : conditions.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.Condition condition = (ca.uhn.fhir.model.dstu2.resource.Condition) item.getResource();
                    setDstu2ConditionOnCdsInput(condition, input);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }

    private static void setDstu2ConditionOnCdsInput(ca.uhn.fhir.model.dstu2.resource.Condition condition, CdsInputWrapper input) {
        final String METHODNAME = "setDstu2ObservationOnCdsInput ";
        logger.debug(METHODNAME, "condition=", condition);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void setDstu3ConditionOnCdsInput(org.hl7.fhir.dstu3.model.Condition condition, CdsInputWrapper input) throws FHIRException {
        final String METHODNAME = "setDstu3ObservationOnCdsInput ";
        if (condition != null) {
            logger.warn(METHODNAME, "condition=", condition);
            logger.warn(METHODNAME, "condition.hasExtension()=", condition.hasExtension());
            logger.warn(METHODNAME, "condition.hasId()=", condition.hasId());
            logger.warn(METHODNAME, "condition.hasOnsetDateTimeType()=", condition.hasOnsetDateTimeType());
            logger.warn(METHODNAME, "condition.hasCode()=", condition.hasCode());
            logger.warn(METHODNAME, "condition.hasClinicalStatus()=", condition.hasClinicalStatus());
            if (condition.hasClinicalStatus()) {
                logger.warn(METHODNAME, "condition.getClinicalStatus().toCode()=", condition.getClinicalStatus().toCode());
            }
        }
        if (condition != null
                && !condition.hasExtension()
                && condition.hasCode()
                && condition.hasId()
                && condition.hasClinicalStatus()
                && condition.hasOnsetDateTimeType()
                && "active".equalsIgnoreCase(condition.getClinicalStatus().toCode())) {

            // id
            String id = condition.getId();

            // onset date
            org.hl7.fhir.dstu3.model.DateTimeType onsetDateTimeType = condition.getOnsetDateTimeType();
            Integer year = onsetDateTimeType.getYear();
            Integer month = onsetDateTimeType.getMonth();
            Integer day = onsetDateTimeType.getDay();
            String valueAsString = onsetDateTimeType.getValueAsString();
            logger.warn(METHODNAME, "year=", year);
            logger.warn(METHODNAME, "month=", month);
            logger.warn(METHODNAME, "day=", day);
            logger.warn(METHODNAME, "valueAsString=", valueAsString);
            logger.warn(METHODNAME, "valueAsString=", valueAsString.substring(0, 10).replace("-", ""));
            String onsetDateTime = valueAsString.substring(0, 10).replace("-", "");

            addDstu3ProblemToCdsInput(
                    input,
                    VmrUtils.getFirstCoding(condition.getCode()),
                    onsetDateTime,
                    id);
        }
    }

    private static void addDstu3ProblemToCdsInput(
            CdsInputWrapper input,
            org.hl7.fhir.dstu3.model.Coding problemCoding,
            String onsetDateTime,
            String id) {

        String problemCodeCode = problemCoding.getCode();
        String problemCodeCodeDisplayName = problemCoding.getDisplay();
        String problemCodeCodeSystem = problemCoding.getSystem();
        List<Problem> problems = input.getInstanceProblems().getProblem();
        Problem problem = CdsObjectFactory.getProblem(
                problemCodeCode,
                problemCodeCodeDisplayName,
                problemCodeCodeSystem,
                "",
                "active",
                "Active",
                "2.16.840.1.113883.3.1937.98.5.8",
                "",
                onsetDateTime,
                onsetDateTime);

        II ii = new II();
        ii.setExtension(id);
        ii.setRoot("0.0.0.0");
        problem.setId(ii);

        problems.add(problem);
    }
}
