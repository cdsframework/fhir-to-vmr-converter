/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cdsframework.messageconverter;

import java.util.Map;
import org.cdsframework.cds.vmr.CdsObjectAssist;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 *
 * @author sdn
 */
class Fhir2Vmr {

    static CDSInput getCdsInputFromMap(Map<String, Object> map) {
        return CdsObjectAssist.cdsObjectFromByteArray(new String("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
"<ns4:cdsInput xmlns:ns2=\"org.opencds.vmr.v1_0.schema.vmr\" xmlns:ns3=\"org.opencds.vmr.v1_0.schema.cdsinput.specification\" xmlns:ns4=\"org.opencds.vmr.v1_0.schema.cdsinput\" xmlns:ns5=\"org.opencds.vmr.v1_0.schema.cdsoutput\">\n" +
"    <templateId root=\"2.16.840.1.113883.3.795.11.1.1\"/>\n" +
"    <templateId root=\"2.16.840.1.113883.3.1829.11.1.1.1\"/>\n" +
"    <cdsContext>\n" +
"        <cdsSystemUserType code=\"LAB_FACILITY\" codeSystem=\"2.16.840.1.113883.3.795.5.4.12.2.1\"/>\n" +
"        <cdsSystemUserPreferredLanguage code=\"en\" codeSystem=\"2.16.840.1.113883.6.99\" displayName=\"English\"/>\n" +
"    </cdsContext>\n" +
"    <vmrInput>\n" +
"        <templateId root=\"2.16.840.1.113883.3.795.11.1.1\"/>\n" +
"        <templateId root=\"2.16.840.1.113883.3.1829.11.1.2.1\"/>\n" +
"        <patient>\n" +
"            <templateId root=\"2.16.840.1.113883.3.795.11.2.1.1\"/>\n" +
"            <templateId root=\"2.16.840.1.113883.3.1829.11.2.1.1\"/>\n" +
"            <id root=\"2.16.840.1.113883.3.795.5.2.1.1\" extension=\"2707abf2aeb2cefaf5fb16c7a9c84d8a\"/>\n" +
"            <demographics>\n" +
"                <birthTime value=\"19910504\"/>\n" +
"                <gender code=\"F\" codeSystem=\"2.16.840.1.113883.5.1\" codeSystemName=\"GENDER\" displayName=\"Female\"/>\n" +
"                <address use=\"HP\">\n" +
"                    <part type=\"SAL\" value=\"123 My Home Street\"/>\n" +
"                    <part type=\"CTY\" value=\"Some City\"/>\n" +
"                    <part type=\"STA\" value=\"DEFAULT\"/>\n" +
"                    <part type=\"ZIP\" value=\"00000\"/>\n" +
"                    <part type=\"CPA\" value=\"Some County\"/>\n" +
"                    <part type=\"CNT\" value=\"USA\"/>\n" +
"                </address>\n" +
"            </demographics>\n" +
"            <relatedEntity>\n" +
"                <targetRole/>\n" +
"                <facility>\n" +
"                    <templateId root=\"2.16.840.1.113883.3.1829.11.13.2.2\"/>\n" +
"                    <id root=\"2.1.1.3\"/>\n" +
"                    <entityType code=\"LAB\" codeSystem=\"2.16.840.1.113883.3.795.5.4.12.2.1\"/>\n" +
"                    <address use=\"\">\n" +
"                        <part type=\"SAL\" value=\"123 Facility/Provider Street\"/>\n" +
"                        <part type=\"CTY\" value=\"Some City\"/>\n" +
"                        <part type=\"STA\" value=\"DEFAULT\"/>\n" +
"                        <part type=\"ZIP\" value=\"00000\"/>\n" +
"                        <part type=\"CPA\" value=\"Some County\"/>\n" +
"                        <part type=\"CNT\" value=\"USA\"/>\n" +
"                    </address>\n" +
"                </facility>\n" +
"            </relatedEntity>\n" +
"            <clinicalStatements>\n" +
"                <encounterEvents/>\n" +
"                <observationResults>\n" +
"                    <observationResult>\n" +
"                        <templateId root=\"2.16.840.1.113883.3.1829.11.6.3.15\"/>\n" +
"                        <id root=\"4.1.1.1\" extension=\"34725cc71f64d02c523c74abff513660\"/>\n" +
"                        <observationFocus code=\"14461-8\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Chlamydia trachomatis [Presence] in Blood by Organism specific culture\"/>\n" +
"                        <observationValue>\n" +
"                            <concept code=\"46651001\" codeSystem=\"2.16.840.1.113883.6.96\" codeSystemName=\"SNOMED-CT\" displayName=\"Isolated (qualifier value)\"/>\n" +
"                        </observationValue>\n" +
"                    </observationResult>\n" +
"                </observationResults>\n" +
"                <observationOrders/>\n" +
"                <problems/>\n" +
"            </clinicalStatements>\n" +
"        </patient>\n" +
"    </vmrInput>\n" +
"    <cdsResource>\n" +
"        <cdsResourceType code=\"RCKMS_Criteria_Debug_Mode\" codeSystem=\"2.16.840.1.113883.3.795.5.4.12.2.3\" codeSystemName=\"Reportable Conditions Focus\" displayName=\"Reportable Conditions Focus\"/>\n" +
"    </cdsResource>\n" +
"</ns4:cdsInput>").getBytes(), CDSInput.class);
    }
    
}
