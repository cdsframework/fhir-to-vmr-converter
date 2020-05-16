package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;

import org.cdsframework.messageconverter.Fhir2Vmr;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Brian Lamb
 */
public class Fhir2VmrTest {
    protected Fhir2Vmr fhir2Vmr;

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

        this.fhir2Vmr = new Fhir2Vmr(fileContents.getBytes());        
    }

    @Test
    public void jsonDataCreatesJsonObjectTest() {        
        String data = "{json: true}";

        Fhir2Vmr fhir2Vmr = new Fhir2Vmr(data.getBytes());
        JsonObject fhirData = fhir2Vmr.getFhirElement();

        assertTrue(fhirData.has("json"));
        assertTrue(fhirData.isJsonObject());
    }

    @Test
    public void xmlDataCreatesJsonObjectTest() {
        String data = "<json>true</json>";

        Fhir2Vmr fhir2Vmr = new Fhir2Vmr(data.getBytes());
        JsonObject fhirData = fhir2Vmr.getFhirElement();

        assertTrue(fhirData.has("json"));
        assertTrue(fhirData.isJsonObject());
    }
}