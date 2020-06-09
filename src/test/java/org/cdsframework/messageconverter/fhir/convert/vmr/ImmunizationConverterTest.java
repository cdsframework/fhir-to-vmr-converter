package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Immunization;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.AdministrableSubstance;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;

import ca.uhn.fhir.parser.DataFormatException;

/**
 * @author Brian Lamb
 */
public class ImmunizationConverterTest {
    protected IceCdsInputWrapper wrapper;
    protected JSONObject immunization;
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();

    @Before
    public void setUp() throws IOException {
        this.wrapper = new IceCdsInputWrapper();

        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/immunization.json"));
        String fileContents = new String(data);

        this.immunization = new JSONObject(fileContents);
        this.immunization = this.immunization.getJSONObject("resource");
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
        this.immunization.put("occurrenceString", "2020-05-20");

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
        this.immunization.getJSONObject("vaccineCode").getJSONArray("coding").getJSONObject(0)
                .remove("system");

        this.immunization.getJSONObject("vaccineCode").getJSONArray("coding").getJSONObject(0)
                .put("system", "does-not-exit");

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

        String code = this.immunization.getJSONObject("vaccineCode")
            .getJSONArray("coding")
            .getJSONObject(0)
            .getString("code");
        String administered = this.immunization.getString("occurrenceDateTime").replace("-", "");
        String codeOid = Config.getCodeSystemOid("VACCINE");
        String administrationId = Config.getCodeSystemOid("ADMINISTRATION_ID");
        String id = "Immunization/" + this.immunization.getString("id") + "/_history/1";
        
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
        JSONObject json = new JSONObject();
        this.immunizationConverter.convertToFhir(json);        
    }

    @Test
    public void convertToFhirDoesntUpdateVaccineCodeIfNoSubstanceCode() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        Immunization immunization = this.immunizationConverter.convertToFhir(event);
        assertTrue(immunization.getVaccineCode().isEmpty());
    }

    @Test
    public void convertToFhirCreatesImmunizationFromSubstanceAdministrationEvent() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        AdministrableSubstance substance = new AdministrableSubstance();
        
        CD code = new CD();
        code.setCode("jut");
        code.setDisplayName("Junit Test");

        substance.setSubstanceCode(code);
        event.setSubstance(substance);

        Immunization immunization = this.immunizationConverter.convertToFhir(event);
        assertFalse(immunization.getVaccineCode().isEmpty());
    }

    @Test
    public void convertToCdsSetsCorrectData() {
        CodeableConcept code = new CodeableConcept();
        Coding coding = new Coding();

        coding.setCode("immcode");
        code.addCoding(coding);

        Immunization immunization = new Immunization();
        immunization.setVaccineCode(code);

        SubstanceAdministrationEvent event = this.immunizationConverter.convertToCds(immunization);

        assertNotNull(event);
        assertTrue(event instanceof SubstanceAdministrationEvent);
        assertEquals("immcode", event.getSubstance().getSubstanceCode().getCode());
    }
}