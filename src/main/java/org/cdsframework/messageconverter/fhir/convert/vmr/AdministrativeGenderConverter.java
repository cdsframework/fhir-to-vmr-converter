package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public class AdministrativeGenderConverter implements CodeConverter<AdministrativeGender> {
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
     * Convert a fhir compliant administrative gender element into an opencds
     * CD object.
     * 
     * @param AdministrativeGender gender : the FHIR compliant gender object
     * @return CD
     */
    public CD convertToCds(AdministrativeGender gender) {
        CD code = new CD();

        code.setCode(this.convertGenderCode(gender));

        return code;
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

    /**
     * This method converts a FHIR AdministrativeGender object into string that matches
     * a CD code.
     * 
     * @param AdministrativeGender gender : the FHIR object containing the code
     * @return String
     */
    public String convertGenderCode(AdministrativeGender gender) {
        try {
            switch (gender.toCode().toLowerCase()) {
                case "male" :
                    return "M";

                case "female" :
                    return "F";
            }
        } catch (NullPointerException exception) {
            return "";
        }

        return "";
    }
}