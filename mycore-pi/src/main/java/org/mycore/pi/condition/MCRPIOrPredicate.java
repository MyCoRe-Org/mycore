package org.mycore.pi.condition;

import org.mycore.datamodel.metadata.MCRBase;

public class MCRPIOrPredicate extends MCRPICombinedPredicate {
    public MCRPIOrPredicate(String propertyPrefix) {
        super(propertyPrefix);
    }

    @Override
    public boolean test(MCRBase mcrBase) {
        return getCombinedPredicates().anyMatch(predicate -> predicate.test(mcrBase));
    }
}
