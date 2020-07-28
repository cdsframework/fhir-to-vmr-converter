package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.AdministrableSubstance;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.SubstanceAdministrationEvents;
import org.opencds.vmr.v1_0.schema.II;
import org.opencds.vmr.v1_0.schema.IVLTS;
import org.opencds.vmr.v1_0.schema.ObservationResult;
import org.opencds.vmr.v1_0.schema.ObservationResult.ObservationValue;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;

import ca.uhn.fhir.parser.DataFormatException;

/**
 * @author Brian Lamb
 */
public class ImmunizationConverterTest {
    protected IceCdsInputWrapper wrapper;
    protected JSONObject immunization;
    protected ImmunizationConverter immunizationConverter = new ImmunizationConverter();
    protected IdentifierFactory identifierFactory = new IdentifierFactory();
    protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmdd");
    protected Patient patient = new Patient();

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
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, event);
        assertTrue(immunization.getVaccineCode().isEmpty());
    }

    @Test
    public void convertToFhirCreatesImmunizationFromSubstanceAdministrationEvent() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        AdministrableSubstance substance = new AdministrableSubstance();

        CD code = new CD();
        code.setCode("jut");
        code.setDisplayName("Junit Test");

        II id = new II();
        id.setRoot("root");

        substance.setId(id);
        substance.setSubstanceCode(code);
        event.setSubstance(substance);

        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, event);
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
    // TODO: FIX
