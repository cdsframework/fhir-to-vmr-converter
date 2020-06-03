package org.cdsframework.messageconverter.fhir.convert.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Brian Lamb
 */
public class VmrUtilsTest {
    @Test
    public void getOidNoMatchReturnsNullTest() {
        String oid = VmrUtils.getOid("nope");
        assertNull(oid);
    }

    @Test
    public void getOidMatchReturnsNotNullTest() {
        String oid = VmrUtils.getOid("http://loinc.org");
        assertNotNull(oid);
    }
}