package org.cdsframework.messageconverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CDSInput;
import org.opencds.vmr.v1_0.schema.CDSOutput;

/**
 * @author Brian Lamb
 */
public class Fhir2VmrTest {
    protected Fhir2Vmr fhir2Vmr;
    protected String fileContents;
    protected CdsInputWrapper wrapper;
    protected String defaultOutput;
    protected Patient patient;
    protected List<Immunization> observationResults = new ArrayList<Immunization>();
    protected List<Immunization> substanceAdministrationEvents = new ArrayList<Immunization>();
    protected IdentifierFactory identifierFactory = new IdentifierFactory();

    @Before
    public void setUp() throws FileNotFoundException, IOException, ParseException {
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmdd");
        Date birthDate = dateFormat.parse("20200612");

        this.patient = new Patient();
        this.patient.setGender(AdministrativeGender.MALE);
        this.patient.setBirthDate(birthDate);

        Immunization immunization = new Immunization();
        immunization.setId("nil");

        this.observationResults.add(immunization);
        this.substanceAdministrationEvents.add(immunization);
    }

    @Test
    public void jsonDataCreatesJSONObjectTest() {
        String data = "{json: true}";

        JSONObject fhirData = this.fhir2Vmr.createFhirElement(data);

        assertTrue(fhirData.has("json"));
        assertFalse(fhirData.isEmpty());
    }

    @Test
    public void xmlDataCreatesJSONObjectTest() {
        String data = "<json>true</json>";

        JSONObject fhirData = this.fhir2Vmr.createFhirElement(data);

        assertTrue(fhirData.has("json"));
        assertFalse(fhirData.isEmpty());
    }

    @Test
    public void createFhirElementWorksWithBytesTest() {
        String data = "{json: true}";

        JSONObject fhirData = this.fhir2Vmr.createFhirElement(data.getBytes());

        assertTrue(fhirData.has("json"));
        assertFalse(fhirData.isEmpty());
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
        JSONObject data = this.fhir2Vmr.createFhirElement(this.fileContents);
        JSONArray dataArray = data.getJSONArray("parameter");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject element = dataArray.getJSONObject(i);

            if (element.has("name") && element.getString("name").equals("immunization")) {
                data.getJSONArray("parameter").remove(i);
            }
        }

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputFromUpdatesWithImmunizationDataTest() {
        JSONObject data = this.fhir2Vmr.createFhirElement(this.fileContents);
        JSONArray dataArray = data.getJSONArray("parameter");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject element = dataArray.getJSONObject(i);

            if (element.has("name") && element.getString("name") == "patient") {
                dataArray.remove(i);
            }
        }

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputUpdatesWithForecastDataTest() {
        JSONObject data = this.fhir2Vmr.createFhirElement(this.fileContents);

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
    public void getCdsInputWorksWithIceCdsInputWrapperJSONObjectDataTest() {
        IceCdsInputWrapper wrapper = new IceCdsInputWrapper();
        JSONObject data = this.fhir2Vmr.createFhirElement(this.fileContents);

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(wrapper, data);
        String output = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);

        assertNotEquals(this.defaultOutput, output);
    }

    @Test
    public void getCdsInputFromFhirPopulatesDemographicData() {
        Patient patient = new Patient();

        CDSInput noPatient = this.fhir2Vmr.getCdsInputFromFhir(patient);
        CDSInput withPatient = this.fhir2Vmr.getCdsInputFromFhir(this.patient);

        assertNotEquals(
            CdsObjectAssist.cdsObjectToString(noPatient, CDSInput.class),
            CdsObjectAssist.cdsObjectToString(withPatient, CDSInput.class)
        );
    }

    @Test
    public void getCdsInputFromFhirPopulatesDemographicDataAndObservationResults() {
        List<Immunization> observationResults = new ArrayList<Immunization>();

        CDSInput noObservationResults = this.fhir2Vmr.getCdsInputFromFhir(this.patient, observationResults);
        CDSInput withObservationResults = this.fhir2Vmr.getCdsInputFromFhir(this.patient, this.observationResults);

        assertNotEquals(
            CdsObjectAssist.cdsObjectToString(noObservationResults, CDSInput.class),
            CdsObjectAssist.cdsObjectToString(withObservationResults, CDSInput.class)
        );
    }

    @Test
    public void getCdsInputFromFhirPopulatesDemographicDataObservationResultsAndSubstanceAdministrationEvents() {
        List<Immunization> substanceAdministrationEvents = new ArrayList<Immunization>();

        CDSInput noSubstanceAdministrationEvents = this.fhir2Vmr.getCdsInputFromFhir(this.patient, substanceAdministrationEvents, this.observationResults);
        CDSInput withSubstanceAdministrationEvents = this.fhir2Vmr.getCdsInputFromFhir(this.patient, this.substanceAdministrationEvents, this.observationResults);

        assertNotEquals(
            CdsObjectAssist.cdsObjectToString(noSubstanceAdministrationEvents, CDSInput.class),
            CdsObjectAssist.cdsObjectToString(withSubstanceAdministrationEvents, CDSInput.class)
        );
    }

