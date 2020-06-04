package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public class CodeableConceptConverter implements CDConverter<CodeableConcept> {
    /**
     * Convert a CD object into a CodeableConcept.
     * 
     * @param CD code : the CD object containing the data for the codeable concept
     * @return CodeableConcept
     */
    public CodeableConcept convertToFhir(CD code) {
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();

        coding.setCode(code.getCode());
        coding.setDisplay(code.getDisplayName());
        codeableConcept.addCoding(coding);     

        return codeableConcept;
    }
}