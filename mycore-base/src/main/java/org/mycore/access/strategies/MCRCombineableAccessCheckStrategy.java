/**
 * 
 */
package org.mycore.access.strategies;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public interface MCRCombineableAccessCheckStrategy extends MCRAccessCheckStrategy {

    /**
     * Checks if this strategy has a rule mapping defined.
     * Can be used by other more complex strategies that require this information to decide if this strategy should be used.
     * @param id 
     *              a possible MCRObjectID of the object or any other "id"
     * @param permission
     *              the access permission for the rule
     * @return true if there is a mapping to a rule defined
     */
    public abstract boolean hasRuleMapping(String id, String permission);

}
