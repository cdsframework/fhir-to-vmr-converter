package org.cdsframework.messageconverter;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CDSInput;
import org.opencds.vmr.v1_0.schema.CDSOutput;

/**
 * @author Brian Lamb
 */
public class Vmr2FhirTest {
    protected Vmr2Fhir vmr2Fhir = new Vmr2Fhir();
    protected CDSOutput output;
    protected CDSInput input;

    @Before
    public void setUp() throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/recommendation.xml"));
        this.output = CdsObjectAssist.cdsObjectFromByteArray(data, CDSOutput.class);

        data = Files.readAllBytes(Paths.get("src/test/resources/vmrInput.xml"));
        this.input = CdsObjectAssist.cdsObjectFromByteArray(data, CDSInput.class);
    }

    @Test
    public void getEvaluationsContainsListOfEvaluations() {
        List<ImmunizationEvaluation> evaluations = this.vmr2Fhir.getEvaluations(this.output);
        
        assertFalse(evaluations.isEmpty());
    }

    @Test
    public void getRecommendationContainsRecommendations() {
        ImmunizationRecommendation recommendation = this.vmr2Fhir.getRecommendation(this.output);

        assertFalse(recommendation.getRecommendation().isEmpty());
    }

    @Test
    public void getImmunizationsExtractsImmunizationsFromCdsInput() {
        List<Immunization> immunizations = this.vmr2Fhir.getImmunizations(this.input);

        assertFalse(immunizations.isEmpty());
    }
}