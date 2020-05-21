package org.cdsframework.messageconverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 * @author Brian Lamb
 */
public class Fhir2VmrTest {
    protected Fhir2Vmr fhir2Vmr;
    protected String fileContents;
    protected CdsInputWrapper wrapper;
    protected String defaultOutput;

    @Before
    public void setUp() throws FileNotFoundException, IOException {
        InputStream inputStream = new FileInputStream("src/test/resources/forecast.json");
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(inputStreamReader);

        String fileContents = "";
        String line = "";

        while ((line = br.readLine()) != null) {
            fileContents += line;
        }

        br.close();

        this.wrapper = CdsInputWrapper.getCdsInputWrapper();
        this.fhir2Vmr = new Fhir2Vmr();
        this.fileContents = fileContents;
        this.defaultOutput = CdsObjectAssist.cdsObjectToString(this.wrapper.getCdsObject(), CDSInput.class);
    }

    @Test
    public void jsonDataCreatesJsonObjectTest() {        
        String data = "{json: true}";

        JsonObject fhirData = this.fhir2Vmr.createFhirElement(data);

        assertTrue(fhirData.has("json"));
        assertTrue(fhirData.isJsonObject());
    }

    @Test
    public void xmlDataCreatesJsonObjectTest() {
        String data = "<json>true</json>";

        JsonObject fhirData = this.fhir2Vmr.createFhirElement(data);

        assertTrue(fhirData.has("json"));
        assertTrue(fhirData.isJsonObject());
    }

    @Test
    public void createFhirElementWorksWithBytesTest() {
        String data = "{json: true}";

        JsonObject fhirData = this.fhir2Vmr.createFhirElement(data.getBytes());

        assertTrue(fhirData.has("json"));
        assertTrue(fhirData.isJsonObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getCdsInputFromFhirThrowsExceptionIfNoParameterArgumentTest() {
        String data = "{json: true}";
        this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
    }

    @Test
    public void getCdsInputFromFhirDoesntUpdateIfNoParameterDataTest() {
        String data = "{parameter: []}";

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputFromFhirDoesntUpdateIfParameterDoesntHaveNameTest() {
        String data = "{parameter: [{ type: \"name\" }, { type: \"patient\" }]}";

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputFromFhirDoesntUpdateIfParameterDoesntHaveResourceTest() {
        String data = "{parameter: [{ name: \"patient\"}, { name: \"immunization\"}]}";

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertEquals(this.defaultOutput, output);        
    }

    @Test
    public void getCdsInputFromFhirDoesntUpdateIfNameIsNotPrimitiveTypeTest() {
        String data = "{parameter: [{ name: { type: \"patient\" } }]}";

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputFromFhirDoesntUpdateUnrecognizedNamesTest() {
        String data = "{parameter: [{ name: \"car\", resource: { id: 4 } }]} ";

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputFromFhirUpdatesWithPatientDataTest() {
        JsonObject data = this.fhir2Vmr.createFhirElement(this.fileContents);
        JsonArray dataArray = data.getAsJsonArray("parameter");

        for (int i = 0; i < dataArray.size(); i++) {
            JsonObject element = dataArray.get(i).getAsJsonObject();

            if (element.has("name") && element.getAsJsonPrimitive("name").toString() == "immunization") {
                dataArray.remove(i);
            }
        }        

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputFromUpdatesWithImmunizationDataTest() {
        JsonObject data = this.fhir2Vmr.createFhirElement(this.fileContents);
        JsonArray dataArray = data.getAsJsonArray("parameter");

        for (int i = 0; i < dataArray.size(); i++) {
            JsonObject element = dataArray.get(i).getAsJsonObject();

            if (element.has("name") && element.getAsJsonPrimitive("name").toString() == "patient") {
                dataArray.remove(i);
            }
        }

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);        
    }

    @Test
    public void getCdsInputUpdatesWithForecastDataTest() {
        JsonObject data = this.fhir2Vmr.createFhirElement(this.fileContents);

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);         
    }

    @Test
    public void getCdsInputWorksWithStringDataTest() {
        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.fileContents);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);
    }
    
    @Test
    public void getCdsInputWorksWithByteDataTest() {
        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.fileContents.getBytes());
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputWorksWithCdsInputWrapperStringDataTest() {
        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, this.fileContents);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);
    }
    
    @Test
    public void getCdsInputWorksWithCdsInputWrapperByteDataTest() {
        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, this.fileContents.getBytes());
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);
    }
    
    @Test
    public void getCdsInputWorksWithIceCdsInputWrapperStringDataTest() {
        IceCdsInputWrapper wrapper = new IceCdsInputWrapper();

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(wrapper, this.fileContents);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);       
    }

    @Test
    public void getCdsInputWorksWithIceCdsInputWrapperByteDataTest() {
        IceCdsInputWrapper wrapper = new IceCdsInputWrapper();

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(wrapper, this.fileContents.getBytes());
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);    
    }

    @Test
    public void getCdsInputWorksWithIceCdsInputWrapperJsonObjectDataTest() {
        IceCdsInputWrapper wrapper = new IceCdsInputWrapper();
        JsonObject data = this.fhir2Vmr.createFhirElement(this.fileContents);

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);    
    }
}