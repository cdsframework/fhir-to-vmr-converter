package org.cdsframework.messageconverter.fhir.convert.vmr;

import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.hl7.fhir.r4.model.DomainResource;

public interface FhirConverter {
    public CdsInputWrapper convertToCds(CdsInputWrapper input, JsonObject data);
    public DomainResource convertToFhir(JsonObject data);
}