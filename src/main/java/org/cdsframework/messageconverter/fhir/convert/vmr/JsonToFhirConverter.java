package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.hl7.fhir.r4.model.DomainResource;
import org.json.JSONObject;

/**
 * @author Brian Lamb
 */
public interface JsonToFhirConverter {
    public DomainResource convertToFhir(JSONObject data);
}