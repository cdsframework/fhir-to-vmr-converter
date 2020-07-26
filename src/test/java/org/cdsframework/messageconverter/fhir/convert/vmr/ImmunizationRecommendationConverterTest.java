package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationDateCriterionComponent;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.AdministrableSubstance;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.II;
import org.opencds.vmr.v1_0.schema.IVLTS;
import org.opencds.vmr.v1_0.schema.ObservationResult;
import org.opencds.vmr.v1_0.schema.ObservationResult.ObservationValue;
import org.opencds.vmr.v1_0.schema.RelatedClinicalStatement;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationProposal;

/**
 * @author Brian Lamb
 */
public class ImmunizationRecommendationConverterTest {
    protected ImmunizationRecommendationConverter immunizationRecommendationConverter = new ImmunizationRecommendationConverter();
    protected CDSOutput output;
    protected CD code;
    protected Patient patient = new Patient();
    protected List<SubstanceAdministrationProposal> proposals = new ArrayList<SubstanceAdministrationProposal>();
    protected SubstanceAdministrationProposal proposal = new SubstanceAdministrationProposal();
    protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmdd");
    protected ImmunizationRecommendation recommendation = new ImmunizationRecommendation();
    protected IdentifierFactory identifierFactory = new IdentifierFactory();

    @Before
    public void setUp() throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/recommendation.xml"));
        this.output = CdsObjectAssist.cdsObjectFromByteArray(data, CDSOutput.class);

        this.code = new CD();
        this.code.setCode("jut");
        this.code.setDisplayName("Junit Test");

