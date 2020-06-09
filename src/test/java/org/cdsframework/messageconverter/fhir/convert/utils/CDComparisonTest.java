package org.cdsframework.messageconverter.fhir.convert.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public class CDComparisonTest {
    protected CDComparison cdComparison = new CDComparison();
    protected CD base;
    protected CD compare;

    @Before
    public void setUp() {
        this.base = new CD();
        this.compare = new CD();

        this.base.setCode("cd");
        this.compare.setCode("cd");

        this.base.setCodeSystem("code system");
        this.compare.setCodeSystem("code system");

        this.base.setOriginalText("original text");
        this.compare.setOriginalText("original text");
    }
    
    @Test
    public void baseIsNullAreNotEqual() {
        assertFalse(this.cdComparison.isEqual(null, this.compare));
    }

    @Test
    public void differentClassAreNotEqual() {
        CDExtended base = new CDExtended();
        base.setCode("cd");
        base.setCodeSystem("code system");
        base.setOriginalText("original text");

        assertFalse(this.cdComparison.isEqual(base, this.compare));
    }
    
    @Test
    public void nullAndNotNullCodesAreNotEqual() {
        this.base.setCode(null);

        assertFalse(this.cdComparison.isEqual(this.base, this.compare));
    }
    
    @Test
    public void differentCodesAreNotEqual() {
        this.base.setCode("ncd");

        assertFalse(this.cdComparison.isEqual(this.base, this.compare));
    }
    
    @Test
    public void nullAndNotNullCodeSystemAreNotEqual() {
        this.base.setCodeSystem(null);

        assertFalse(this.cdComparison.isEqual(this.base, this.compare));
    }
    
    @Test
    public void differentCodeSystemsAreNotEqual() {
        this.base.setCodeSystem("new code system");

        assertFalse(this.cdComparison.isEqual(this.base, this.compare));
    }
    
    @Test
    public void nullOriginalTextNotNullCodeAreNotEqual() {
        this.base.setOriginalText(null);
        this.compare.setCode(null);

        assertFalse(this.cdComparison.isEqual(this.base, this.compare));
    }

    @Test
    public void notNullOriginalTextNullCodeAreNotEqual() {
        this.base.setCode(null);
        this.compare.setOriginalText(null);

        assertFalse(this.cdComparison.isEqual(this.base, this.compare));
    }
    
    @Test
    public void nullCodesDifferentOriginalTextAreNotEqual() {
        this.base.setOriginalText("new original text");

        assertFalse(this.cdComparison.isEqual(this.base, this.compare));
    }
    
    @Test
    public void sameCodesSystemsAndOriginalTextAreEqual() {
        assertTrue(this.cdComparison.isEqual(this.base, this.compare));
    }
    
    @Test
    public void differentDisplayNamesStillEqual() {
        this.base.setDisplayName("display name one");
        this.compare.setDisplayName("display name two");

        assertTrue(this.cdComparison.isEqual(this.base, this.compare));
    }
    
    @Test
    public void differentCodeSystemNamesStillEqual() {
        this.base.setCodeSystemName("code system name");
        this.compare.setCodeSystemName("new code system name");

        assertTrue(this.cdComparison.isEqual(this.base, this.compare));
    }
}