package org.cdsframework.messageconverter.fhir.convert.vmr;

import java.util.List;

import org.hl7.fhir.r4.model.DomainResource;
import org.opencds.vmr.v1_0.schema.CDSOutput;

/**
 * @author Brian Lamb
 */
public interface CdsToFhirListConverter {
    public List<? extends DomainResource> convertToFhir(CDSOutput data);
}