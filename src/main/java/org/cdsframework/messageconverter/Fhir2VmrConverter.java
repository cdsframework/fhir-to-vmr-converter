package org.cdsframework.messageconverter;

import java.io.IOException;
import java.util.List;
import org.cdsframework.util.LogUtils;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 *
 * @author sdn
 */
public class Fhir2VmrConverter {

    private static final LogUtils logger = LogUtils.getLogger(Fhir2VmrConverter.class);

    private final Fhir2Vmr fhir2Vmr;

    public Fhir2VmrConverter(byte[] payload) {
        fhir2Vmr = new Fhir2Vmr(payload);
    }

    /**
     * Get the value of errorList
     *
     * @return the value of errorList
     */
    public List<String> getErrorList() {
        return fhir2Vmr.getErrorList();
    }

    public CDSInput convert() throws IOException {
        return fhir2Vmr.getCdsInputFromFhir();
    }

}
