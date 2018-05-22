package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import com.google.gson.Gson;
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

    public static void setObservationData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken, List<String> errorList) {
        final String METHODNAME = "setObservationData ";
        JsonObject observationObject = VmrUtils.getJsonObjectFromPrefetchOrServer(prefetchObject, "Observation", gson, patientId, fhirServer, accessToken);
        if (observationObject != null) {
            FhirContext ctx = FhirContext.forDstu3();
            try {
                org.hl7.fhir.dstu3.model.Bundle observations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(observationObject));
                List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent> entry = observations.getEntry();
                if (entry.isEmpty()) {
                    observationObject = VmrUtils.getMissingData(gson, "Observation", patientId, fhirServer, accessToken);
                    observations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(observationObject));
                    entry = observations.getEntry();
                }
//            logger.warn(METHODNAME, "observationResourceElement=", gson.toJson(observationResourceElement));
                for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : entry) {
                    org.hl7.fhir.dstu3.model.Observation observation = (org.hl7.fhir.dstu3.model.Observation) item.getResource();
                    setDstu3ObservationOnCdsInput(observation, input);
                }
            } catch (ConfigurationException | DataFormatException | FHIRException e) {
                logger.error(e);
                ctx = FhirContext.forDstu2();
                try {
                    ca.uhn.fhir.model.dstu2.resource.Bundle observations = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(observationObject));
                    logger.debug(METHODNAME, "observations=", observations);
                    observations.getEntry().stream().map((item) -> (ca.uhn.fhir.model.dstu2.resource.Observation) item.getResource()).forEachOrdered((observation) -> {
                        setDstu2ObservationOnCdsInput(observation, input);
                    });
                } catch (ConfigurationException | DataFormatException ex) {
                    logger.error(ex);
                    errorList.add(ex.getMessage());
                }
            }
        } else {
            logger.error(METHODNAME, "observationObject is null!!!");
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
            if (logger.isDebugEnabled()) {
                logger.debug(METHODNAME, "observation.hasExtension()=", observation.hasExtension());
                logger.debug(METHODNAME, "observation.hasId()=", observation.hasId());
                logger.debug(METHODNAME, "observation.hasEffectiveDateTimeType()=", observation.hasEffectiveDateTimeType());
                logger.debug(METHODNAME, "observation.hasCode()=", observation.hasCode());
                logger.debug(METHODNAME, "observation.hasValueCodeableConcept()=", observation.hasValueCodeableConcept());
                logger.debug(METHODNAME, "observation.hasComponent()=", observation.hasComponent());
            }
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
            String effectiveDate = VmrUtils.getDateString(observation.getEffectiveDateTimeType());
            logger.debug(METHODNAME, "effectiveDate=", effectiveDate);

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
