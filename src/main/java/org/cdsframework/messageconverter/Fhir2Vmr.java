package org.cdsframework.messageconverter;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.vmr.PatientConverter;
import org.cdsframework.util.LogUtils;
import org.json.JSONObject;
import org.json.XML;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 * @author sdn
 */
public class Fhir2Vmr {
    private static final LogUtils logger = LogUtils.getLogger(Fhir2Vmr.class);
    private final List<String> errorList = new ArrayList<>();

    protected PatientConverter patientConverter = new PatientConverter();

    /**
     * Convert string into a JsonObject. This is used to validate fhir elements later and ensure
     * that the data has the appropriate properties. The string object can be either json or xml
     * formatted data.
     * 
     * @param String data the data to convert to a JsonObject
     * @return a json object containing the data in String data
     */
    protected JsonObject createFhirElement(String data) {
        if (logger.isDebugEnabled()) {
            final String METHODNAME = "Fhir2Vmr ";
            logger.debug(METHODNAME, "payload=", data);
        }
        
        // the data may be in xml, if so, convert to json
        if (data.startsWith("<")) {
            JSONObject xmlJSONObj = XML.toJSONObject(data);
            data = xmlJSONObj.toString(4);
        } 

        Gson gson = new Gson();

        JsonElement jsonElement = gson.fromJson(data, JsonElement.class);
        return jsonElement.getAsJsonObject();
    }

    /**
     * @see createFhirElement(String)
     */
    protected JsonObject createFhirElement(byte[] data) {
        String payload = new String(data);
        return this.createFhirElement(payload);
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
     * Convert fhir data as json object into cds formatted data. This uses several converter objects to 
     * convert each respective structure definition.
     * 
     * @param CdsInputWrapper wrapper : the wrapper object that will be returned containing the json data
     * @param JsonObject fhirElement : the fhir data converted to a json object
     * @return CDSInput element containing the data in the fhir json object
     */
    public CDSInput getCdsInputFromFhir(CdsInputWrapper wrapper, JsonObject fhirElement) {
        // currently, this is a parameters resource
        // @TODO when the spec is adopted, use the hapi fhir library
        // the interesting part is in the parameters array
        if (!fhirElement.has("parameter")) {
            throw new IllegalArgumentException();
        }

        JsonElement parameters = fhirElement.get("parameter");

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

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JsonObject)
     */
    public CDSInput getCdsInputFromFhir(String data) {
        CdsInputWrapper wrapper = CdsInputWrapper.getCdsInputWrapper();
        JsonObject fhirElement = this.createFhirElement(data);

        return this.getCdsInputFromFhir(wrapper, fhirElement);
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JsonObject)
     */
    public CDSInput getCdsInputFromFhir(byte[] data) {
        CdsInputWrapper wrapper = CdsInputWrapper.getCdsInputWrapper();
        JsonObject fhirElement = this.createFhirElement(data);

        return this.getCdsInputFromFhir(wrapper, fhirElement);       
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JsonObject)
     */
    public CDSInput getCdsInputFromFhir(JsonObject data) {
        CdsInputWrapper wrapper = CdsInputWrapper.getCdsInputWrapper();

        return this.getCdsInputFromFhir(wrapper, data);
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JsonObject)
     */
    public CDSInput getCdsInputFromFhir(CdsInputWrapper wrapper, String data) {
        JsonObject fhirElement = this.createFhirElement(data);
        return this.getCdsInputFromFhir(wrapper, fhirElement);
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JsonObject)
     */
    public CDSInput getCdsInputFromFhir(CdsInputWrapper wrapper, byte[] data) {
        JsonObject fhirElement = this.createFhirElement(data);
        return this.getCdsInputFromFhir(wrapper, fhirElement);        
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JsonObject)
     */
    public CDSInput getCdsInputFromFhir(IceCdsInputWrapper wrapper, String data) {
        JsonObject fhirElement = this.createFhirElement(data);

        this.getCdsInputFromFhir(wrapper.getCdsInputWrapper(), fhirElement);
        return wrapper.getCdsInput();
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JsonObject)
     */
    public CDSInput getCdsInputFromFhir(IceCdsInputWrapper wrapper, byte[] data) {
        JsonObject fhirElement = this.createFhirElement(data);

        this.getCdsInputFromFhir(wrapper.getCdsInputWrapper(), fhirElement);
        return wrapper.getCdsInput();
    }

    /**
     * @see getCdsInputFromFhir(CdsInputWrapper, JsonObject)
     */
    public CDSInput getCdsInputFromFhir(IceCdsInputWrapper wrapper, JsonObject data) {
        this.getCdsInputFromFhir(wrapper.getCdsInputWrapper(), data);
        return wrapper.getCdsInput();
    }
}
