package org.cdsframework.messageconverter.fhir.convert.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.hl7.fhir.r4.model.DomainResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * @author Brian Lamb
 */
public class FhirOutput {
    public String convertToString(DomainResource resource) {
        // Create a FHIR context
        FhirContext ctx = FhirContext.forR4();

        // Create a parser
        IParser parser = ctx.newJsonParser();

        // Indent the output
        parser.setPrettyPrint(true);

        // Serialize it
        String serialized = parser.encodeResourceToString(resource);

        return serialized;
    }

    public void convertToFile(String pathToFile, String header) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(pathToFile, true));

        writer.write(header + "\n");
        writer.close();
    }

    public void convertToFile(DomainResource resource, String pathToFile) throws IOException {
        String data = this.convertToString(resource);

        BufferedWriter writer = new BufferedWriter(new FileWriter(pathToFile, true));
        writer.write(data + "\n");

        writer.close();
    }

    public void convertToFile(DomainResource resource, String pathToFile, String header) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(pathToFile, true));

        writer.write(header + "\n");
        writer.close();

        this.convertToFile(resource, pathToFile);
    }
}
