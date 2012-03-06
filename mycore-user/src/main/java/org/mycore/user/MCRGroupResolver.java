package org.mycore.user;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom.transform.JDOMSource;
import org.mycore.access.MCRAccessException;

public class MCRGroupResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String target = href.substring(href.indexOf(":") + 1);
        String[] part = target.split(":");
        String method = part[0].toLowerCase();
        try {
            if("getassignablegroupsforuser".equals(method)) {
                return new JDOMSource(MCRUserEditorHandler.getAssignableGroupsForUser());
            } else if("retrievegroupxml".equals(method)) {
                return new JDOMSource(MCRUserEditorHandler.retrieveGroupXml(part[1]));
            } else if("getallgroups".equals(method)) {
                return new JDOMSource(MCRUserEditorHandler.getAllGroups());
            }
        } catch(MCRAccessException exc) {
            throw new TransformerException(exc);
        }
        throw new TransformerException(new IllegalArgumentException("Unknown method " + method + " in uri " + href));
    }

}
