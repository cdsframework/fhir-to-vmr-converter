package org.cdsframework.messageconverter;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 * @author sdn
 */
public class Fhir2VmrConverter {
    // utility class to convert fhir data to vmr data
    private final Fhir2Vmr fhir2Vmr;

    /**
     * @param payload : the fhir data to convert
     */
    public Fhir2VmrConverter(byte[] payload) throws JSONException {
        this.fhir2Vmr = new Fhir2Vmr(payload);
    }

    /**
     * Get the value of errorList
     *
     * @return the value of errorList
     */
    public List<String> getErrorList() {
        return this.fhir2Vmr.getErrorList();
    }

    /**
     * Convert FHIR data into valid cds data
     * 
     * @return CDSInput object containing the data from the fhir request
     */
    public CDSInput convert() throws IOException {
        return this.fhir2Vmr.getCdsInputFromFhir();
    }
}