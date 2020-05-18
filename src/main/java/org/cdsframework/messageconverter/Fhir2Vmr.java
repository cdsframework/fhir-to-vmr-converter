package org.cdsframework.messageconverter;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.vmr.PatientConverter;
import org.cdsframework.util.LogUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 * @author sdn
 */
public class Fhir2Vmr {

    private static final LogUtils logger = LogUtils.getLogger(Fhir2Vmr.class);
    private final List<String> errorList = new ArrayList<>();
    private final JsonObject fhirElement;
    private final Gson gson = new Gson();

    protected PatientConverter patientConverter = new PatientConverter();

    /**
     * Get the value of fhirElement
     *
     * @return the value of fhirElement
     */
    public JsonObject getFhirElement() {
        return this.fhirElement;
    }

    /**
     * @param payload : the fhir data
     */
    public Fhir2Vmr(byte[] payload) throws JSONException {
        String data = new String(payload);

        if (logger.isDebugEnabled()) {
            final String METHODNAME = "Fhir2Vmr ";
            logger.debug(METHODNAME, "payload=", data);
        }
        
        // the data may be in xml, if so, convert to json
        if (data.startsWith("<")) {
            JSONObject xmlJSONObj = XML.toJSONObject(data);
            data = xmlJSONObj.toString(4);
        } 

        JsonElement jsonElement = this.gson.fromJson(data, JsonElement.class);
        this.fhirElement = jsonElement.getAsJsonObject();
    }

    /**
     * Get the value of errorList
     *
     * @return the value of errorList
     */
    public List<String> getErrorList() {
        return this.errorList;
    }

    /**
     * Convert fhir data into cds format
     * 
     * @param input 
     */
    public CDSInput getCdsInputFromFhir() {
        CdsInputWrapper wrapper = CdsInputWrapper.getCdsInputWrapper();

        // currently, this is a parameters resource
        // @TODO when the spec is adopted, use the hapi fhir library
        // the interesting part is in the parameters array
        
        if (!this.fhirElement.has("parameter")) {
            throw new IllegalArgumentException();
        }

        JsonElement parameters = this.fhirElement.get("parameter");

        for (JsonElement element : parameters.getAsJsonArray()) {
            JsonObject object = element.getAsJsonObject();

            if (object.has("name") && object.has("resource")) {
                JsonElement name = object.get("name");

                // this should be a primitive
                if (name.isJsonPrimitive()) {
                    switch (name.getAsString()) {
                        case "patient" :
                            // convert patient data
                            wrapper = this.patientConverter.convertToCds(wrapper, object.getAsJsonObject("resource"));
                            break;
                    }
                }
            }
        }

        return wrapper.getCdsObject();
    }
}
