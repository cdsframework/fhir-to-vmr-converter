package org.cdsframework.messageconverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.cdsframework.util.LogUtils;
import org.opencds.vmr.v1_0.schema.CDSInput;

/**
 *
 * @author sdn
 */
public class Fhir2VmrConverter {

    private static final LogUtils logger = LogUtils.getLogger(Fhir2VmrConverter.class);

    public CDSInput convert(String payload) throws IOException {
        final String METHODNAME = "convert ";
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map;
        map = mapper.readValue(payload, new TypeReference<Map<String, Object>>() {
        });
        logger.warn(METHODNAME, "map=", map);
        CDSInput cdsInput = Fhir2Vmr.getCdsInputFromMap(map);
        return cdsInput;
    }

}
