package org.cdsframework.messageconverter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;

import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CDSInput;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements.SubstanceAdministrationEvents;
import org.opencds.vmr.v1_0.schema.RelatedClinicalStatement;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationProposal;

/**
 * @author Brian Lamb
 */
public class EndToEndTest {
    protected String[] inputFiles;
    protected String[] outputFiles;
    protected String inputDirectory = "src/test/resources/ice-test-cases/inputs";
    protected String outputDirectory = "src/test/resources/ice-test-cases/outputs";

    protected Fhir2Vmr fhir2Vmr = new Fhir2Vmr();
    protected Vmr2Fhir vmr2Fhir = new Vmr2Fhir();

    @Before
    public void setUp() {
        this.inputFiles = new File(this.inputDirectory).list();
        this.outputFiles = new File(this.outputDirectory).list();
    }

    @Test
    public void cdsInputToFhirToCdsInputIsTheSame() throws IOException, ParseException {
        // run the test for every input file
        for (String filename : this.inputFiles) {
            byte[] data = Files.readAllBytes(Paths.get(this.inputDirectory + "/" + filename));
            CDSInput input = CdsObjectAssist.cdsObjectFromByteArray(data, CDSInput.class);

            // convert cds object to patient, immunizations, and observations
            List<Immunization> immunizations = this.vmr2Fhir.getImmunizations(input);
            List<Immunization> observations = this.vmr2Fhir.getObservations(input);
            Patient patient = this.vmr2Fhir.getPatient(input);

            // convert those fhir objects back to a cds input object
            CDSInput converted = this.fhir2Vmr.getCdsInputFromFhir(patient, immunizations, observations);

            // these do not slot neatly into a fhir resource so they are manually copied
            // for testing purposes
            converted.setCdsContext(input.getCdsContext());
            converted.getTemplateId().add(input.getTemplateId().get(0));
            converted.getVmrInput().getTemplateId().add(input.getVmrInput().getTemplateId().get(0));

            // convert cds objects to strings to compare
            String inputString = CdsObjectAssist.cdsObjectToString(input, CDSInput.class);
            String convertedString = CdsObjectAssist.cdsObjectToString(converted, CDSInput.class);

            // make sure both strings are equal to each other and not null
            assertNotNull(input);
            assertNotNull(converted);
            assertTrue(inputString.equals(convertedString));
        }
    }

    @Test
    public void cdsOutputToFhirToCdsOutputIsTheSame() throws IOException, ParseException, IllegalArgumentException {
         // run the test for every output file
         for (String filename : this.outputFiles) {
            byte[] data = Files.readAllBytes(Paths.get(this.outputDirectory + "/" + filename));
            CDSOutput output = CdsObjectAssist.cdsObjectFromByteArray(data, CDSOutput.class);

            // convert cds object to evaluations and recommendation
            List<ImmunizationEvaluation> evaluations = this.vmr2Fhir.getEvaluations(output);
            List<Immunization> observations = this.vmr2Fhir.getObservations(output);
            List<ImmunizationRecommendation> recommendations = this.vmr2Fhir.getRecommendations(output);
            List<Immunization> immunizations = this.vmr2Fhir.getImmunizations(output);

            Patient patient = this.vmr2Fhir.getPatient(output);

            // convert those fhir objects back to a cds output object
            CDSOutput converted = this.fhir2Vmr.getCdsOutputFromFhir(patient, observations, immunizations, evaluations, recommendations);

            // remove the things from the output that we aren't worried about mapping
            output.getVmrOutput().getTemplateId().clear();

            SubstanceAdministrationEvents events = output.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationEvents();

            if (events != null) {
                for (SubstanceAdministrationEvent event : events.getSubstanceAdministrationEvent()) {
                    for (RelatedClinicalStatement statement : event.getRelatedClinicalStatement()) {
                        statement.setTargetRelationshipToSource(null);

                        SubstanceAdministrationEvent eventInner = statement.getSubstanceAdministrationEvent();

                        for (RelatedClinicalStatement statementInner : eventInner.getRelatedClinicalStatement()) {
                            statementInner.setTargetRelationshipToSource(null);
                        }
                    }
                }
            }

            for (SubstanceAdministrationProposal proposal : output.getVmrOutput().getPatient().getClinicalStatements().getSubstanceAdministrationProposals().getSubstanceAdministrationProposal()) {
                for (RelatedClinicalStatement statement : proposal.getRelatedClinicalStatement()) {
                    statement.setTargetRelationshipToSource(null);
                }
            }

            // convert cds objects to strings to compare
            String outputString = CdsObjectAssist.cdsObjectToString(output, CDSOutput.class);
            String convertedString = CdsObjectAssist.cdsObjectToString(converted, CDSOutput.class);

            assertNotNull(output);
            assertNotNull(converted);
            assertTrue(outputString.equals(convertedString));
        }
    }
}