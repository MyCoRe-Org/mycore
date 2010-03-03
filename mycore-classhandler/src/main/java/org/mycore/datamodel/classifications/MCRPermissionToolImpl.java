package org.mycore.datamodel.classifications;

import org.mycore.access.MCRAccessManager;

public class MCRPermissionToolImpl implements MCRPermissionTool {

    @Override
    public boolean checkPermission(String permission) {
        return MCRAccessManager.checkPermission(permission);
    }

    @Override
    public boolean checkPermission(String id, String permission) {
        return MCRAccessManager.checkPermission(id, permission);
    }

}
