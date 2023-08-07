package org.mycore.iview2.services;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

public class MCRIview2URIResolver implements URIResolver {
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] params = href.split(":");

        if (params.length != 3) {
            throw new TransformerException("Invalid href: " + href);
        }

        switch (params[1]) {
            case "isCompletelyTiled" -> {
                boolean completelyTiled = MCRIView2Tools.isCompletelyTiled(params[2]);
                return new JDOMSource(new Element(String.valueOf(completelyTiled)));
            }
            default -> throw new TransformerException("Invalid href: " + href);
        }
    }
}
