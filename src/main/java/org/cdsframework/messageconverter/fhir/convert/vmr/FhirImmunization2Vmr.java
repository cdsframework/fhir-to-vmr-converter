package org.cdsframework.messageconverter.fhir.convert.vmr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final Map<String, String> CODE_SYSTEM_MAP = new HashMap<>();

    static {
        CODE_SYSTEM_MAP.put("http://www2a.cdc.gov/vaccines/IIS/IISStandards/vaccines.asp?rpt=cvx", Config.getCodeSystemOid("VACCINE"));
        CODE_SYSTEM_MAP.put("http://hl7.org/fhir/sid/cvx", Config.getCodeSystemOid("VACCINE"));
    }

    public static void setImmunizationData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken) {
        final String METHODNAME = "setImmunizationData ";
        IceCdsInputWrapper iceInput = new IceCdsInputWrapper(input);
        JsonElement immunizationResourceElement = null;
        boolean dataMissing = true;
        if (prefetchObject != null && prefetchObject.getAsJsonObject("immunization") != null) {
            dataMissing = false;
            JsonObject immunizationElement = prefetchObject.getAsJsonObject("immunization");
            immunizationResourceElement = immunizationElement.get("resource");
        }
        if (dataMissing) {
            immunizationResourceElement = VmrUtils.getMissingData(gson, "Immunization", patientId, fhirServer, accessToken);
        }
        FhirContext ctx = FhirContext.forDstu3();
        try {
            org.hl7.fhir.dstu3.model.Bundle immunizations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationResourceElement));
            List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent> entry = immunizations.getEntry();
            if (entry.isEmpty()) {
                immunizationResourceElement = VmrUtils.getMissingData(gson, "Immunization", patientId, fhirServer, accessToken);
                immunizations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationResourceElement));
                entry = immunizations.getEntry();
            }
//            logger.warn(METHODNAME, "immunizationResourceElement=", gson.toJson(immunizationResourceElement));
            for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent item : entry) {
                org.hl7.fhir.dstu3.model.Immunization immunization = (org.hl7.fhir.dstu3.model.Immunization) item.getResource();
                setDstu3ImmunizationOnCdsInput(immunization, iceInput);
            }
        } catch (Exception e) {
            logger.error(e);
            ctx = FhirContext.forDstu2();
            try {
                ca.uhn.fhir.model.dstu2.resource.Bundle immunizations = (ca.uhn.fhir.model.dstu2.resource.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationResourceElement));
                logger.debug(METHODNAME, "immunizations=", immunizations);
                for (ca.uhn.fhir.model.dstu2.resource.Bundle.Entry item : immunizations.getEntry()) {
                    ca.uhn.fhir.model.dstu2.resource.Immunization immunization = (ca.uhn.fhir.model.dstu2.resource.Immunization) item.getResource();
                    setDstu2ImmunizationOnCdsInput(immunization, iceInput);
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }

    private static void setDstu2ImmunizationOnCdsInput(ca.uhn.fhir.model.dstu2.resource.Immunization immunization, IceCdsInputWrapper iceInput) {
        final String METHODNAME = "setDstu3ImmunizationOnCdsInput ";
        logger.debug(METHODNAME, "immunization=", immunization);
        if (immunization != null
                && !immunization.getWasNotGiven()
                && immunization.getDate() != null
                && immunization.getVaccineCode() != null) {
            logger.warn(METHODNAME, "adding immunization ", immunization.getId());
            List<CodingDt> codingList = immunization.getVaccineCode().getCoding();
            if (!codingList.isEmpty()) {
                if (codingList.size() > 1) {
                    logger.warn(METHODNAME, "coding size is greater than 1! ", codingList);
                }
                CodingDt coding = codingList.get(0);
                String substanceCodeOid = CODE_SYSTEM_MAP.get(coding.getSystem());
                if (substanceCodeOid == null) {
                    logger.error(METHODNAME, "missing code system mapping: ", coding.getSystem());
                } else {
                    String code = coding.getCode();
                    String root = immunization.getId().getValue();
                    iceInput.addSubstanceAdministrationEvent(code, substanceCodeOid, immunization.getDate(), root, Config.getCodeSystemOid("ADMINISTRATION_ID"));
                }
            }
        }
    }

    private static void setDstu3ImmunizationOnCdsInput(org.hl7.fhir.dstu3.model.Immunization immunization, IceCdsInputWrapper iceInput) {
        final String METHODNAME = "setDstu3ImmunizationOnCdsInput ";
        if (immunization != null) {
            logger.debug(METHODNAME, "immunization=", immunization);
            logger.warn(METHODNAME, "immunization.getNotGiven()=", immunization.getNotGiven());
            logger.warn(METHODNAME, "immunization.hasDate()=", immunization.hasDate());
            logger.warn(METHODNAME, "immunization.hasVaccineCode()=", immunization.hasVaccineCode());
        }
        if (immunization != null
                && !immunization.getNotGiven()
                && immunization.hasDate()
                && immunization.hasId()
                && immunization.hasVaccineCode()) {
            logger.warn(METHODNAME, "adding immunization ", immunization.getId());
            Coding coding = VmrUtils.getFirstCoding(immunization.getVaccineCode());
            if (coding != null) {
                String substanceCodeOid = CODE_SYSTEM_MAP.get(coding.getSystem());
                if (substanceCodeOid == null) {
                    logger.error(METHODNAME, "missing code system mapping: ", coding.getSystem());
                } else {
                    String code = coding.getCode();
                    String root = immunization.getId();
                    iceInput.addSubstanceAdministrationEvent(code, substanceCodeOid, immunization.getDate(), root, Config.getCodeSystemOid("ADMINISTRATION_ID"));
                }
            }
        }
    }
}
