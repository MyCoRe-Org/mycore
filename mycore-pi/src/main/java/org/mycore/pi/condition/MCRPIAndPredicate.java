package org.mycore.pi.condition;

import org.mycore.datamodel.metadata.MCRBase;

public class MCRPIAndPredicate extends MCRPICombinedPredicate {

    public MCRPIAndPredicate(String propertyPrefix) {
        super(propertyPrefix);
    }

    @Override
    public boolean test(MCRBase mcrBase) {
        return getCombinedPredicates()
            .allMatch(predicate -> predicate.test(mcrBase));
    }
}
