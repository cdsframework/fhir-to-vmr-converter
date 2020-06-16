package org.cdsframework.messageconverter.fhir.convert.vmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.AdministrableSubstance;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;
import org.opencds.vmr.v1_0.schema.VMR;

/**
 * @author Brian Lamb
 */
public class ImmunizationEvaluationConverterTest {
    protected ImmunizationEvaluationConverter immunizationEvaluationConverter = new ImmunizationEvaluationConverter();
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
    public void convertToFhirReturnsNoEvaluationsIfNoPatient() {
        CDSOutput blankOutput = new CDSOutput();
        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(blankOutput);
        
        assertEquals(evaluations.size(), 0);
    }

    @Test
    public void convertToFhirReturnsNoEvaluationsIfNoSubstanceAdministrationEvents() {
        CDSOutput blankOutput = new CDSOutput();
        EvaluatedPerson patient = new EvaluatedPerson();
        VMR vmr = new VMR();

        vmr.setPatient(patient);

        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(blankOutput);
        
        assertEquals(evaluations.size(), 0);

    }
    
    @Test
    public void convertToFhirAddsEvaluations() {
        List<ImmunizationEvaluation> evaluations = this.immunizationEvaluationConverter.convertToFhir(this.output);
        assertFalse(evaluations.isEmpty());
    }

    @Test
    public void convertToFhirSetsPatientAndImmunization() {
        Patient patient = new Patient();
        patient.setId("id");

        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        AdministrableSubstance substance = new AdministrableSubstance();
        
        CD code = new CD();
        code.setCode("jut");
        code.setDisplayName("Junit Test");

        substance.setSubstanceCode(code);
        event.setSubstance(substance);
        event.setSubstanceAdministrationGeneralPurpose(code);

        ImmunizationEvaluation evaluation = this.immunizationEvaluationConverter.convertToFhir(patient, event);

        assertFalse(evaluation.getPatientTarget().isEmpty());
        assertFalse(evaluation.getImmunizationEventTarget().isEmpty());
    }
}