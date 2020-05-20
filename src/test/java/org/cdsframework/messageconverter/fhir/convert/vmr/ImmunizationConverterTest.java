package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.r4.model.Immunization;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CDSInput;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;

import ca.uhn.fhir.parser.DataFormatException;

/**
 * @author Brian Lamb
 */
public class ImmunizationConverterTest {
    protected IceCdsInputWrapper wrapper;
    protected JsonObject immunization;
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected String before;

    @Before
    public void setUp() throws FileNotFoundException {
        this.wrapper = new IceCdsInputWrapper();

        JsonParser parser = new JsonParser();
        Object obj;

        try {
            obj = parser.parse(new FileReader("src/test/resources/immunization.json"));
        } catch (FileNotFoundException exception) {
            obj = "";
        }

        this.immunization = (JsonObject) obj;
        this.immunization = this.immunization.getAsJsonObject("resource");

        this.before = CdsObjectAssist.cdsObjectToString(this.wrapper.getCdsInput(), CDSInput.class);
    }

    @Test
    public void convertToCdsReturnsIceCdsInputWrapperTest() {
        IceCdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper, this.immunization);
        assertNotNull(wrapper);
    }

    @Test
    public void wrapperHasNoImmunizationsByDefaultTest() {
        List<SubstanceAdministrationEvent> immunizations = this.wrapper.getCdsInputWrapper().getSubstanceAdministrationEvents();
        assertEquals(0, immunizations.size());
    }


    @Test
    public void convertToCdsDoesNothingIfNoOccurrenceTest() {
        this.immunization.remove("occurrenceDateTime");

        IceCdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper, this.immunization);
        List<SubstanceAdministrationEvent> immunizations = wrapper.getCdsInputWrapper().getSubstanceAdministrationEvents();

        assertEquals(0, immunizations.size());
    }

    @Test
    public void convertToCdsDoesNothingIfNoOccurrenceDateTimeTest() {
        this.immunization.remove("occurrenceDateTime");
        this.immunization.addProperty("occurrenceString", "2020-05-20");

        IceCdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper, this.immunization);
        List<SubstanceAdministrationEvent> immunizations = wrapper.getCdsInputWrapper().getSubstanceAdministrationEvents();

        assertEquals(0, immunizations.size());
    }

    @Test
    public void convertToCdsDoesNothingIfNoIdTest() {
        this.immunization.remove("id");

        IceCdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper, this.immunization);
        List<SubstanceAdministrationEvent> immunizations = wrapper.getCdsInputWrapper().getSubstanceAdministrationEvents();

        assertEquals(0, immunizations.size());
    }

    @Test
    public void convertToCdsDoesNothingIfNoVaccineCodeTest() {
        this.immunization.remove("vaccineCode");

        IceCdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper, this.immunization);
        List<SubstanceAdministrationEvent> immunizations = wrapper.getCdsInputWrapper().getSubstanceAdministrationEvents();

        assertEquals(0, immunizations.size());
    }

    @Test
    public void convertToCdsDoesNothingIfCodeSystemHasNoOidTest() {
        this.immunization.getAsJsonObject("vaccineCode").getAsJsonArray("coding").get(0).getAsJsonObject()
                .remove("system");

        this.immunization.getAsJsonObject("vaccineCode").getAsJsonArray("coding").get(0).getAsJsonObject()
                .addProperty("system", "does-not-exit");

        IceCdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper, this.immunization);
        List<SubstanceAdministrationEvent> immunizations = wrapper.getCdsInputWrapper().getSubstanceAdministrationEvents();

        assertEquals(0, immunizations.size());
    }

    @Test
    public void convertToCdsAddsImmunizationTest() {
        IceCdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper, this.immunization);
        List<SubstanceAdministrationEvent> immunizations = wrapper.getCdsInputWrapper().getSubstanceAdministrationEvents();

        assertEquals(1, immunizations.size());
    }

    @Test
    public void convertToCdsSetsCorrectDataTest() {
        IceCdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper, this.immunization);
        List<SubstanceAdministrationEvent> immunizations = wrapper.getCdsInputWrapper().getSubstanceAdministrationEvents();

        String code = this.immunization.getAsJsonObject("vaccineCode")
            .getAsJsonArray("coding")
            .get(0)
            .getAsJsonObject()
            .getAsJsonPrimitive("code")
            .getAsString();
        String administered = this.immunization.get("occurrenceDateTime").getAsString().replace("-", "");
        String codeOid = Config.getCodeSystemOid("VACCINE");
        String administrationId = Config.getCodeSystemOid("ADMINISTRATION_ID");
        String id = "Immunization/" + this.immunization.get("id").getAsString() + "/_history/1";
        
        for (SubstanceAdministrationEvent immunization : immunizations) {
            // check the things were set correctly
            assertEquals(code, immunization.getSubstance().getSubstanceCode().getCode());
            assertEquals(administered, immunization.getAdministrationTimeInterval().getHigh());
            assertEquals(administered, immunization.getAdministrationTimeInterval().getLow());
            assertEquals(codeOid, immunization.getSubstance().getSubstanceCode().getCodeSystem());
            assertEquals(administrationId, immunization.getId().getExtension());
            assertEquals(id, immunization.getId().getRoot());
        }
    }
    
    @Test
    public void convertToCdsWorksWithCdsInputWrapperTest() {
        CdsInputWrapper wrapper = this.immunizationConverter.convertToCds(this.wrapper.getCdsInputWrapper(), this.immunization);
        List<SubstanceAdministrationEvent> immunizations = wrapper.getSubstanceAdministrationEvents();

        assertEquals(1, immunizations.size());

    }
    
    @Test
    public void convertToFhirCreatesImmunizationObjectTest() {
        Immunization immunization = this.immunizationConverter.convertToFhir(this.immunization);
        assertTrue(immunization instanceof Immunization);        
    }
    
    @Test(expected = DataFormatException.class)
    public void convertToFhirFailsIfInvalidData() {
        JsonObject json = new JsonObject();
        this.immunizationConverter.convertToFhir(json);        
    }
}