/**
 * 
 */
package org.mycore.access.strategies;

import java.util.Collection;

import org.mycore.datamodel.common.MCRLinkTableManager;

/**
 * @author Silvio Hermann
 */
public class MCRDerivateIDStrategy implements MCRAccessCheckStrategy {

    @Override
    public boolean checkPermission(String id, String permission) {
        if (!id.contains("_derivate_")) {
            return new MCRObjectIDStrategy().checkPermission(id, permission);
        }
        final Collection<String> l = MCRLinkTableManager.instance().getSourceOf(id, "derivate");
        if (l != null && !l.isEmpty()) {
            return checkPermission(l.iterator().next(), permission);
        }
        return false;
    }

}
