package org.cdsframework.messageconverter.fhir.convert.utils;

import org.opencds.vmr.v1_0.schema.CD;

/**
 * @author Brian Lamb
 */
public class CDComparison {
    /**
     * The standard CD class does not have a built in equals method so this method simulates the work
     * done in other methods. It compares two CD objects and returns whether or not the two objects are 
     * equal.
     * 
     * @param CD base : the first CD object to compare
     * @param CD compare : the CD object to compare it to
     * @return boolean
     */
    public boolean isEqual(CD base, CD compare) {
        if (base == null) {
            return false;
        }

        if (base.getClass() != compare.getClass()) {
            return false;
        }

        if (base.getCode() == null && compare.getCode() != null) {
            return false;
        }

        if (!base.getCode().equals(compare.getCode())) {
            return false;
        }

        if (base.getCodeSystem() == null && compare.getCodeSystem() != null) {
            return false;
        }

        if (base.getCodeSystem() != null || compare.getCodeSystem() != null) {
            if (!base.getCodeSystem().equals(compare.getCodeSystem())) {
                return false;
            }
        }

        if (base.getOriginalText() == null
            && base.getCode() != null
            && compare.getOriginalText() != null
            && compare.getCode() == null
        ) {
            return false;
        }

        if (base.getOriginalText() != null
            && base.getCode() == null
            && compare.getOriginalText() == null
            && compare.getCode() != null
        ) {
            return false;
        }

        if (!base.getOriginalText().equals(compare.getOriginalText())) {
            return false;
        }

        return true;
    }
}