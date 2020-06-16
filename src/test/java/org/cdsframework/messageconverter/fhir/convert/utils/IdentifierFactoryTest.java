package org.cdsframework.messageconverter.fhir.convert.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Identifier;
import org.junit.Test;

/**
 * @author Brian Lamb
 */
public class IdentifierFactoryTest {
    protected IdentifierFactory identiferFactory = new IdentifierFactory();

    @Test
    public void createPopulatesIdentifier() {
        Identifier identifier = this.identiferFactory.create("description", "value");
        
        assertNotNull(identifier);
        assertTrue(identifier instanceof Identifier);
        assertEquals(identifier.getValue(), "value");
        assertEquals(identifier.getType().getText(), "description");
    }
}