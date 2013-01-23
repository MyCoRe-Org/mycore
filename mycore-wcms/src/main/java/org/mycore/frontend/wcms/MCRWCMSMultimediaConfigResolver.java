package org.mycore.frontend.wcms;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

public class MCRWCMSMultimediaConfigResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        Element root = new Element("cms");
        MCRWCMSAdminServlet.getMultimediaConfig(root);
        return new JDOMSource(root);
    }

}