package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public interface CDConverter <T> {
    public T convertToFhir(CD code);
}