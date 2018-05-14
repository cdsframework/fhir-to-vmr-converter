package org.cdsframework.messageconverter.fhir.convert.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.cdsframework.cds.util.CdsObjectFactory;
import org.cdsframework.util.LogUtils;
import org.cdsframework.util.support.cds.Config;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 *
 * @author sdn
 */
public class VmrUtils {

    private static final LogUtils logger = LogUtils.getLogger(VmrUtils.class);
    private static final Map<String, String> CODE_SYSTEM_MAP = new HashMap<>();
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{new X509TrustManager() {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String string) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String string) throws CertificateException {
        }
    }
    };

    static {
        CODE_SYSTEM_MAP.put("http://loinc.org", "2.16.840.1.113883.6.1");
        CODE_SYSTEM_MAP.put("http://www2a.cdc.gov/vaccines/IIS/IISStandards/vaccines.asp?rpt=cvx", Config.getCodeSystemOid("VACCINE"));
        CODE_SYSTEM_MAP.put("http://hl7.org/fhir/sid/cvx", Config.getCodeSystemOid("VACCINE"));
        CODE_SYSTEM_MAP.put("http://hl7.org/fhir/sid/icd-9-cm", "2.16.840.1.113883.6.103");
        CODE_SYSTEM_MAP.put("http://hl7.org/fhir/sid/icd-10-cm", "2.16.840.1.113883.6.90");
        CODE_SYSTEM_MAP.put("http://hl7.org/fhir/sid/icd-10-de", "2.16.840.1.113883.6.3.2");
        CODE_SYSTEM_MAP.put("http://hl7.org/fhir/sid/icd-10-nl", "1.2.276.0.76.5.409");
        CODE_SYSTEM_MAP.put("http://snomed.info/sct", "2.16.840.1.113883.6.96");
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TRUST_ALL_CERTS, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public static void setSystemUserType(CDSInput cdsInput) {

        cdsInput.getTemplateId().clear();
        cdsInput.getTemplateId().add(CdsObjectFactory.getII(FhirConstants.CDSINPUT_TEMPLATE_ID));

        cdsInput.getCdsContext().setCdsInformationRecipientPreferredLanguage(null);

        CD systemUserType = new CD();
        systemUserType.setCodeSystem(FhirConstants.SYSTEMUSERTYPE_CODESYSTEM);
        systemUserType.setCode(FhirConstants.PROVIDER_FACILITY);
        cdsInput.getCdsContext().setCdsSystemUserType(systemUserType);
    }

    public static JsonObject retrieveResource(Gson gson, String url, String accessToken) {
        final String METHODNAME = "retrieveResource ";
        logger.warn(METHODNAME, "url=" + url);
        JsonObject jsonObject = null;

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TRUST_ALL_CERTS, new SecureRandom());

            Client client = ClientBuilder.newBuilder().sslContext(sc).build();

            WebTarget resource = client.target(url);
            Invocation.Builder request = resource.request();
            if (accessToken != null) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }

            request.accept(MediaType.APPLICATION_JSON);

            Response response = request.get();
            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                logger.debug(METHODNAME, "Success! status=" + response.getStatus());
                String entity = response.readEntity(String.class);
                logger.debug(METHODNAME, "entity=", entity);
                JsonElement jsonELement = gson.fromJson(entity, JsonElement.class);
                if (!jsonELement.isJsonNull()) {
                    jsonObject = (JsonObject) jsonELement;
                }
            } else {
                logger.warn(METHODNAME, "ERROR! status=" + response.getStatus());
                String entity = response.readEntity(String.class);
                logger.warn(METHODNAME, "entity=", entity);
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return jsonObject;
    }

    public static boolean isCategoryMatch(List<CodeableConcept> categories, String matchString) {
        boolean result = false;
        if (categories != null) {
            for (CodeableConcept category : categories) {
                for (Coding coding : category.getCoding()) {
                    if (coding != null && matchString.equalsIgnoreCase(coding.getCode())) {
                        result = true;
                        break;
                    }
                }
                if (result) {
                    break;
                }
            }
        }
        return result;
    }

    public static Coding getFirstCoding(CodeableConcept codeableConcept) {
        Coding result = null;
        for (Coding coding : codeableConcept.getCoding()) {
            if (coding != null) {
                result = coding;
                break;
            }
        }
        return result;
    }

    public static String getOid(String identifier) {
        final String METHODNAME = "getOid ";
        String result = CODE_SYSTEM_MAP.get(identifier);
        if (result == null) {
            logger.error(METHODNAME, "could not find OID for submitted identifier: ", identifier);
        }
        return result;
    }

    public static JsonObject getMissingData(Gson gson, String objectName, String patientId, String fhirServer, String accessToken) {
        return VmrUtils.retrieveResource(gson, fhirServer + objectName + "?patient=" + patientId, accessToken);
    }

    public static JsonObject getJsonObject(JsonObject parent, String node) {
        JsonObject result = null;
        if (parent.has(node)) {
            JsonElement element = parent.get(node);
            if (!(element.isJsonNull())) {
                result = element.getAsJsonObject();
            }
        }
        return result;
    }

    public static String getJsonObjectAsString(JsonObject parent, String node) {
        String result = null;
        if (parent.has(node)) {
            JsonElement element = parent.get(node);
            if (!(element.isJsonNull())) {
                result = element.getAsString();
            }
        }
        return result;
    }

    public static JsonObject getJsonObjectFromPrefetchOrServer(JsonObject prefetchObject, String objectString, Gson gson, String patientId, String fhirServer, String accessToken) {
        JsonObject result = null;
        if (prefetchObject != null) {
            result = VmrUtils.getJsonObject(prefetchObject, objectString.toLowerCase());
        }
        if (result == null) {
            result = VmrUtils.getMissingData(gson, objectString, patientId, fhirServer, accessToken);
        }
        return result;
    }
}
