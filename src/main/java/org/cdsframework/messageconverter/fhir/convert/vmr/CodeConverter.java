package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public interface CodeConverter <T> {
    public T convertToFhir(CD code);
    public CD convertToCds(T code);
}