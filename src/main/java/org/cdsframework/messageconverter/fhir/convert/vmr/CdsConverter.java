package org.cdsframework.messageconverter.fhir.convert.vmr;

import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;

/**
 * @author Brian Lamb
 */
public interface CdsConverter {
    public CdsInputWrapper convertToCds(CdsInputWrapper input, JsonObject data);
    public IceCdsInputWrapper convertToCds(IceCdsInputWrapper wrapper, JsonObject data);
}