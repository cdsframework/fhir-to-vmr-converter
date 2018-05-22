package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.parser.DataFormatException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.LogUtils;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.dstu3.model.Coding;

/**
 *
 * @author sdn
 */
public class FhirImmunization2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(FhirImmunization2Vmr.class);

    public static void setImmunizationData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken, List<String> errorList) {
        final String METHODNAME = "setImmunizationData ";
        IceCdsInputWrapper iceInput = new IceCdsInputWrapper(input);
        JsonObject immunizationObject = VmrUtils.getJsonObjectFromPrefetchOrServer(prefetchObject, "Immunization", gson, patientId, fhirServer, accessToken);
        if (immunizationObject != null) {

            FhirContext ctx = FhirContext.forDstu3();
            try {
                org.hl7.fhir.dstu3.model.Bundle immunizations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationObject));
                List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent> entry = immunizations.getEntry();
                if (entry.isEmpty()) {
                    immunizationObject = VmrUtils.getMissingData(gson, "Immunization", patientId, fhirServer, accessToken);
                    immunizations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationObject));
                    entry = immunizations.getEntry();
                }
                for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : entry) {
                    org.hl7.fhir.dstu3.model.Immunization immunization = (org.hl7.fhir.dstu3.model.Immunization) item.getResource();
                    setDstu3ImmunizationOnCdsInput(immunization, iceInput, errorList);
                }
            } catch (ConfigurationException | DataFormatException e) {
                logger.error(e);
                ctx = FhirContext.forDstu2();
                try {
                    ca.uhn.fhir.model.dstu2.resource.Bundle immunizations = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationObject));
                    logger.debug(METHODNAME, "immunizations=", immunizations);
                    immunizations.getEntry().stream().map((item) -> (ca.uhn.fhir.model.dstu2.resource.Immunization) item.getResource()).forEachOrdered((immunization) -> {
                        setDstu2ImmunizationOnCdsInput(immunization, iceInput, errorList);
                    });
                } catch (ConfigurationException | DataFormatException ex) {
                    logger.error(ex);
                    errorList.add(ex.getMessage());
                }
            }
        } else {
            logger.error(METHODNAME, "immunizationObject was null!!!");
        }
    }

    private static void setDstu2ImmunizationOnCdsInput(ca.uhn.fhir.model.dstu2.resource.Immunization immunization, IceCdsInputWrapper iceInput, List<String> errorList) {
        final String METHODNAME = "setDstu3ImmunizationOnCdsInput ";
        logger.debug(METHODNAME, "immunization=", immunization);
        if (immunization != null
                && !immunization.getWasNotGiven()
                && immunization.getDate() != null
                && immunization.getVaccineCode() != null) {
            logger.debug(METHODNAME, "adding immunization ", immunization.getId());
            List<CodingDt> codingList = immunization.getVaccineCode().getCoding();
            if (!codingList.isEmpty()) {
                if (codingList.size() > 1) {
                    errorList.add(logger.warn(METHODNAME, "coding size is greater than 1! ", codingList));
                }
                CodingDt coding = codingList.get(0);
                String substanceCodeOid = VmrUtils.getOid(coding.getSystem());
                if (substanceCodeOid == null) {
                    errorList.add(logger.error(METHODNAME, "missing code system mapping: ", coding.getSystem()));
                } else {
                    String code = coding.getCode();
                    String root = immunization.getId().getValue();
                    iceInput.addSubstanceAdministrationEvent(code, substanceCodeOid, immunization.getDate(), root, Config.getCodeSystemOid("ADMINISTRATION_ID"));
                }
            }
        }
    }

    private static void setDstu3ImmunizationOnCdsInput(org.hl7.fhir.dstu3.model.Immunization immunization, IceCdsInputWrapper iceInput, List<String> errorList) {
        final String METHODNAME = "setDstu3ImmunizationOnCdsInput ";
        if (immunization != null) {
            if (logger.isDebugEnabled()) {
                logger.debug(METHODNAME, "immunization=", immunization);
                logger.debug(METHODNAME, "immunization.getNotGiven()=", immunization.getNotGiven());
                logger.debug(METHODNAME, "immunization.hasDate()=", immunization.hasDate());
                logger.debug(METHODNAME, "immunization.hasVaccineCode()=", immunization.hasVaccineCode());
            }
        }
        if (immunization != null
                && !immunization.getNotGiven()
                && immunization.hasDate()
                && immunization.hasId()
                && immunization.hasVaccineCode()) {
            logger.debug(METHODNAME, "adding immunization ", immunization.getId());
            Coding coding = VmrUtils.getFirstCoding(immunization.getVaccineCode());
            if (coding != null) {
                String substanceCodeOid = VmrUtils.getOid(coding.getSystem());
                if (substanceCodeOid == null) {
                    errorList.add(logger.error(METHODNAME, "missing code system mapping: ", coding.getSystem()));
                } else {
                    String code = coding.getCode();
                    String root = immunization.getId();
                    iceInput.addSubstanceAdministrationEvent(code, substanceCodeOid, immunization.getDate(), root, Config.getCodeSystemOid("ADMINISTRATION_ID"));
                }
            }
        }
    }
}
