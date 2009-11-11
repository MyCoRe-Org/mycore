package org.mycore.datamodel.classifications;

/**
 * The purpose of this class is to make MCRClassificationBrowserData more testable.
 * This is necessary due to the static methods in MCRAccessManager.
 * 
 * @author "Huu Chi Vu"
 *
 */
public interface MCRPermissionTool {
    public boolean checkPermission(String permission);
    public boolean checkPermission(String id, String permission);
}
