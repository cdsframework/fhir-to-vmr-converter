package org.cdsframework.messageconverter.fhir.convert.vmr;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.json.JSONObject;

/**
 * @author Brian Lamb
 */
public interface CdsConverter {
    public CdsInputWrapper convertToCds(CdsInputWrapper input, JSONObject data);
    public IceCdsInputWrapper convertToCds(IceCdsInputWrapper wrapper, JSONObject data);
}