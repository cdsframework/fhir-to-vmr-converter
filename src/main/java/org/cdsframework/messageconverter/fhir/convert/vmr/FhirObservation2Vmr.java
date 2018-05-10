package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.LogUtils;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.exceptions.FHIRException;

/**
 *
 * @author sdn
 */
public class FhirObservation2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirObservation2Vmr.class);

    public static void setObservationData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setObservationData ";
        JsonElement observationResourceElement = null;
        boolean dataMissing = true;
        if (prefetchObject != null & prefetchObject.getAsJsonObject("observation") != null) {
            dataMissing = false;
            JsonObject observationElement = prefetchObject.getAsJsonObject("observation");
            observationResourceElement = observationElement.get("resource");
        }
        if (dataMissing) {
            observationResourceElement = VmrUtils.getMissingData(gson, "Observation", patientId, fhirServer, accessToken);
        }
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle observations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(observationResourceElement));
            List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent> entry = observations.getEntry();
            if (entry.isEmpty()) {
                observationResourceElement = VmrUtils.getMissingData(gson, "Observation", patientId, fhirServer, accessToken);
                observations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(observationResourceElement));
                entry = observations.getEntry();
            }
//            logger.warn(METHODNAME, "observationResourceElement=", gson.toJson(observationResourceElement));
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : entry) {
                org.hl7.fhir.dstu3.model.Observation observation = (org.hl7.fhir.dstu3.model.Observation) item.getResource();
                setDstu3ObservationOnCdsInput(observation, input);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle observations = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(observationResourceElement));
                logger.debug(METHODNAME, "observations=", observations);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : observations.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.Observation observation = (ca.uhn.fhir.model.dstu2.resource.Observation) item.getResource();
                    setDstu2ObservationOnCdsInput(observation, input);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }

        // add phony zika result for demonstrative purposes
        input.addObservationResult(
                "80823-8",
                "Zika virus IgM Ab [Presence] in Cerebral spinal fluid by Immunoassay",
                "2.16.840.1.113883.6.1",
                "LOINC",
                "46651001",
                "Isolated (qualifier value)",
                "2.16.840.1.113883.6.96",
                "SNOMED-CT",
                "16ba0827869e721fdf3bc6a3ac820d55",
                "4.1.1.1");
    }

    private static void setDstu2ObservationOnCdsInput(ca.uhn.fhir.model.dstu2.resource.Observation observation, CdsInputWrapper input) {
        final String METHODNAME = "setDstu2ObservationOnCdsInput ";
        logger.debug(METHODNAME, "observation=", observation);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void setDstu3ObservationOnCdsInput(org.hl7.fhir.dstu3.model.Observation observation, CdsInputWrapper input) throws FHIRException {
        final String METHODNAME = "setDstu3ObservationOnCdsInput ";
        if (observation != null) {
            logger.warn(METHODNAME, "observation.hasExtension()=", observation.hasExtension());
            logger.warn(METHODNAME, "observation.hasId()=", observation.hasId());
            logger.warn(METHODNAME, "observation.hasEffectiveDateTimeType()=", observation.hasEffectiveDateTimeType());
            logger.warn(METHODNAME, "observation.hasCode()=", observation.hasCode());
            logger.warn(METHODNAME, "observation.hasValueCodeableConcept()=", observation.hasValueCodeableConcept());
            logger.warn(METHODNAME, "observation.hasComponent()=", observation.hasComponent());
        }
        if (observation != null
                && !observation.hasExtension()
                && observation.hasId()
                && observation.hasEffectiveDateTimeType()
                && ((observation.hasCode() && observation.hasValueCodeableConcept())
                || observation.hasComponent())) {

            // id
            String id = observation.getId();

            // effective date
            org.hl7.fhir.dstu3.model.DateTimeType effectiveDateTimeType = observation.getEffectiveDateTimeType();
            Integer year = effectiveDateTimeType.getYear();
            Integer month = effectiveDateTimeType.getMonth();
            Integer day = effectiveDateTimeType.getDay();
            String valueAsString = effectiveDateTimeType.getValueAsString();
            logger.warn(METHODNAME, "year=", year);
            logger.warn(METHODNAME, "month=", month);
            logger.warn(METHODNAME, "day=", day);
            logger.warn(METHODNAME, "valueAsString=", valueAsString);
            logger.warn(METHODNAME, "valueAsString=", valueAsString.substring(0, 10).replace("-", ""));
            String effectiveDate = valueAsString.substring(0, 10).replace("-", "");

            if (observation.hasCode() && observation.hasValueCodeableConcept()) {

                if ((observation.hasCategory() && VmrUtils.isCategoryMatch(observation.getCategory(), "laboratory"))
                        || !observation.hasCategory()) {
                    addDstu3ObservationToCdsInput(
                            input,
                            VmrUtils.getFirstCoding(observation.getCode()),
                            VmrUtils.getFirstCoding(observation.getValueCodeableConcept()),
                            effectiveDate,
                            id);
                }
            } else if (observation.hasComponent()) {
                for (org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent component
                        : observation.getComponent()) {
                    if (component != null && component.hasCode() && component.hasValueCodeableConcept()) {
                        addDstu3ObservationToCdsInput(
                                input,
                                VmrUtils.getFirstCoding(component.getCode()),
                                VmrUtils.getFirstCoding(component.getValueCodeableConcept()),
                                effectiveDate,
                                id);
                    }
                }
            }
        }
    }

    private static void addDstu3ObservationToCdsInput(CdsInputWrapper input, org.hl7.fhir.dstu3.model.Coding focusCoding, org.hl7.fhir.dstu3.model.Coding valueCoding, String effectiveDate, String id) {
        String focusCode = focusCoding.getCode();
        String focusDisplayName = focusCoding.getDisplay();
        String focusOid = VmrUtils.getOid(focusCoding.getSystem());

        String valueCode = valueCoding.getCode();
        String valueDisplayName = valueCoding.getDisplay();
        String valueOid = VmrUtils.getOid(valueCoding.getSystem());

        input.addObservationResult(focusCode,
                focusDisplayName,
                focusOid,
                "",
                valueCode,
                valueDisplayName,
                valueOid,
                "",
                effectiveDate,
                effectiveDate,
                id,
                Config.getCodeSystemOid("ADMINISTRATION_ID"));
    }
}
