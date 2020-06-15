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
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CDSInput;

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
}