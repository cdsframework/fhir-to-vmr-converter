package org.cdsframework.messageconverter.fhir.convert.vmr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.cdsframework.cds.vmr.CdsInputWrapper;
import org.cdsframework.ice.input.IceCdsInputWrapper;
import org.cdsframework.messageconverter.fhir.convert.utils.IdentifierFactory;
import org.cdsframework.messageconverter.fhir.convert.utils.VmrUtils;
import org.cdsframework.util.LogUtils;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.json.JSONObject;
import org.opencds.vmr.v1_0.schema.AdministrableSubstance;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.II;
import org.opencds.vmr.v1_0.schema.IVLTS;
import org.opencds.vmr.v1_0.schema.ObservationResult;
import org.opencds.vmr.v1_0.schema.ObservationResult.ObservationValue;
import org.opencds.vmr.v1_0.schema.SubstanceAdministrationEvent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;

/**
 * @author sdn
 */
public class ImmunizationConverter implements CdsConverter, FhirConverter<SubstanceAdministrationEvent, Immunization> {
    protected CodeableConceptConverter codeableConceptConverter = new CodeableConceptConverter();
    private final LogUtils logger = LogUtils.getLogger(ImmunizationConverter.class);
    protected IdentifierFactory identifierFactory = new IdentifierFactory();

    protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmdd");

    /**
     * This converts a FHIR Immunization object into an ObservationResult OpenCDS object. There
     * is some overlap between this and the SubstanceAdministrationEvent version of this method.
     *
     * @param Immunization immunization : the FHIR Immunization object
     * @return ObservationResult
     */
    public ObservationResult convertToCdsObservation(Immunization immunization) {
        ObservationResult observation = new ObservationResult();

        // template id is stored with the Identifiers and distinguished by the concept type
        for (Identifier identifier : immunization.getIdentifier()) {
            CodeableConcept concept = identifier.getType();

            if (concept.getText() == "templateId") {
                II templateId = new II();
                templateId.setRoot(identifier.getValue());

                observation.getTemplateId().add(templateId);
            }
        }

        II id = new II();
        id.setRoot(immunization.getId());
        observation.setId(id);

        CD observationFocus = this.codeableConceptConverter.convertToCds(immunization.getVaccineCode());
        observation.setObservationFocus(observationFocus);

        try {
            // this is safe because if we can't access the date, we don't need to set it anyway
            IVLTS observationEventTime = new IVLTS();
            String observationTime = this.dateFormat.format(immunization.getOccurrenceDateTimeType().getValue());
            observationEventTime.setHigh(observationTime);
            observationEventTime.setLow(observationTime);
            observation.setObservationEventTime(observationEventTime);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToCdsObservation", "No date found in immunization");
        }

        // usually there is only one but this allows for multiples
        for (CodeableConcept reasonCode : immunization.getReasonCode()) {
            CD interpretation = this.codeableConceptConverter.convertToCds(reasonCode);
            observation.getInterpretation().add(interpretation);
        }

        CodeableConcept statusReason = immunization.getStatusReason();
        CD observationValueCode = this.codeableConceptConverter.convertToCds(statusReason);
        ObservationValue observationValue = new ObservationValue();

        observationValue.setConcept(observationValueCode);
        observation.setObservationValue(observationValue);

        return observation;
    }