//
//    @Test
//    public void convertToCdsObservationDoesntAddTemplateIdIfNoIdentifiers() {
//        Immunization immunization = new Immunization();
//
//        ObservationResult observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//        assertTrue(observationResult.getTemplateId().isEmpty());
//    }
//
//    @Test
//    public void convertToCdsObservationsDoesntAddTemplateIdIfNoTemplateIdIdentifier() {
//        Immunization immunization = new Immunization();
//        Identifier notTemplateId = this.identifierFactory.create("notTemplateId", "random");
//        immunization.addIdentifier(notTemplateId);
//
//        ObservationResult observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//        assertTrue(observationResult.getTemplateId().isEmpty());
//    }
//
//    @Test
//    public void convertToCdsObservationSetsObservationFocus() {
//        CodeableConcept vaccineCode = new CodeableConcept();
//        Coding coding = new Coding();
//
//        coding.setCode("code");
//        coding.setSystem("system");
//
//        vaccineCode.addCoding(coding);
//
//        Immunization immunization = new Immunization();
//        ObservationResult observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//        assertNull(observationResult.getObservationFocus().getCode());
//
//        immunization.setVaccineCode(vaccineCode);
//
//        observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//        assertNotNull(observationResult.getObservationFocus().getCode());
//    }
//
//    @Test
//    public void convertToCdsObservationSilentlyFailsIfNoDateFound() {
//        Immunization immunization = new Immunization();
//        ObservationResult observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//        assertNull(observationResult.getObservationEventTime());
//    }
//
//    @Test
//    public void convertToCdsObservationSetsHighAndLowToBeSame() throws ParseException {
//        Date observationEventTime = this.dateFormat.parse("20200613");
//        DateTimeType occurrence = new DateTimeType(observationEventTime);
//
//        Immunization immunization = new Immunization();
//        immunization.setOccurrence(occurrence);
//
//        ObservationResult observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//
//        assertEquals(
//            observationResult.getObservationEventTime().getHigh(),
//            observationResult.getObservationEventTime().getLow()
//        );
//    }
//
//    @Test
//    public void convertToCdsObservationUsesDateTimeToSetEventTime() throws ParseException {
//        Date observationEventTime = this.dateFormat.parse("20200613");
//        DateTimeType occurrence = new DateTimeType(observationEventTime);
//
//        Immunization immunization = new Immunization();
//        ObservationResult observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//
//        assertNull(observationResult.getObservationEventTime());
//
//        immunization.setOccurrence(occurrence);
//
//        observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//
//        assertNotNull(observationResult.getObservationEventTime());
//    }
//
//    @Test
//    public void convertToCdsObservationAddsNoInterpretationsIfNoReasonCodes() {
//        Immunization immunization = new Immunization();
//        ObservationResult observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//
//        assertTrue(observationResult.getInterpretation().isEmpty());
//    }
//
//    @Test
//    public void convertToCdsObservationSetsObservationValue() {
//        Immunization immunization = new Immunization();
//        ObservationResult observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//
//        assertNull(observationResult.getObservationValue().getConcept().getCode());
//
//        CodeableConcept statusReason = new CodeableConcept();
//        Coding coding = new Coding();
//        coding.setSystem("system");
//        coding.setCode("code");
//        statusReason.addCoding(coding);
//
//        immunization.setStatusReason(statusReason);
//
//        observationResult = this.immunizationConverter.convertToCdsObservation(immunization);
//
//        assertNotNull(observationResult.getObservationValue().getConcept().getCode());
//    }

    @Test
    public void convertToCdsSetsAdministrationTimeInterval() throws ParseException {
        Immunization immunization = new Immunization();
        SubstanceAdministrationEvent event = this.immunizationConverter.convertToCds(immunization);

        assertNull(event.getAdministrationTimeInterval());

        DateTimeType dateTime = new DateTimeType();
        Date administeredDate = this.dateFormat.parse("20200613");
        dateTime.setValue(administeredDate);

        immunization.setOccurrence(dateTime);

        event = this.immunizationConverter.convertToCds(immunization);

        assertNotNull(event.getAdministrationTimeInterval());
    }

    @Test
    public void convertToCdsSetsTimeIntervalHighAndLowToBeTheSame() throws ParseException {
        Immunization immunization = new Immunization();

        DateTimeType dateTime = new DateTimeType();
        Date administeredDate = this.dateFormat.parse("20200613");
        dateTime.setValue(administeredDate);

        immunization.setOccurrence(dateTime);

        SubstanceAdministrationEvent event = this.immunizationConverter.convertToCds(immunization);

        assertEquals(
            event.getAdministrationTimeInterval().getHigh(),
            event.getAdministrationTimeInterval().getLow()
        );
    }

    @Test
    public void convertToCdsSilentlyFailsIfNoDate() {
        Immunization immunization = new Immunization();
        SubstanceAdministrationEvent event = this.immunizationConverter.convertToCds(immunization);

        assertNull(event.getAdministrationTimeInterval());
    }

    @Test
    public void convertToFhirSetsNoIdentifiersIfNoTemplateId() {
        ObservationResult observationResult = new ObservationResult();
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertTrue(immunization.getIdentifier().isEmpty());
    }

    @Test
    public void convertToFhirDoesntSetStatusReasonIfNoObservationValue() {
        ObservationResult observationResult = new ObservationResult();
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertTrue(immunization.getStatusReason().isEmpty());
    }

    @Test
    public void convertToFhirDoesntSetDateIfNoEventTime() {
        ObservationResult observationResult = new ObservationResult();
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertNull(immunization.getOccurrence());
    }

    @Test
    public void convertToFhirDoesntSetVaccineCodeIfNoObservationFocus() {
        ObservationResult observationResult = new ObservationResult();
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertTrue(immunization.getVaccineCode().isEmpty());
    }

    @Test
    public void convertToFhirSetsVaccineCode() {
        CD observationFocus = new CD();
        observationFocus.setCode("focus");

        ObservationResult observationResult = new ObservationResult();
        observationResult.setObservationFocus(observationFocus);

        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertFalse(immunization.getVaccineCode().isEmpty());
    }

    @Test
    public void convertToFhirSetsOccurrence() {
        IVLTS eventTime = new IVLTS();
        eventTime.setHigh("20200613");
        eventTime.setLow("20200613");

        ObservationResult observationResult = new ObservationResult();
        observationResult.setObservationEventTime(eventTime);

        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertNotNull(immunization.getOccurrenceDateTimeType());
    }

    @Test
    public void convertToFhirSilentlyFailsIfNoObservationEventTime() {
        ObservationResult observationResult = new ObservationResult();

        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertNull(immunization.getOccurrence());
    }

    @Test
    public void convertToFhirAddsNoReasonsIfNoInterpretations() {
        ObservationResult observationResult = new ObservationResult();
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertTrue(immunization.getReasonCode().isEmpty());
    }

    @Test
    public void convertToFhirSetsStatusReason() {
        ObservationResult observationResult = new ObservationResult();
        ObservationValue observationValue = new ObservationValue();

        CD code = new CD();

        observationValue.setConcept(code);

        observationResult.setObservationValue(observationValue);

        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, observationResult);

        assertTrue(immunization.getStatusReason().isEmpty());
    }

    @Test
    public void convertToFhirAddsNoIdentifiersIfNoTemplateIdsOrExtension() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();

        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, event);
        assertTrue(immunization.getIdentifier().isEmpty());
    }

    @Test
    public void convertToFhirSetsRecordedTime() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();

        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, event);

        assertNull(immunization.getOccurrence());

        IVLTS timeInterval = new IVLTS();
        timeInterval.setHigh("20200615");
        timeInterval.setLow("20200615");

        event.setAdministrationTimeInterval(timeInterval);

        immunization = this.immunizationConverter.convertToFhir(this.patient, event);

        assertNotNull(immunization.getOccurrence());
    }

    @Test
    public void convertToFhirDoesntAddIdentifierForNoExtension() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, event);

        assertTrue(immunization.getIdentifier().isEmpty());
    }

    @Test
    public void convertToFhirHasNoVaccineCodeIfNoSubstance() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, event);

        assertTrue(immunization.getVaccineCode().isEmpty());
    }

    @Test
    public void convertToFhirBuildsVaccineCode() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        AdministrableSubstance substance = new AdministrableSubstance();

        CD code = new CD();
        code.setCode("code");
        II id = new II();

        substance.setSubstanceCode(code);
        substance.setId(id);

        event.setSubstance(substance);

        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, event);

        assertFalse(immunization.getVaccineCode().isEmpty());
    }

    @Test
    public void convertToFhirDoesNotAddReasonCodeIfNoSubstanceAdministrationGeneralPurpose() {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        Immunization immunization = this.immunizationConverter.convertToFhir(this.patient, event);

        assertTrue(immunization.getReasonCode().isEmpty());
    }

    @Test
    public void convertToCdsAddsNoEventsIfNoImmunizations() {
        List<Immunization> immunizations = new ArrayList<Immunization>();
        SubstanceAdministrationEvents events = this.immunizationConverter.convertToCds(immunizations);

        assertEquals(0, events.getSubstanceAdministrationEvent().size());
    }

    @Test
    public void convertToCdsAddsEventForEachImmunization() {
        List<Immunization> immunizations = new ArrayList<Immunization>();
        Immunization immunization = new Immunization();

        immunizations.add(immunization);
        immunizations.add(immunization);
        immunizations.add(immunization);

        SubstanceAdministrationEvents events = this.immunizationConverter.convertToCds(immunizations);

        assertEquals(3, events.getSubstanceAdministrationEvent().size());
    }

    @Test
    public void convertToCdsSetsValidIfStatusExists() {
        Immunization immunization = new Immunization();

        SubstanceAdministrationEvent event = this.immunizationConverter.convertToCds(immunization);

        assertNull(event.getIsValid());

        ImmunizationStatus immunizationStatus = ImmunizationStatus.COMPLETED;

        immunization.setStatus(immunizationStatus);

        event = this.immunizationConverter.convertToCds(immunization);

        assertNotNull(event.getIsValid());
    }
}