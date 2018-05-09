package org.cdsframework.messageconverter;

import java.io.IOException;
import org.cdsframework.util.LogUtils;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 *
 * @author sdn
 */
public class Fhir2VmrConverter {

    private static final LogUtils logger = LogUtils.getLogger(Fhir2VmrConverter.class);

    public CDSInput convert(String payload) throws IOException {
        final String METHODNAME = "convert ";
        logger.debug(METHODNAME, "payload=", payload);
        return Fhir2Vmr.getCdsInputFromFhir(payload);
    }

}
