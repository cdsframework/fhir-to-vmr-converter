package org.cdsframework.messageconverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CDSInput;
import org.xml.sax.SAXException;

/**
 * @author Brian Lamb
 */
public class EndToEndTest {
    protected String[] files;
    protected String directory = "src/test/resources/ice-test-cases/inputs";

    @Before
    public void setUp() {
        File testDirectory = new File(this.directory);
        this.files = testDirectory.list();
    }

    @Test
    public void cdsInputToFhirToCdsInputIsTheSame() throws SAXException, ParserConfigurationException, IOException {
        CDSInput input = new CDSInput();

        for (String filename : this.files) {
            byte[] data = Files.readAllBytes(Paths.get(this.directory + "/" + filename));
            input = CdsObjectAssist.cdsObjectFromByteArray(data, CDSInput.class);

            
        }
    }
}