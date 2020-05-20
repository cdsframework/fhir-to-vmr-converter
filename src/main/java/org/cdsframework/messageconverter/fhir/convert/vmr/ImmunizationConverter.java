package org.cdsframework.messageconverter.fhir.convert.vmr;

import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Immunization;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;

/**
 * @author sdn
 */
public class ImmunizationConverter implements FhirConverter {
    /**
     * Convert a json object of fhir data to cds format. Save the results to the ice cds input wrapper.
     * 
     * @param IceCdsInputWrapper wrapper : wrapper object, used to store immunization data
     * @param JsonObject data : a json object of fhir data
     * @return IceCdsInputWrapper object updated with fhir data
     */    
    public IceCdsInputWrapper convertToCds(IceCdsInputWrapper wrapper, JsonObject data) {
        Immunization immunization = this.convertToFhir(data);

        if (immunization.hasOccurrence()
            && immunization.hasOccurrenceDateTimeType()
            && immunization.hasId()
            && immunization.hasVaccineCode()) {

            CodeableConcept vaccineCode = immunization.getVaccineCode();
            Coding code = vaccineCode.getCodingFirstRep();

            if (code != null) {
                String substanceCodeOid = VmrUtils.getOid(code.getSystem());

                if (substanceCodeOid != null) {
                    String c = code.getCode();
                    String root = immunization.getId();

                    wrapper.addSubstanceAdministrationEvent(
                        c,
                        substanceCodeOid,
                        immunization.getOccurrenceDateTimeType().getValue(),
                        root, 
                        Config.getCodeSystemOid("ADMINISTRATION_ID")
                    );
                }
            }
        }

        return wrapper;        
    }

    /**
     * Convert a json object of fhir data to cds format. Save the results to the cds input wrapper.
     * 
     * @param CdsInputWrapper wrapper : wrapper object, used to store immunization data
     * @param JsonObject data : a json object of fhir data
     * @return CdsInputWrapper object updated with fhir data
     */    
    public CdsInputWrapper convertToCds(CdsInputWrapper wrapper, JsonObject data) {
        IceCdsInputWrapper iceInput = new IceCdsInputWrapper(wrapper);
        iceInput = this.convertToCds(iceInput, data);

        return iceInput.getCdsInputWrapper();
    }

    /**
     * To make parsing the immunization data easier, convert to an immunization object to easily get
     * the data out.
     * 
     * @param JsonObject data : the immunization fhir data
     * @return a immunization object populated via the fhir data
     */    
    public Immunization convertToFhir(JsonObject data) {
        FhirContext ctx = FhirContext.forR4();

        // Create a parser and configure it to use the strict error handler
        IParser parser = ctx.newJsonParser();
        parser.setParserErrorHandler(new StrictErrorHandler());

        // get the string representation of the json object
        String str = data.toString();
                
        // The following will throw a DataFormatException because of the StrictErrorHandler
        Immunization immunization = parser.parseResource(Immunization.class, str);
        return immunization;
    }

    /*
    public static void setImmunizationData(CdsInputWrapper input, JsonObject prefetchObject, Gson gson, String patientId, String fhirServer, String accessToken, List<String> errorList) {
        final String METHODNAME = "setImmunizationData ";
        IceCdsInputWrapper iceInput = new IceCdsInputWrapper(input);
        JsonObject immunizationObject = VmrUtils.getJsonObjectFromPrefetchOrServer(prefetchObject, "Immunization", gson, patientId, fhirServer, accessToken);
        if (immunizationObject != null) {

            FhirContext ctx = FhirContext.forDstu3();
            try {
                org.hl7.fhir.dstu3.model.Bundle immunizations = (org.hl7.fhir.dstu3.model.Bundle) ctx.newJsonParser().parseResource(gson.toJson(immunizationObject));
                List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent> entry = immunizations.getEntry();
                logger.info(METHODNAME, "dstu3 entries=", entry);
                logger.info(METHODNAME, "entry.isEmpty()=", entry.isEmpty());
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
                    logger.info(METHODNAME, "dstu2 immunizations=", immunizations);
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
    */
}
