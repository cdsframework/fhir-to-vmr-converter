package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public class AdministrativeGenderConverter implements CDConverter<AdministrativeGender> {
    /**
     * Convert a CD object into AdministrativeGender.
     * 
     * @param CD code : the CD object containing the gender
     * @return AdministrativeGender
     */
    public AdministrativeGender convertToFhir(CD code) {
        try {
            return AdministrativeGender.fromCode(code.getCode());
        } catch (FHIRException exception) {
            String newCode = this.convertGenderCode(code);
            return AdministrativeGender.fromCode(newCode);
        }
    }

    /**
     * Convert a CD object into a recognizeable FHIR gender code.
     * 
     * @param CD code : the CD object containing the gender
     * @return String
     */
    public String convertGenderCode(CD code) {
        switch (code.getCode().toLowerCase()) {
            case "m" :
                return "male";

            case "f" :
                return "female";
        }

        return "";
    }
}