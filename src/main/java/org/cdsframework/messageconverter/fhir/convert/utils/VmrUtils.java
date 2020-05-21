package org.cdsframework.messageconverter.fhir.convert.utils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.cdsframework.util.LogUtils;
import org.cdsframework.util.support.cds.Config;

/**
 * @author sdn
 */
public class VmrUtils {

    private static final LogUtils logger = LogUtils.getLogger(VmrUtils.class);
    public static final Map<String, String> CODE_SYSTEM_MAP = new HashMap<>();
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
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            logger.error(e);
        }
    }

    public static String getOid(String identifier) {
        final String METHODNAME = "getOid ";
        String result = CODE_SYSTEM_MAP.get(identifier);
        if (result == null) {
            logger.error(METHODNAME, "could not find OID for submitted identifier: ", identifier);
        }
        return result;
    }
}
