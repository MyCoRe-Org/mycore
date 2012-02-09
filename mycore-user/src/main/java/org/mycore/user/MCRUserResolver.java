package org.mycore.user;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom.transform.JDOMSource;

public class MCRUserResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String target = href.substring(href.indexOf(":") + 1);
        String[] part = target.split(":");
        String method = part[0].toLowerCase();
        try {
            if("retrieveuserxml".equals(method)) {
                return new JDOMSource(MCRUserEditorHandler.retrieveUserXml(part[1]));
            } else if("getallusers".equals(method)) {
                return new JDOMSource(MCRUserEditorHandler.getAllUsers());
            }
        } catch(Exception exc) {
            throw new TransformerException(exc);
        }
        throw new TransformerException(new IllegalArgumentException("Unknown method " + method + " in uri " + href));
    }

}
