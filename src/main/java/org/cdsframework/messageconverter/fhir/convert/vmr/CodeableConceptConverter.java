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

    /**
     * Convert a FHIR compliant CodeableConcept object into an Open CDS compliant
     * CD object.
     *
     * @param CodeableConcept concept : FHIR compliant CodeableConcept object
     * @return CD
     */
    public CD convertToCds(CodeableConcept concept) {
        CD cd = new CD();

        // it's possible for a concept to have multiple codings
        // but CD only saves one so for simplicity, we use the first
        Coding coding = concept.getCodingFirstRep();

        cd.setCode(coding.getCode());
        cd.setDisplayName(coding.getDisplay());

        return cd;
    }
}