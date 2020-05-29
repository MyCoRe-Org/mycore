package org.mycore.pi.condition;

/**
 * Just here for backward compatibility
 */
public class MCRPIPublishedPredicate extends MCRPIStatePredicate {
    public MCRPIPublishedPredicate(String propertyPrefix) {
        super(propertyPrefix);
    }

    @Override
    protected String getRequiredState() {
        return "published";
    }
}