    @Test
    public void getCdsInputFromFhirDoesNotAddSubstanceAdministrationEventsIfNoImmunizations() {
        List<Immunization> substanceAdministrationEvents = new ArrayList<Immunization>();

        CDSInput input = this.fhir2Vmr.getCdsInputFromFhir(this.patient, substanceAdministrationEvents, this.observationResults);

        assertNull(input.getVmrInput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents());
    }

    @Test
    public void getCdsOutputFromFhirWithPatientReturnsPopulatedCdsOutputObject() {
        Patient patient = new Patient();

        CDSOutput noPatient = this.fhir2Vmr.getCdsOutputFromFhir(patient);
        CDSOutput withPatient = this.fhir2Vmr.getCdsOutputFromFhir(this.patient);

        assertNotEquals(
            CdsObjectAssist.cdsObjectToString(noPatient, CDSOutput.class),
            CdsObjectAssist.cdsObjectToString(withPatient, CDSOutput.class)
        );
    }

    @Test
    public void getCdsOutputFromFhirWithPatientObservationsReturnsPopulatedCdsOutputObject() {
        List<Immunization> empty = new ArrayList<Immunization>();
        List<Immunization> populated = new ArrayList<Immunization>();

        Immunization immunization = new Immunization();
        populated.add(immunization);

        CDSOutput noObservations = this.fhir2Vmr.getCdsOutputFromFhir(this.patient, empty);
        CDSOutput observations = this.fhir2Vmr.getCdsOutputFromFhir(this.patient, populated);

        assertNotEquals(
            CdsObjectAssist.cdsObjectToString(noObservations, CDSOutput.class),
            CdsObjectAssist.cdsObjectToString(observations, CDSOutput.class)
        );
    }

    @Test
    public void getCdsoutputFromFhirAddsObservationResultForEachObservation() {
        List<Immunization> immunizations = new ArrayList<Immunization>();
        Immunization immunization = new Immunization();

        immunizations.add(immunization);
        immunizations.add(immunization);
        immunizations.add(immunization);

        CDSOutput output = this.fhir2Vmr.getCdsOutputFromFhir(this.patient, immunizations);

        assertEquals(
            output.getVmrOutput().getPatient().getClinicalStatements().getObservationResults().getObservationResult().size(),
            3
        );
    }

    @Test
    public void getCdsOutputFromFhirWithPatientObservationsAndImmunizationsReturnsPopulatedCdsOutputObject() {
        List<Immunization> immunizations = new ArrayList<Immunization>();
        List<Immunization> observations = new ArrayList<Immunization>();

        Immunization immunization = new Immunization();

        observations.add(immunization);
        observations.add(immunization);
        observations.add(immunization);

        CDSOutput noObservations = this.fhir2Vmr.getCdsOutputFromFhir(this.patient, immunizations, immunizations);
        CDSOutput observationOutput = this.fhir2Vmr.getCdsOutputFromFhir(this.patient, immunizations, observations);

        assertNotEquals(
            CdsObjectAssist.cdsObjectToString(noObservations, CDSOutput.class),
            CdsObjectAssist.cdsObjectToString(observationOutput, CDSOutput.class)
        );
    }

    @Test
    public void getCdsOutputFromFhirDoesNotAddSubstanceAdministrationEventsIfNoImmunizations() {
        List<Immunization> immunizations = new ArrayList<Immunization>();

        CDSOutput output = this.fhir2Vmr.getCdsOutputFromFhir(this.patient, immunizations, immunizations);

        assertNull(
            output.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents()
        );
    }

    @Test
    public void getCdsOutputFromFhirWithPatientObservationsImmunizationsAndEvaluationsReturnsPopulatedCdsOutputObject() {
        List<Immunization> immunizations = new ArrayList<Immunization>();
        List<Immunization> observations = new ArrayList<Immunization>();
        List<ImmunizationEvaluation> empty = new ArrayList<ImmunizationEvaluation>();
        List<ImmunizationEvaluation> populated = new ArrayList<ImmunizationEvaluation>();

        Identifier identifier = this.identifierFactory.create("parentId", "id");

        Immunization immunization = new Immunization();
        immunization.setId("id");

        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        evaluation.addIdentifier(identifier);
        evaluation.setId("eval-id");

        CodeableConcept targetDisease = new CodeableConcept();
        Coding coding = new Coding();
        coding.setCode("code");
        coding.setDisplay("display");
        coding.setSystem("system");

        evaluation.setTargetDisease(targetDisease);

        targetDisease.addCoding(coding);

        observations.add(immunization);
        immunizations.add(immunization);

        populated.add(evaluation);

        CDSOutput noEvaluations = this.fhir2Vmr.getCdsOutputFromFhir(
            this.patient,
            observations,
            immunizations,
            empty
        );

        CDSOutput withEvaluations = this.fhir2Vmr.getCdsOutputFromFhir(
            this.patient,
            observations,
            immunizations,
            populated
        );

        assertNotEquals(
            CdsObjectAssist.cdsObjectToString(noEvaluations, CDSOutput.class),
            CdsObjectAssist.cdsObjectToString(withEvaluations, CDSOutput.class)
        );
    }

