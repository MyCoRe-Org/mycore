package org.mycore.pi.condition;

import org.mycore.datamodel.metadata.MCRBase;

public class MCRTruePredicate extends MCRPIPredicateBase {

    public MCRTruePredicate(String propertyPrefix) {
        super(propertyPrefix);
    }

    @Override
    public boolean test(MCRBase mcrBase) {
        return true;
    }
}
