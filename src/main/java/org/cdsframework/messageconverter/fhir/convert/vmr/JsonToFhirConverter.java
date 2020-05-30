package org.cdsframework.messageconverter.fhir.convert.vmr;

import com.google.gson.JsonObject;

import org.hl7.fhir.r4.model.DomainResource;

/**
 * @author Brian Lamb
 */
public interface JsonToFhirConverter {
    public DomainResource convertToFhir(JsonObject data);
}