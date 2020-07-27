package org.cdsframework.messageconverter.fhir.convert.vmr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.FhirConstants;
import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.cdsframework.util.LogUtils;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.json.JSONObject;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSOutput;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson;
import org.opencds.vmr.v1_0.schema.EvaluatedPerson.Demographics;
import org.opencds.vmr.v1_0.schema.TS;
import org.opencds.vmr.v1_0.schema.VMR;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;

/**
 * @author Brian Lamb
 */
public class PatientConverter implements CdsConverter, FhirConverter<EvaluatedPerson, Patient> {
    protected AdministrativeGenderConverter administrativeGenderConverter = new AdministrativeGenderConverter();
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    protected IdentifierFactory identifierFactory = new IdentifierFactory();
    private final LogUtils logger = LogUtils.getLogger(ImmunizationConverter.class);

    /**
     * Convert a json object of fhir data to cds format. Save the results to the ice
     * cds input wrapper.
     *
     * @param IceCdsInputWrapper wrapper : wrapper object, used to store patient
     *                           data
     * @param JSONObject         data : a json object of fhir data
     * @return IceCdsInputWrapper object updated with fhir data
     */
    public IceCdsInputWrapper convertToCds(IceCdsInputWrapper wrapper, JSONObject data) {
        this.convertToCds(wrapper.getCdsInputWrapper(), data);
        return wrapper;
    }

    /**
     * Convert a FHIR compliant Patient object into an OpenCDS compliant
     * EvaluatedPerson object.
     *
     * @param Patient patient : the FHIR compliant object to convert
     * @return EvaluatedPerson
     */
    public EvaluatedPerson convertToCds(Patient patient) {
        EvaluatedPerson person = new EvaluatedPerson();
        Demographics demographics = new Demographics();

        CD gender = this.administrativeGenderConverter.convertToCds(patient.getGender());

        try {
            SimpleDateFormat birthDateFormat = new SimpleDateFormat("yyyymmdd");
            TS birthTime = new TS();

            birthTime.setValue(
                birthDateFormat.format(patient.getBirthDate())
            );

            demographics.setBirthTime(birthTime);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToCds", "Cannot get birth date");
        }

        demographics.setGender(gender);
        person.setDemographics(demographics);

        return person;
    }

    /**
     * Convert a json object of fhir data to cds format. Save the results to the cds
     * input wrapper.
     *
     * @param CdsInputWrapper wrapper : wrapper object, used to store patient data
     * @param JSONObject      data : a json object of fhir data
     * @return CdsInputWrapper object updated with fhir data
     */
    public CdsInputWrapper convertToCds(CdsInputWrapper wrapper, JSONObject data) {
        Patient patient = this.convertToFhir(data);

        HumanName humanName = patient.getNameFirstRep();
        List<StringType> givenNames = humanName.getGiven();
        StringType givenName = givenNames.get(0);
        String familyName = humanName.getFamily();

        if (patient.getGender() != null) {
            wrapper.setPatientGender(patient.getGender().toCode(), FhirConstants.GENDER_CODE_SYSTEM);
        }

        if (patient.getBirthDate() != null) {
            wrapper.setPatientBirthTime(patient.getBirthDate());
        }

        wrapper.setPatientName(givenName.asStringValue(), familyName);
        wrapper.setPatientId(patient.getId());

        return wrapper;
    }

    /**
     * To make parsing the patient data easier, convert to a patient object to
     * easily get the data out.
     *
     * @param JSONObject data : the patient fhir data
     * @return a patient object populated via the fhir data
     */
    public Patient convertToFhir(JSONObject data) {
        FhirContext ctx = FhirContext.forR4();

        // Create a parser and configure it to use the strict error handler
        IParser parser = ctx.newJsonParser();
        parser.setParserErrorHandler(new StrictErrorHandler());

        // get the string representation of the json object
        String str = data.toString();

        // The following will throw a DataFormatException because of the
        // StrictErrorHandler
        Patient patient = parser.parseResource(Patient.class, str);
        return patient;
    }

    /**
     * Converts a CDSOutput object into a Patient record. The patient data exists in
     * the VMR object inside of the EvaluatedPerson object. That object is passed to
     * a method to convert the EvaluatedPerson object to a Patient.
     *
     * @param CDSOutput : the cds output object to convert to a Patient object
     * @return a patient object
     */
    public Patient convertToFhir(CDSOutput output) throws IllegalArgumentException, ParseException {
        VMR vmr = output.getVmrOutput();
        EvaluatedPerson patient = vmr.getPatient();

        return this.convertToFhir(patient);
    }

    /**
     * Convert an EvaluatedPerson from a VMR record to the FHIR version of the
     * Patient. For now, it only saves the id to be used as a reference but this can
     * be updated to include additional metadata.
     *
     * @param EvaluatedPerson person : the evaluated person from a VMR record
     * @return a patient object
     */
    public Patient convertToFhir(EvaluatedPerson person) throws IllegalArgumentException {
        Patient patient = new Patient();

        Meta meta = new Meta();
        meta.addProfile("http://hl7.org/fhir/us/ImmunizationFHIRDS/StructureDefinition/immds-patient");

        patient.setMeta(meta);
        patient.setId(UUID.randomUUID().toString());

        try {
            AdministrativeGender gender = this.administrativeGenderConverter.convertToFhir(
                person.getDemographics().getGender()
            );

            patient.setGender(gender);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No gender found in EvaluatedPerson");
        }

        try {
            Date birthdate = new SimpleDateFormat("yyyymmdd").parse(
                person.getDemographics().getBirthTime().getValue()
            );
            patient.setBirthDate(birthdate);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No birthtime found in EvaluatedPerson");
        } catch (ParseException exception) {
            this.logger.debug("convertToFhir", "No birthtime in EvaluatedPerson is improperly formatted");
        }

        return patient;
    }
}