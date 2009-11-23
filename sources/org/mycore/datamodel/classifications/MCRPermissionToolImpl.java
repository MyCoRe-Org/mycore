package org.mycore.datamodel.classifications;

import org.mycore.access.MCRAccessManager;

public class MCRPermissionToolImpl implements MCRPermissionTool {

    public boolean checkPermission(String permission) {
        return MCRAccessManager.checkPermission(permission);
    }

    public boolean checkPermission(String id, String permission) {
        return MCRAccessManager.checkPermission(id, permission);
    }

}
