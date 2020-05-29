package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.hl7.fhir.r4.model.DomainResource;
import org.opencds.vmr.v1_0.schema.CDSOutput;

public interface CdsOutputToFhirConverter {
    public DomainResource convertToFhir(CDSOutput data);
}