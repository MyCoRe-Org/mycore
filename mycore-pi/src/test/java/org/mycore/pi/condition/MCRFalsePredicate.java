package org.mycore.pi.condition;

import org.mycore.datamodel.metadata.MCRBase;

public class MCRFalsePredicate extends MCRPIPredicateBase {

    public MCRFalsePredicate(String propertyPrefix) {
        super(propertyPrefix);
    }

    @Override
    public boolean test(MCRBase mcrBase) {
        return false;
    }
}
