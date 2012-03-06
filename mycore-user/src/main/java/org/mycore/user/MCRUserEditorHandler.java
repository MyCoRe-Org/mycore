package org.mycore.user;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public abstract class MCRUserEditorHandler {

    public static Element getAssignableGroupsForUser() throws MCRAccessException {
        // Get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getUserInformation().getUserID();
        List<String> groupIDs = null;

        // The list of assignable groups depends on the privileges of the
        // current user.
        MCRUser currentUser = MCRUserMgr.instance().retrieveUser(currentUserID);

        try {
            if (MCRAccessManager.checkPermission("administrate-user")) {
                groupIDs = MCRUserMgr.instance().getAllGroupIDs();
            } else if (MCRAccessManager.checkPermission("create-user")) {
                groupIDs = currentUser.getGroupIDs();
            } else {
                throw new MCRAccessException("Not enough permissions! " + "Someone might have tried to call the new user form directly.");
            }
        } catch(MCRException exc) {
            throw new MCRAccessException("Not enough permissions", exc);
        }

        // Loop over all assignable group IDs
        org.jdom.Element root = new org.jdom.Element("items");

        for (int i = 0; i < groupIDs.size(); i++) {
            org.jdom.Element item = new org.jdom.Element("item")
                .setAttribute("value", (String) groupIDs.get(i))
                .setAttribute("label", (String) groupIDs.get(i));
            root.addContent(item);
        }
        return root;
    }

    public static Document retrieveGroupXml(String groupID) throws MCRAccessException {
        // We first check the privileges for this use case
        if (!MCRAccessManager.checkPermission("modify-user") && !MCRAccessManager.checkPermission("modify-contact")) {
            throw new MCRAccessException("No permissions");
        }
        MCRGroup group = MCRUserMgr.instance().retrieveGroup(groupID);
        return group.toJDOMDocument();
    }

    public static Document retrieveUserXml(String userID) throws MCRAccessException {
        if (!MCRAccessManager.checkPermission("modify-user") && !MCRAccessManager.checkPermission("modify-contact")) {
            throw new MCRAccessException("No permissions");
        }
        MCRUser user = MCRUserMgr.instance().retrieveUser(userID);
        return user.toJDOMDocument();
    }
    
    public static Document getAllUsers() throws MCRAccessException {
        if (!MCRAccessManager.checkPermission("modify-user") && !MCRAccessManager.checkPermission("modify-contact")) {
            throw new MCRAccessException("No permissions");
        }
        List<String> userIDs = MCRUserMgr.instance().getAllUserIDs();
        // Loop over all assignable group IDs
        Element root = new org.jdom.Element("items");
        for (int i = 0; i < userIDs.size(); i++) {
            Element item = new Element("item").setAttribute("value", (String) userIDs.get(i)).setAttribute("label", (String) userIDs.get(i));
            root.addContent(item);
        }
        return new Document(root);
    }
    
    public static Document getAllGroups() throws MCRAccessException {
        List<String> groupIDs;
        if (!MCRAccessManager.checkPermission("modify-user") && !MCRAccessManager.checkPermission("modify-contact")) {
            throw new MCRAccessException("No permissions");
        }
        groupIDs = MCRUserMgr.instance().getAllGroupIDs();
        // Loop over all assignable group IDs
        org.jdom.Element root = new org.jdom.Element("items");
        for (int i = 0; i < groupIDs.size(); i++) {
            org.jdom.Element item = new org.jdom.Element("item")
                .setAttribute("value", (String) groupIDs.get(i))
                .setAttribute("label", (String) groupIDs.get(i));
            root.addContent(item);
        }
        return new Document(root);
    }
}
