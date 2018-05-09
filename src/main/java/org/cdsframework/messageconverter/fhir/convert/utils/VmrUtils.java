package org.cdsframework.messageconverter.fhir.convert.utils;

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

    public static void retrieveResource(String url, String accessToken) {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(url);
        Invocation.Builder request = resource.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        request.accept(MediaType.APPLICATION_JSON);

        Response response = request.get();
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            logger.warn("Success! " + response.getStatus());
            logger.warn(response.readEntity(String.class));
        } else {
            logger.warn("ERROR! " + response.getStatus());
            logger.warn(response.readEntity(String.class));
        }
    }
}
