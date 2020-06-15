package org.cdsframework.messageconverter.fhir.convert.utils;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;

/**
 * @author Brian Lamb
 */
public class IdentifierFactory {
    /**
     * This method creates a FHIR compatible Identifier object using a description and a value. This allows
     * for different identifiers to be used for different purposes.
     * 
     * @param String description : a description of the identifier, it's purpose or use
     * @param String value : the value of the identifier
     * @return Identifier
     */
    public Identifier create(String description, String value) {
        CodeableConcept concept = new CodeableConcept();
        concept.setText(description);

        Identifier identifier = new Identifier();
        identifier.setType(concept);
        identifier.setValue(value);

        return identifier;
    }
}