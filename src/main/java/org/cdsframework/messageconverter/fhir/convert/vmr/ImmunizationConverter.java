package org.cdsframework.messageconverter.fhir.convert.vmr;

import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.LogUtils;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Immunization;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;

/**
 * @author sdn
 */
public class ImmunizationConverter implements CdsConverter, JsonToFhirConverter {
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    private final LogUtils logger = LogUtils.getLogger(ImmunizationConverter.class);

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

    /**
     * Immunization data can be located in a substance administration event object. This method
     * converts that object to an Immunization data record.
     * 
     * @param SubstanceAdministrationEvent event
     * @return Immunization
     */
    public Immunization convertToFhir(SubstanceAdministrationEvent event) {
        Immunization immunization = new Immunization();

        try {
            immunization.setVaccineCode(
                this.codeableConceptConverter.convertToFhir(event.getSubstance().getSubstanceCode())
            );
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "Null pointer exception found when accessing substance code");
        }

        return immunization;
    }
}