    @Test
    public void getCdsOutputFromFhirAddsRelatedClinicalStatementForEachEvaluation() {
        List<Immunization> immunizations = new ArrayList<Immunization>();
        List<Immunization> observations = new ArrayList<Immunization>();
        List<ImmunizationEvaluation> evaluations = new ArrayList<ImmunizationEvaluation>();

        Identifier identifier = this.identifierFactory.create("parentId", "id");

        Immunization immunization = new Immunization();
        immunization.setId("id");

        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        evaluation.addIdentifier(identifier);
        evaluation.setId("eval-id");

        CodeableConcept targetDisease = new CodeableConcept();
        Coding coding = new Coding();
        coding.setCode("code");
        coding.setDisplay("display");
        coding.setSystem("system");

        evaluation.setTargetDisease(targetDisease);

        targetDisease.addCoding(coding);

        observations.add(immunization);
        immunizations.add(immunization);

        evaluations.add(evaluation);
        evaluations.add(evaluation);
        evaluations.add(evaluation);

        CDSOutput output = this.fhir2Vmr.getCdsOutputFromFhir(
            this.patient,
            observations,
            immunizations,
            evaluations
        );

        assertEquals(
            output.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().get(0).getRelatedClinicalStatement().size(),
            3
        );
    }

    @Test
    public void getCdsOutputFromFhirWithPatientObservationsImmunizationsEvaluationsAndRecommendationsReturnsPopulatedCdsOutputObject() {
        List<Immunization> immunizations = new ArrayList<Immunization>();
        List<Immunization> observations = new ArrayList<Immunization>();
        List<ImmunizationEvaluation> evaluations = new ArrayList<ImmunizationEvaluation>();
        List<ImmunizationRecommendation> empty = new ArrayList<ImmunizationRecommendation>();
        List<ImmunizationRecommendation> populated = new ArrayList<ImmunizationRecommendation>();

        Identifier identifier = this.identifierFactory.create("parentId", "id");

        Immunization immunization = new Immunization();
        immunization.setId("id");

        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        evaluation.addIdentifier(identifier);
        evaluation.setId("eval-id");

        CodeableConcept targetDisease = new CodeableConcept();
        Coding coding = new Coding();
        coding.setCode("code");
        coding.setDisplay("display");
        coding.setSystem("system");

        evaluation.setTargetDisease(targetDisease);

        targetDisease.addCoding(coding);

        observations.add(immunization);
        immunizations.add(immunization);

        evaluations.add(evaluation);

        ImmunizationRecommendation recommendation = new ImmunizationRecommendation();
        populated.add(recommendation);

        CDSOutput noRecommendations = this.fhir2Vmr.getCdsOutputFromFhir(
            this.patient,
            observations,
            immunizations,
            evaluations,
            empty
        );

        CDSOutput withRecommendations = this.fhir2Vmr.getCdsOutputFromFhir(
            this.patient,
            observations,
            immunizations,
            evaluations,
            populated
        );

        assertNotEquals(
            CdsObjectAssist.cdsObjectToString(noRecommendations, CDSOutput.class),
            CdsObjectAssist.cdsObjectToString(withRecommendations, CDSOutput.class)
        );
    }

    @Test
    public void getCdsOutputFromFhirAddsSubstanceAdministrationProposalForEachRecommendation() {
        List<Immunization> immunizations = new ArrayList<Immunization>();
        List<Immunization> observations = new ArrayList<Immunization>();
        List<ImmunizationEvaluation> evaluations = new ArrayList<ImmunizationEvaluation>();
        List<ImmunizationRecommendation> recommendations = new ArrayList<ImmunizationRecommendation>();

        Identifier identifier = this.identifierFactory.create("parentId", "id");

        Immunization immunization = new Immunization();
        immunization.setId("id");

        ImmunizationEvaluation evaluation = new ImmunizationEvaluation();
        evaluation.addIdentifier(identifier);
        evaluation.setId("eval-id");

        CodeableConcept targetDisease = new CodeableConcept();
        Coding coding = new Coding();
        coding.setCode("code");
        coding.setDisplay("display");
        coding.setSystem("system");

        evaluation.setTargetDisease(targetDisease);

        targetDisease.addCoding(coding);

        observations.add(immunization);
        immunizations.add(immunization);

        evaluations.add(evaluation);

        ImmunizationRecommendation recommendation = new ImmunizationRecommendation();
        recommendations.add(recommendation);
        recommendations.add(recommendation);
        recommendations.add(recommendation);

        CDSOutput output = this.fhir2Vmr.getCdsOutputFromFhir(
            this.patient,
            observations,
            immunizations,
            evaluations,
            recommendations
        );

        assertEquals(
            output.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationProposals().getSubstanceAdministrationProposal().size(),
            3
        );
    }
}