package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.hl7.fhir.r4.model.DomainResource;
import org.json.JSONObject;

/**
 * @author Brian Lamb
 */
public interface FhirConverter <T, S extends DomainResource> {
    public DomainResource convertToFhir(T data);
    public DomainResource convertToFhir(JSONObject data);
    public T convertToCds(S resource);
}