        this.patient.setId("patient");
    }

    @Test
    public void convertToFhirSetsPatient() {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        assertFalse(recommendation.getPatientTarget().isEmpty());
    }

    @Test
    public void convertToFhirDoesNotSetIdIfNoId() {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        assertNull(recommendation.getId());
    }

    @Test
    public void convertToFhirSetsIdFromId() {
        II id = new II();
        id.setRoot("proposalid");

        this.proposal.setId(id);

        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        assertEquals("proposalid", recommendation.getId());
    }

    @Test
    public void convertToFhirDoesNotSetDateIfNoTimeInterval() {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(0, component.getDateCriterion().size());
    }

    @Test
    public void convertToFhirDoesNotSetDateIfNotProperlyFormatted() {
        IVLTS timeInterval = new IVLTS();
        timeInterval.setHigh("209");

        this.proposal.setProposedAdministrationTimeInterval(timeInterval);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(0, component.getDateCriterion().size());
    }

    @Test
    public void convertToFhirAddsNoDateIfNoLowOrHigh() {
        IVLTS timeInterval = new IVLTS();

        this.proposal.setProposedAdministrationTimeInterval(timeInterval);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(0, component.getDateCriterion().size());
    }

    @Test
    public void convertToFhirAddsDateForLowIfExists() {
        IVLTS timeInterval = new IVLTS();
        timeInterval.setLow("20200702");

        this.proposal.setProposedAdministrationTimeInterval(timeInterval);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, component.getDateCriterion().size());

        ImmunizationRecommendationRecommendationDateCriterionComponent date = component.getDateCriterionFirstRep();

        assertEquals("20200702", this.dateFormat.format(date.getValue()));
    }

    @Test
    public void convertToFhirAddsDateForHighIfExists() {
        IVLTS timeInterval = new IVLTS();
        timeInterval.setHigh("20200702");

        this.proposal.setProposedAdministrationTimeInterval(timeInterval);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, component.getDateCriterion().size());

        ImmunizationRecommendationRecommendationDateCriterionComponent date = component.getDateCriterionFirstRep();

        assertEquals("20200702", this.dateFormat.format(date.getValue()));
    }

    @Test
    public void convertToFhirAddsDatesForLowAndHighIfBothExist() {
        IVLTS timeInterval = new IVLTS();
        timeInterval.setHigh("20200702");
        timeInterval.setLow("20200703");

        this.proposal.setProposedAdministrationTimeInterval(timeInterval);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(2, component.getDateCriterion().size());

        for (ImmunizationRecommendationRecommendationDateCriterionComponent date : component.getDateCriterion()) {
            if (date.getCode().getText().equals("low")) {
                assertEquals("20200703", this.dateFormat.format(date.getValue()));
            } else if (date.getCode().getText().equals("high")) {
                assertEquals("20200702", this.dateFormat.format(date.getValue()));
            } else {
                assertFalse(true);
            }
        }
    }

    @Test
    public void convertToFhirDoesntAddTemplateIdIfNoTemplateIds() {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        assertEquals(0, recommendation.getIdentifier().size());
    }

    @Test
    public void convertToFhirAddsTemplateIdForEachTemplateId() {
        II templateId = new II();
        templateId.setRoot("templateId");

        this.proposal.getTemplateId().add(templateId);
        this.proposal.getTemplateId().add(templateId);
        this.proposal.getTemplateId().add(templateId);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        assertEquals(3, recommendation.getIdentifier().size());
    }

    @Test
    public void convertToFhirDoesntAddContraindicatedVaccineCodeIfNoGeneralPurpose() {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(0, component.getContraindicatedVaccineCode().size());
    }

    @Test
    public void convertToFhirAddsContraindicatedVaccineCodeFromGeneralPurpose() {
        CD generalPurpose = new CD();
        generalPurpose.setCode("code");

        this.proposal.setSubstanceAdministrationGeneralPurpose(generalPurpose);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, component.getContraindicatedVaccineCode().size());

        CodeableConcept vaccineCode = component.getContraindicatedVaccineCodeFirstRep();

        assertFalse(vaccineCode.isEmpty());
    }

    @Test
    public void convertToFhirDoesNotSetVaccineCodeIfNoSubstance() {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(0, component.getVaccineCode().size());
    }

    @Test
    public void convertToFhirDoesNotSetVaccineCodeIfNoSubstanceCode() {
        AdministrableSubstance substance = new AdministrableSubstance();
        this.proposal.setSubstance(substance);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(0, component.getVaccineCode().size());
    }

    @Test
    public void convertToFhirDoesNotSetVaccineCodeIdIfNoSubstanceId() {
        CD substanceCode = new CD();
        substanceCode.setCode("code");

        AdministrableSubstance substance = new AdministrableSubstance();
        substance.setSubstanceCode(substanceCode);

        this.proposal.setSubstance(substance);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, component.getVaccineCode().size());

        CodeableConcept vaccineCode = component.getVaccineCodeFirstRep();

        assertNull(vaccineCode.getId());
    }

    @Test
    public void convertToFhirSetsVaccineCodeIdFromSubstanceId() {
        II id = new II();
        id.setRoot("substanceId");

        CD substanceCode = new CD();
        substanceCode.setCode("code");

        AdministrableSubstance substance = new AdministrableSubstance();
        substance.setSubstanceCode(substanceCode);
        substance.setId(id);

        this.proposal.setSubstance(substance);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, component.getVaccineCode().size());

        CodeableConcept vaccineCode = component.getVaccineCodeFirstRep();

        assertEquals("substanceId", vaccineCode.getId());
    }

    @Test
    public void convertToFhirAddsOneRecommendationIfNoRelatedClinicalStatements() {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        assertEquals(1, recommendation.getRecommendation().size());
    }

    @Test
    public void convertToFhirDoesNotAddRecommendationForClinicalStatementIfNoObservationResult() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();

        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        assertEquals(1, recommendation.getRecommendation().size());
    }

    @Test
    public void convertToFhirAddsRecommendationForEachRelatedClinicalStatements() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        clinicalStatement.setObservationResult(observationResult);

        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        assertEquals(3, recommendation.getRecommendation().size());
    }

    @Test
    public void convertToFhirDoesNotSetComponentIdIfNoObservationResultId() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, recommendation.getRecommendation().size());
        assertNull(component.getId());
    }

    @Test
    public void convertToFhirSetsComponentIdFromObservationResultId() {
        II id = new II();
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        id.setRoot("observationResultId");
        observationResult.setId(id);

        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, recommendation.getRecommendation().size());
        assertEquals("observationResultId", component.getId());
    }

    @Test
    public void convertToFhirDoesNotSetTargetDiseaseIfNoObservationFocus() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, recommendation.getRecommendation().size());
        assertTrue(component.getTargetDisease().isEmpty());
    }

    @Test
    public void convertToFhirSetsTargetDiseaseFromObservationFocus() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        CD observationFocus = new CD();
        observationFocus.setCode("observationFocus");

        observationResult.setObservationFocus(observationFocus);

        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(1, recommendation.getRecommendation().size());
        assertFalse(component.getTargetDisease().isEmpty());
    }

    @Test
    public void convertToFhirDoesNotAddTemplateIdentifiersIfNoTemplateId() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        for (Identifier identifier : recommendation.getIdentifier()) {
            if (identifier.getType().getText().equals("observationTemplateId")) {
                fail("Observation template id found");
            }
        }
    }

    @Test
    public void convertToFhirAddsTemplateIdentifiersForEachTemplateId() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        II id = new II();
        id.setRoot("observationId");

        II templateId = new II();
        templateId.setRoot("observationTemplateId");

        observationResult.setId(id);
        observationResult.getTemplateId().add(templateId);
        observationResult.getTemplateId().add(templateId);

        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        int numTemplateIds = 0;

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        for (Identifier identifier : recommendation.getIdentifier()) {
            if (identifier.getType().getText().equals("observationTemplateId")) {
                numTemplateIds++;
            }
        }

        assertEquals(6, numTemplateIds);
    }

    @Test
    public void convertToFhirDoesntSetForecastStatusIfNoObservationValue() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertTrue(component.getForecastStatus().isEmpty());
    }

    @Test
    public void convertToFhirDoesNotSetForecastStatusIfNoObservationValueCode() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();
        ObservationValue observationValue = new ObservationValue();

        observationResult.setObservationValue(observationValue);
        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertTrue(component.getForecastStatus().isEmpty());
    }

    @Test
    public void convertToFhirSetsForecastStatusFromObservationValue() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();
        ObservationValue observationValue = new ObservationValue();

        CD observationValueCode = new CD();
        observationValueCode.setCode("observationValue");
        observationValue.setConcept(observationValueCode);

        observationResult.setObservationValue(observationValue);
        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertFalse(component.getForecastStatus().isEmpty());
    }

    @Test
    public void convertToFhirSetsNoForecastReasonsIfNoInterpretations() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        clinicalStatement.setObservationResult(observationResult);
        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(0, component.getForecastReason().size());
    }

    @Test
    public void convertToFhirAddsForecastReasonForEachInterpretation() {
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();
        CD interpretation = new CD();

        interpretation.setCode("interpretation");

        observationResult.getInterpretation().add(interpretation);
        observationResult.getInterpretation().add(interpretation);
        observationResult.getInterpretation().add(interpretation);

        clinicalStatement.setObservationResult(observationResult);

        this.proposal.getRelatedClinicalStatement().add(clinicalStatement);
        this.proposals.add(this.proposal);

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(
            this.patient,
            this.proposals
        );

        ImmunizationRecommendationRecommendationComponent component = recommendation.getRecommendationFirstRep();

        assertEquals(3, component.getForecastReason().size());
    }

    @Test
    public void convertToCdsSetsIdFromFhirId() {
        this.recommendation.setId("recommendationId");

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertNotNull(proposal.getId());
        assertEquals("recommendationId", proposal.getId().getRoot());
    }

    @Test
    public void convertToCdsAddsNoTemplateIdsIfNoIdentifiers(){
        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(0, proposal.getTemplateId().size());
    }

    @Test
    public void convertToCdsAddsNoTemplateIdsIfNoTemplateIdIdentifier() {
        Identifier identifier = this.identifierFactory.create("notTemplateId", "id");

        this.recommendation.addIdentifier(identifier);
        this.recommendation.addIdentifier(identifier);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(0, proposal.getTemplateId().size());
    }

    @Test
    public void convertToCdsAddsTemplateIdForEachTemplateIdIdentifier() {
        Identifier identifier = this.identifierFactory.create("templateId", "id");

        this.recommendation.addIdentifier(identifier);
        this.recommendation.addIdentifier(identifier);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(2, proposal.getTemplateId().size());
    }

    @Test
    public void convertToCdsAddsNoRelatedClinicalStatementsIfNoRecommendationComponents() {
        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(0, proposal.getRelatedClinicalStatement().size());
    }

    @Test
    public void convertToCdsAddsRelatedClinicalStatementForEachRecommendationComponent() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        this.recommendation.addRecommendation(component);
        this.recommendation.addRecommendation(component);
        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(3, proposal.getRelatedClinicalStatement().size());
    }

    @Test
    public void convertToCdsDoesNotOverrideTimeIntervalIfAlreadySet() throws ParseException {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();
        ImmunizationRecommendationRecommendationComponent override = new ImmunizationRecommendationRecommendationComponent();

        ImmunizationRecommendationRecommendationDateCriterionComponent dateComponent = new ImmunizationRecommendationRecommendationDateCriterionComponent();
        ImmunizationRecommendationRecommendationDateCriterionComponent dateOverride = new ImmunizationRecommendationRecommendationDateCriterionComponent();

        CodeableConcept low = new CodeableConcept();
        low.setText("low");

        Date validDate = this.dateFormat.parse("20200702");
        Date overrideDate = this.dateFormat.parse("20200703");

        dateComponent.setCode(low);
        dateComponent.setValue(validDate);
        dateOverride.setCode(low);
        dateOverride.setValue(overrideDate);

        component.addDateCriterion(dateComponent);
        override.addDateCriterion(dateOverride);

        this.recommendation.addRecommendation(component);
        this.recommendation.addRecommendation(override);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals("20200702", proposal.getProposedAdministrationTimeInterval().getLow());
    }

    @Test
    public void convertToCdsDoesNotSetTimeIntervalIfNoLowOrHighValuesFound() {
        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);
        assertNull(proposal.getProposedAdministrationTimeInterval());
    }

    @Test
    public void convertToCdsSetsLowPropertyIfLowDateFound() throws ParseException {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();
        ImmunizationRecommendationRecommendationDateCriterionComponent dateComponent = new ImmunizationRecommendationRecommendationDateCriterionComponent();

        CodeableConcept low = new CodeableConcept();
        low.setText("low");

        Date validDate = this.dateFormat.parse("20200702");

        dateComponent.setCode(low);
        dateComponent.setValue(validDate);

        component.addDateCriterion(dateComponent);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals("20200702", proposal.getProposedAdministrationTimeInterval().getLow());
        assertNull(proposal.getProposedAdministrationTimeInterval().getHigh());
    }

    @Test
    public void convertToCdsSetsHighPropertyIfHighDateFound() throws ParseException {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();
        ImmunizationRecommendationRecommendationDateCriterionComponent dateComponent = new ImmunizationRecommendationRecommendationDateCriterionComponent();

        CodeableConcept high = new CodeableConcept();
        high.setText("high");

        Date validDate = this.dateFormat.parse("20200702");

        dateComponent.setCode(high);
        dateComponent.setValue(validDate);

        component.addDateCriterion(dateComponent);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertNull(proposal.getProposedAdministrationTimeInterval().getLow());
        assertEquals("20200702", proposal.getProposedAdministrationTimeInterval().getHigh());
    }

    @Test
    public void convertToCdsSetsLowAndHighIfBothFound() throws ParseException {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();
        ImmunizationRecommendationRecommendationDateCriterionComponent lowDate = new ImmunizationRecommendationRecommendationDateCriterionComponent();
        ImmunizationRecommendationRecommendationDateCriterionComponent highDate = new ImmunizationRecommendationRecommendationDateCriterionComponent();

        CodeableConcept low = new CodeableConcept();
        low.setText("low");

        CodeableConcept high = new CodeableConcept();
        high.setText("high");

        Date validDate = this.dateFormat.parse("20200702");

        lowDate.setCode(low);
        lowDate.setValue(validDate);

        highDate.setCode(high);
        highDate.setValue(validDate);

        component.addDateCriterion(lowDate);
        component.addDateCriterion(highDate);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals("20200702", proposal.getProposedAdministrationTimeInterval().getLow());
        assertEquals("20200702", proposal.getProposedAdministrationTimeInterval().getHigh());
    }

    @Test
    public void convertToCdsDoesNotAddGeneralPurposeIfNoContraindicatedVaccineCode() {
        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);
        assertNull(proposal.getSubstanceAdministrationGeneralPurpose());
    }

    @Test
    public void convertToCdsAddsGeneralPurposeFromContraindicatedVaccineCode() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        CodeableConcept contraindicatedVaccineCode = new CodeableConcept();
        contraindicatedVaccineCode.setText("vaccine code");

        component.addContraindicatedVaccineCode(contraindicatedVaccineCode);
        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);
        assertNotNull(proposal.getSubstanceAdministrationGeneralPurpose());
    }

    @Test
    public void convertToCdsOnlyUsesFirstContraindicatedVaccineCode() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        CodeableConcept contraindicatedVaccineCode = new CodeableConcept();
        Coding code = new Coding();
        code.setCode("one");
        contraindicatedVaccineCode.addCoding(code);

        contraindicatedVaccineCode.setText("vaccine code");

        CodeableConcept secondCode = new CodeableConcept();
        Coding falseCode = new Coding();
        falseCode.setCode("two");
        secondCode.addCoding(falseCode);

        contraindicatedVaccineCode.setText("second vaccine code");

        component.addContraindicatedVaccineCode(contraindicatedVaccineCode);
        component.addContraindicatedVaccineCode(secondCode);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertNotNull(proposal.getSubstanceAdministrationGeneralPurpose());
        assertEquals("one", proposal.getSubstanceAdministrationGeneralPurpose().getCode());
    }

    @Test
    public void convertToCdsDoesNotSetSubstanceIfNoVaccineCode() {
        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);
        assertNull(proposal.getSubstance());
    }

    @Test
    public void convertToCdsSetsSubstanceFromVaccineCode() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        CodeableConcept vaccineCode = new CodeableConcept();
        vaccineCode.setText("vaccine code");

        component.addVaccineCode(vaccineCode);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);
        assertNotNull(proposal.getSubstance());
    }

    @Test
    public void convertToCdsOnlyUsesFirstSubstance() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        CodeableConcept vaccineCodeTrue = new CodeableConcept();
        Coding codingTrue = new Coding();
        codingTrue.setCode("true");
        vaccineCodeTrue.addCoding(codingTrue);
        vaccineCodeTrue.setText("vaccine code");

        CodeableConcept vaccineCodeFalse = new CodeableConcept();
        Coding codingFalse = new Coding();
        codingFalse.setCode("false");
        vaccineCodeFalse.addCoding(codingFalse);
        vaccineCodeFalse.setText("vaccine code");

        component.addVaccineCode(vaccineCodeTrue);
        component.addVaccineCode(vaccineCodeFalse);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertNotNull(proposal.getSubstance());

        AdministrableSubstance substance = proposal.getSubstance();
        CD code = substance.getSubstanceCode();

        assertEquals("true", code.getCode());
    }

    @Test
    public void convertToCdsDoesNotSetObservationFocusIfNoTargetDisease() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertNull(proposal.getRelatedClinicalStatement().get(0).getObservationResult().getObservationFocus());
    }

    @Test
    public void convertToCdsSetsObservationFocusFromTargetDisease() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();
        CodeableConcept targetDisease = new CodeableConcept();
        targetDisease.setText("target disease");

        component.setTargetDisease(targetDisease);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertNotNull(proposal.getRelatedClinicalStatement().get(0).getObservationResult().getObservationFocus());
    }

    @Test
    public void convertToCdsSetsNoTemplateIdIfNoIdentifiers() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(
            0,
            proposal.getRelatedClinicalStatement().get(0).getObservationResult().getTemplateId().size()
        );
    }

    @Test
    public void convertToCdsSetsNoTemplateIdIfNoTemplateIdIdentifiers() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();
        Identifier notTemplateId = this.identifierFactory.create("notTemplateId", "id");

        recommendation.addIdentifier(notTemplateId);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(
            0,
            proposal.getRelatedClinicalStatement().get(0).getObservationResult().getTemplateId().size()
        );
    }

    @Test
    public void convertToCdsDoesNotAddTemplateIdIfNoSystem() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        Identifier identifier = this.identifierFactory.create("observationTemplateId", "not matching id");

        component.setId("correct");
        recommendation.addIdentifier(identifier);
        recommendation.addIdentifier(identifier);
        recommendation.addIdentifier(identifier);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(
            0,
            proposal.getRelatedClinicalStatement().get(0).getObservationResult().getTemplateId().size()
        );

    }

    @Test
    public void convertToCdsDoesNotAddTemplateIdIfNoMatchingSystem() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        Identifier identifier = this.identifierFactory.create("observationTemplateId", "not matching id");
        identifier.setSystem("system");

        component.setId("correct");
        recommendation.addIdentifier(identifier);
        recommendation.addIdentifier(identifier);
        recommendation.addIdentifier(identifier);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(
            0,
            proposal.getRelatedClinicalStatement().get(0).getObservationResult().getTemplateId().size()
        );
    }

    @Test
    public void convertToCdsAddsTemplateIdFromEachMatchingTemplateIdIdentifier() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();
        Identifier notMatch = this.identifierFactory.create("observationTemplateId", "not matching id");
        notMatch.setSystem("incorrect");

        Identifier match = this.identifierFactory.create("observationTemplateId", "matching id");
        match.setSystem("correct");

        component.setId("correct");
        recommendation.addIdentifier(notMatch);
        recommendation.addIdentifier(match);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(
            1,
            proposal.getRelatedClinicalStatement().get(0).getObservationResult().getTemplateId().size()
        );
    }

    @Test
    public void convertToCdsDoesNotSetObservationValueIfNoForecastStatus() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertNull(proposal.getRelatedClinicalStatement().get(0).getObservationResult().getObservationValue());
    }

    @Test
    public void convertToCdsSetsObservationValueFromForecastStatus() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        CodeableConcept forecastStatus = new CodeableConcept();
        forecastStatus.setText("forecast status");

        component.setForecastStatus(forecastStatus);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertNotNull(proposal.getRelatedClinicalStatement().get(0).getObservationResult().getObservationValue());
    }

    @Test
    public void convertToCdsAddsNoInterpretationIfNoForecastReason() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(
            0,
            proposal.getRelatedClinicalStatement().get(0).getObservationResult().getInterpretation().size()
        );
    }

    @Test
    public void convertToCdsAddsInterpretationForEachForecastReason() {
        ImmunizationRecommendationRecommendationComponent component = new ImmunizationRecommendationRecommendationComponent();

        CodeableConcept forecastReason = new CodeableConcept();
        forecastReason.setText("forecast reason");

        component.addForecastReason(forecastReason);
        component.addForecastReason(forecastReason);
        component.addForecastReason(forecastReason);

        this.recommendation.addRecommendation(component);

        SubstanceAdministrationProposal proposal = this.immunizationRecommendationConverter.convertToCds(this.recommendation);

        assertEquals(
            3,
            proposal.getRelatedClinicalStatement().get(0).getObservationResult().getInterpretation().size()
        );
    }
}