    /**
     * Convert a FHIR compliant Immunization object into a SubstanceAdministrationEvent
     * object for OpenCDS
     *
     * @param Immunization immunization : the FHIR compliant Immunization object
     * @return SubstanceAdministrationEvent
     */
    public SubstanceAdministrationEvent convertToCds(Immunization immunization) {
        SubstanceAdministrationEvent event = new SubstanceAdministrationEvent();
        AdministrableSubstance substance = new AdministrableSubstance();

        II id = new II();
        id.setRoot(immunization.getId());

        try {
            II substanceId = new II();
            substanceId.setRoot(immunization.getVaccineCode().getId());
            substance.setId(substanceId);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToCds", "No vaccine code found");
        }

        try {
            // this is safe because if we can't access the date, we don't need to set it anyway
            IVLTS administrationTimeInterval = new IVLTS();
            administrationTimeInterval.setHigh(
                this.dateFormat.format(immunization.getRecorded())
            );
            administrationTimeInterval.setLow(
                this.dateFormat.format(immunization.getRecorded())
            );

            event.setAdministrationTimeInterval(administrationTimeInterval);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToCds", "No date found in immunization");
        }

        // template id is stored with the Identifiers and distinguished by the concept type
        for (Identifier identifier : immunization.getIdentifier()) {
            CodeableConcept concept = identifier.getType();

            // there is usually only one of these but this allows for multiple
            if (concept.getText() == "templateId") {
                II templateId = new II();
                templateId.setRoot(identifier.getValue());

                event.getTemplateId().add(templateId);
            } else if (concept.getText() == "idExtension") {
                id.setExtension(identifier.getValue());
            }
        }

        CD code = this.codeableConceptConverter.convertToCds(immunization.getVaccineCode());
        CD generalPurpose = this.codeableConceptConverter.convertToCds(immunization.getReasonCodeFirstRep());

        substance.setSubstanceCode(code);

        event.setSubstance(substance);
        event.setId(id);

        event.setSubstanceAdministrationGeneralPurpose(generalPurpose);

        return event;
    }

    /**
     * Convert a json object of fhir data to cds format. Save the results to the ice cds input wrapper.
     * 
     * @param IceCdsInputWrapper wrapper : wrapper object, used to store immunization data
     * @param JSONObject data : a json object of fhir data
     * @return IceCdsInputWrapper object updated with fhir data
     */    
    public IceCdsInputWrapper convertToCds(IceCdsInputWrapper wrapper, JSONObject data) {
        Immunization immunization = this.convertToFhir(data);

        if (immunization.hasOccurrence()
            && immunization.hasOccurrenceDateTimeType()
            && immunization.hasId()
            && immunization.hasVaccineCode()) {

            CodeableConcept vaccineCode = immunization.getVaccineCode();
            Coding code = vaccineCode.getCodingFirstRep();

            if (code != null) {
                String substanceCodeOid = VmrUtils.getOid(code.getSystem());

                if (substanceCodeOid != null) {
                    String c = code.getCode();
                    String root = immunization.getId();

                    wrapper.addSubstanceAdministrationEvent(
                        c,
                        substanceCodeOid,
                        immunization.getOccurrenceDateTimeType().getValue(),
                        root, 
                        Config.getCodeSystemOid("ADMINISTRATION_ID")
                    );
                }
            }
        }

        return wrapper;        
    }

    /**
     * Convert a json object of fhir data to cds format. Save the results to the cds input wrapper.
     * 
     * @param CdsInputWrapper wrapper : wrapper object, used to store immunization data
     * @param JSONObject data : a json object of fhir data
     * @return CdsInputWrapper object updated with fhir data
     */    
    public CdsInputWrapper convertToCds(CdsInputWrapper wrapper, JSONObject data) {
        IceCdsInputWrapper iceInput = new IceCdsInputWrapper(wrapper);
        iceInput = this.convertToCds(iceInput, data);

        return iceInput.getCdsInputWrapper();
    }

    /**
     * To make parsing the immunization data easier, convert to an immunization object to easily get
     * the data out.
     * 
     * @param JSONObject data : the immunization fhir data
     * @return a immunization object populated via the fhir data
     */    
    public Immunization convertToFhir(JSONObject data) {
        FhirContext ctx = FhirContext.forR4();

        // Create a parser and configure it to use the strict error handler
        IParser parser = ctx.newJsonParser();
        parser.setParserErrorHandler(new StrictErrorHandler());

        // get the string representation of the json object
        String str = data.toString();
                
        // The following will throw a DataFormatException because of the StrictErrorHandler
        Immunization immunization = parser.parseResource(Immunization.class, str);
        return immunization;
    }

