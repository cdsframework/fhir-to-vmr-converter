package org.cdsframework.messageconverter.fhir.convert.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.cdsframework.cds.util.CdsObjectFactory;
import org.cdsframework.util.LogUtils;
import org.opencds.vmr.v1_0.schema.CD;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 *
 * @author sdn
 */
public class VmrUtils {

    private static final LogUtils logger = LogUtils.getLogger(VmrUtils.class);

    public static void setSystemUserType(CDSInput cdsInput) {

        cdsInput.getTemplateId().clear();
        cdsInput.getTemplateId().add(CdsObjectFactory.getII(FhirConstants.CDSINPUT_TEMPLATE_ID));

        cdsInput.getCdsContext().setCdsInformationRecipientPreferredLanguage(null);

        CD systemUserType = new CD();
        systemUserType.setCodeSystem(FhirConstants.SYSTEMUSERTYPE_CODESYSTEM);
        systemUserType.setCode(FhirConstants.PROVIDER_FACILITY);
        cdsInput.getCdsContext().setCdsSystemUserType(systemUserType);
    }

    public static JsonElement retrieveResource(Gson gson, String url, String accessToken) {
        final String METHODNAME = "retrieveResource ";
        JsonElement jsonElement = null;
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(url);
        Invocation.Builder request = resource.request();
        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        request.accept(MediaType.APPLICATION_JSON);

        Response response = request.get();
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            logger.warn(METHODNAME, "Success! status=" + response.getStatus());
            String entity = response.readEntity(String.class);
            logger.warn(METHODNAME, "entity=", entity);
            jsonElement = gson.fromJson(entity, JsonElement.class);
        } else {
            logger.warn(METHODNAME, "ERROR! status=" + response.getStatus());
            String entity = response.readEntity(String.class);
            logger.warn(METHODNAME, "entity=", entity);
        }
        return jsonElement;
    }
}
