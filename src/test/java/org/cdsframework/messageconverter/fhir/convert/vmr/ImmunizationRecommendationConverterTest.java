package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.AdministrableSubstance;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSOutput;
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

    @Before
    public void setUp() throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/recommendation.xml"));
        this.output = CdsObjectAssist.cdsObjectFromByteArray(data, CDSOutput.class);

        this.code = new CD();
        this.code.setCode("jut");
        this.code.setDisplayName("Junit Test");
    }

    @Test
    public void convertToFhirReturnsEmptyObjectIfNoDataSet() {
        CDSOutput blankOutput = new CDSOutput();
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(blankOutput);

        assertTrue(recommendation.getPatientTarget().isEmpty());
        assertEquals(recommendation.getRecommendation().size(), 0);
    }

    @Test
    public void convertToFhirHasNoRecommendationsIfNoneProvided() {
        this.output.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationProposals().getSubstanceAdministrationProposal().clear();

        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(this.output);

        assertFalse(recommendation.getPatientTarget().isEmpty());
        assertEquals(recommendation.getRecommendation().size(), 0);
    }

    @Test
    public void convertToFhirAddsAllRecommendations() {
        ImmunizationRecommendation recommendation = this.immunizationRecommendationConverter.convertToFhir(this.output);

        assertFalse(recommendation.getPatientTarget().isEmpty());
        assertNotEquals(recommendation.getRecommendation().size(), 0);
    }

    @Test
    public void convertToFhirDoesntAddAnythingIfNoneGiven() {
        SubstanceAdministrationProposal proposal = new SubstanceAdministrationProposal();
        ImmunizationRecommendationRecommendationComponent recommendation = this.immunizationRecommendationConverter.convertToFhir(proposal);

        assertEquals(recommendation.getVaccineCode().size(), 0);
        assertTrue(recommendation.getTargetDisease().isEmpty());
        assertEquals(recommendation.getForecastReason().size(), 0);
    }

    @Test
    public void convertToFhirAddsVaccineCodes() {
        SubstanceAdministrationProposal proposal = new SubstanceAdministrationProposal();
        AdministrableSubstance substance = new AdministrableSubstance();

        substance.setSubstanceCode(this.code);
        proposal.setSubstance(substance);

        ImmunizationRecommendationRecommendationComponent recommendation = this.immunizationRecommendationConverter.convertToFhir(proposal);

        assertEquals(recommendation.getVaccineCode().size(), 1);
        assertTrue(recommendation.getTargetDisease().isEmpty());
        assertEquals(recommendation.getForecastReason().size(), 0);        
    }

    @Test
    public void convertToFhirAddsTargetDisease() {
        SubstanceAdministrationProposal proposal = new SubstanceAdministrationProposal();
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();

        clinicalStatement.setObservationResult(observationResult);
        observationResult.setObservationFocus(this.code);

        proposal.getRelatedClinicalStatement().add(clinicalStatement);

        ImmunizationRecommendationRecommendationComponent recommendation = this.immunizationRecommendationConverter.convertToFhir(proposal);

        assertEquals(recommendation.getVaccineCode().size(), 0);
        assertFalse(recommendation.getTargetDisease().isEmpty());
        assertEquals(recommendation.getForecastReason().size(), 0);         
    }

    @Test
    public void convertToFhirAddsMultipleForecastReasons() {
        SubstanceAdministrationProposal proposal = new SubstanceAdministrationProposal();
        RelatedClinicalStatement clinicalStatement = new RelatedClinicalStatement();
        ObservationResult observationResult = new ObservationResult();
        ObservationValue observationValue = new ObservationValue();

        clinicalStatement.setObservationResult(observationResult);
        observationResult.setObservationValue(observationValue);
        observationValue.setConcept(this.code);

        proposal.getRelatedClinicalStatement().add(clinicalStatement);
        proposal.getRelatedClinicalStatement().add(clinicalStatement);

        ImmunizationRecommendationRecommendationComponent recommendation = this.immunizationRecommendationConverter.convertToFhir(proposal);

        assertEquals(recommendation.getVaccineCode().size(), 0);
        assertTrue(recommendation.getTargetDisease().isEmpty());
        assertEquals(recommendation.getForecastReason().size(), 2);
    }
}