    /**
     * This method converts an OpenCDS ObservationResult to a FHIR compliant Immunization
     * object. This converts all valid data into Immunization data.
     *
     * @param ObservationResult result : the OpenCDS object containing the immunization data
     */
    public Immunization convertToFhir(ObservationResult result) {
        Immunization immunization = new Immunization();

        // there is usually only one template id but we can have several
        // we mark it as a template id and throw it in identifiers
        for (II templateId : result.getTemplateId()) {
            Identifier templateIdentifier = this.identifierFactory.create("templateId", templateId.getRoot());
            immunization.addIdentifier(templateIdentifier);
        }

        try {
            immunization.setId(result.getId().getRoot());
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No id found in observation result");
        }

        try {
            CodeableConcept vaccineCode = this.codeableConceptConverter.convertToFhir(result.getObservationFocus());
            immunization.setVaccineCode(vaccineCode);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No observation focus found");
        }

        try {
            // this is okay because if the date is bad, it shouldn't halt execution
            Date observationEventTime = this.dateFormat.parse(result.getObservationEventTime().getHigh());
            DateTimeType occurrence = new DateTimeType(observationEventTime);

            immunization.setOccurrence(occurrence);
        } catch (ParseException exception) {
            this.logger.debug("convertToFhir", "Improperly formatted observation event time");
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No observation event time found");
        }

        // there is usually only one of these
        for (CD interpretation : result.getInterpretation()) {
            CodeableConcept statusReason = this.codeableConceptConverter.convertToFhir(interpretation);
            immunization.addReasonCode(statusReason);
        }

        try {
            CD observationValue = result.getObservationValue().getConcept();
            CodeableConcept statusReason = this.codeableConceptConverter.convertToFhir(observationValue);
            immunization.setStatusReason(statusReason);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No observation value found");
        }

        return immunization;
    }

    /**
     * Immunization data can be located in a substance administration event object. This method
     * converts that object to an Immunization data record.
     *
     * @param SubstanceAdministrationEvent event
     * @return Immunization
     */
    public Immunization convertToFhir(SubstanceAdministrationEvent event) {
        Immunization immunization = new Immunization();

        // there is usually only one template id but we can have several
        // we mark it as a template id and throw it in identifiers
        for (II templateId : event.getTemplateId()) {
            Identifier templateIdentifier = this.identifierFactory.create("templateId", templateId.getRoot());
            immunization.addIdentifier(templateIdentifier);
        }

        try {
            // we don't want to stop here if a bad date, just ignore it and continue
            Date administeredDate = this.dateFormat.parse(event.getAdministrationTimeInterval().getHigh());
            immunization.setRecorded(administeredDate);
        } catch (ParseException exception) {
            this.logger.debug("convertToFhir", "Improper administration time interval format");
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No administration time interval found");
        }

        try {
            String extension = event.getId().getExtension();

            if (!extension.equals("")) {
                Identifier idExtension = this.identifierFactory.create("idExtension", event.getId().getExtension());
                immunization.addIdentifier(idExtension);
            }

            immunization.setId(event.getId().getRoot());
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No id found");
        }

        try {
            CodeableConcept vaccineCode = this.codeableConceptConverter.convertToFhir(event.getSubstance().getSubstanceCode());
            vaccineCode.setId(event.getSubstance().getId().getRoot());
            vaccineCode.getCodingFirstRep().setSystem(event.getSubstance().getSubstanceCode().getCodeSystem());
            immunization.setVaccineCode(vaccineCode);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No substance found for vaccine code");
        }

        try {
            CodeableConcept reason = this.codeableConceptConverter.convertToFhir(event.getSubstanceAdministrationGeneralPurpose());
            immunization.addReasonCode(reason);
        } catch (NullPointerException exception) {
            this.logger.debug("convertToFhir", "No substance administration event found");
        }

        return immunization;
    }
}