package org.mycore.access.strategies;

import org.mycore.access.MCRAccessManager;

public interface MCRAccessCheckStrategy {

    /**
     * determines whether the current user has the permission to perform a
     * certain action.
     * 
     * @param id
     *            the MCRObjectID of the object
     * @param permission
     *            the access permission for the rule
     * @return true if the access is allowed otherwise it return
     * @see MCRAccessManager#checkPermission(String, String)
     */
    public abstract boolean checkPermission(String id, String permission);

}